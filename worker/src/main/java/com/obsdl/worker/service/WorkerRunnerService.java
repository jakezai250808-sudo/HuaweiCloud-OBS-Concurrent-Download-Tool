package com.obsdl.worker.service;

import com.obsdl.worker.client.MasterClient;
import com.obsdl.worker.config.WorkerRuntimeProperties;
import com.obsdl.worker.dto.MasterTaskObjectResponse;
import com.obsdl.worker.dto.MasterTaskResponse;
import com.obsdl.worker.dto.TaskLeaseObjectResponse;
import com.obsdl.worker.obs.ObsClientFacade;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class WorkerRunnerService {

    private final MasterClient masterClient;
    private final ObsClientFacade obsClientFacade;
    private final RsyncService rsyncService;
    private final WorkerRuntimeProperties props;
    private final int serverPort;

    private final ExecutorService executor;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger queuedTasks = new AtomicInteger(0);
    private final AtomicLong currentTaskId;

    public WorkerRunnerService(MasterClient masterClient,
                               ObsClientFacade obsClientFacade,
                               RsyncService rsyncService,
                               WorkerRuntimeProperties props,
                               @Value("${server.port:8081}") int serverPort) {
        this.masterClient = masterClient;
        this.obsClientFacade = obsClientFacade;
        this.rsyncService = rsyncService;
        this.props = props;
        this.serverPort = serverPort;
        this.executor = new ThreadPoolExecutor(
                props.maxDlConcurrency(),
                props.maxDlConcurrency(),
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.currentTaskId = new AtomicLong(props.leaseTaskId());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws Exception {
        String host = InetAddress.getLocalHost().getHostAddress();
        masterClient.register(host, serverPort);
        log.info("Worker registered, workerId={}, host={}, port={}, initialTaskId={}",
                props.workerId(), host, serverPort, currentTaskId.get());

        Thread leaseThread = new Thread(this::leaseLoop, "worker-lease-loop");
        leaseThread.setDaemon(true);
        leaseThread.start();
    }

    @Scheduled(fixedDelayString = "#{${worker.runtime.heartbeat-seconds:10} * 1000}")
    public void heartbeat() {
        try {
            masterClient.heartbeat(activeTasks.get(), queuedTasks.get());
        } catch (Exception ex) {
            log.warn("heartbeat failed: {}", ex.getMessage(), ex);
        }
    }

    private void leaseLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long taskId = currentTaskId.get();
                List<TaskLeaseObjectResponse> objects = masterClient.lease(taskId);
                if (objects.isEmpty()) {
                    switchToNextTaskIfPossible(taskId);
                    Thread.sleep(props.leaseIdleSleepMs());
                    continue;
                }
                log.info("leased {} objects for taskId={}", objects.size(), taskId);
                for (TaskLeaseObjectResponse object : objects) {
                    queuedTasks.incrementAndGet();
                    executor.submit(() -> processObject(taskId, object));
                }
            } catch (Exception ex) {
                log.error("lease loop error: {}", ex.getMessage(), ex);
                try {
                    Thread.sleep(props.leaseIdleSleepMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processObject(long taskId, TaskLeaseObjectResponse object) {
        queuedTasks.decrementAndGet();
        activeTasks.incrementAndGet();

        String objectKey = object.objectKey();
        Path partPath = buildCachePath(taskId, objectKey, ".part");
        Path finalPath = buildCachePath(taskId, objectKey, "");

        try {
            Files.createDirectories(finalPath.getParent());
            obsClientFacade.downloadToFile(object, partPath);
            Files.move(partPath, finalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String remotePath = "%s:%s/%d/%s".formatted(
                    props.masterSsh(),
                    normalizeRemotePath(props.targetPath(), props.targetTmpSubdir()),
                    taskId,
                    objectKey
            );
            syncToRemoteIfNeeded(finalPath, remotePath, taskId, objectKey);
            masterClient.reportDone(taskId, objectKey);
            log.info("object processed successfully, taskId={}, objectKey={}", taskId, objectKey);
        } catch (Exception ex) {
            log.error("object process failed, taskId={}, objectKey={}, err={}", taskId, objectKey, ex.getMessage(), ex);
            try {
                masterClient.reportFailed(taskId, objectKey, abbreviate(ex.getMessage(), 500));
            } catch (Exception reportEx) {
                log.error("report failed status error, taskId={}, objectKey={}, err={}",
                        taskId, objectKey, reportEx.getMessage(), reportEx);
            }
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    private void syncToRemoteIfNeeded(Path finalPath, String remotePath, long taskId, String objectKey) throws Exception {
        if (props.masterSsh() == null || props.masterSsh().isBlank()) {
            log.warn("skip rsync because MASTER_SSH is blank, taskId={}, objectKey={}", taskId, objectKey);
            return;
        }
        try {
            rsyncService.sync(finalPath, remotePath);
        } catch (Exception ex) {
            if (Boolean.TRUE.equals(props.rsyncRequired())) {
                throw ex;
            }
            log.warn("rsync failed but continue as success because rsyncRequired=false, taskId={}, objectKey={}, err={}",
                    taskId, objectKey, ex.getMessage());
        }
    }

    private void switchToNextTaskIfPossible(long currentTaskId) {
        List<MasterTaskResponse> tasks = masterClient.listTasks();
        Long nextTaskId = tasks.stream()
                .filter(task -> task.id() != null)
                .filter(this::hasPendingObjects)
                .map(MasterTaskResponse::id)
                .sorted()
                .findFirst()
                .orElse(null);
        if (nextTaskId == null || nextTaskId.equals(currentTaskId)) {
            return;
        }
        this.currentTaskId.set(nextTaskId);
        log.info("switch task lease id from {} to {}", currentTaskId, nextTaskId);
    }

    private boolean hasPendingObjects(MasterTaskResponse task) {
        if (task.objects() == null || task.objects().isEmpty()) {
            return false;
        }
        return task.objects().stream()
                .map(MasterTaskObjectResponse::status)
                .filter(Objects::nonNull)
                .anyMatch(status -> "PENDING".equalsIgnoreCase(status));
    }

    private Path buildCachePath(long taskId, String objectKey, String suffix) {
        Path taskDir = Paths.get(props.cacheDir(), String.valueOf(taskId));
        return taskDir.resolve(objectKey + suffix);
    }

    private String normalizeRemotePath(String targetPath, String tmpSubdir) {
        String base = targetPath.endsWith("/") ? targetPath.substring(0, targetPath.length() - 1) : targetPath;
        String sub = tmpSubdir.startsWith("/") ? tmpSubdir.substring(1) : tmpSubdir;
        if (!base.startsWith("/")) {
            base = "/" + base;
        }
        return base + "/" + sub;
    }

    private String abbreviate(String message, int maxLen) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() <= maxLen ? message : message.substring(0, maxLen);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
