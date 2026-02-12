package com.obsdl.worker.service;

import com.obsdl.worker.client.MasterClient;
import com.obsdl.worker.config.WorkerRuntimeProperties;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws Exception {
        String host = InetAddress.getLocalHost().getHostAddress();
        masterClient.register(host, serverPort);
        log.info("Worker registered, workerId={}, host={}, port={}", props.workerId(), host, serverPort);

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
                List<TaskLeaseObjectResponse> objects = masterClient.lease();
                if (objects.isEmpty()) {
                    Thread.sleep(props.leaseIdleSleepMs());
                    continue;
                }
                log.info("leased {} objects", objects.size());
                for (TaskLeaseObjectResponse object : objects) {
                    queuedTasks.incrementAndGet();
                    executor.submit(() -> processObject(object));
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

    private void processObject(TaskLeaseObjectResponse object) {
        queuedTasks.decrementAndGet();
        activeTasks.incrementAndGet();

        String objectKey = object.objectKey();
        Path partPath = buildCachePath(objectKey, ".part");
        Path finalPath = buildCachePath(objectKey, "");

        try {
            Files.createDirectories(finalPath.getParent());
            obsClientFacade.downloadToFile(object, partPath);
            Files.move(partPath, finalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String remotePath = "%s:%s/%d/%s".formatted(
                    props.masterSsh(),
                    normalizeRemotePath(props.targetPath(), props.targetTmpSubdir()),
                    props.leaseTaskId(),
                    objectKey
            );
            rsyncService.sync(finalPath, remotePath);
            masterClient.reportDone(objectKey);
            log.info("object processed successfully: {}", objectKey);
        } catch (Exception ex) {
            log.error("object process failed, objectKey={}, err={}", objectKey, ex.getMessage(), ex);
            try {
                masterClient.reportFailed(objectKey, abbreviate(ex.getMessage(), 500));
            } catch (Exception reportEx) {
                log.error("report failed status error, objectKey={}, err={}", objectKey, reportEx.getMessage(), reportEx);
            }
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    private Path buildCachePath(String objectKey, String suffix) {
        Path taskDir = Paths.get(props.cacheDir(), String.valueOf(props.leaseTaskId()));
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
