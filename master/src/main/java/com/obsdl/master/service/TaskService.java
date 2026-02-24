package com.obsdl.master.service;

import com.obsdl.master.dto.task.*;
import com.obsdl.master.entity.DownloadTaskEntity;
import com.obsdl.master.entity.ObsAccountEntity;
import com.obsdl.master.entity.TaskObjectEntity;
import com.obsdl.master.exception.BizException;
import com.obsdl.master.mapper.DownloadTaskMapper;
import com.obsdl.master.mapper.TaskObjectMapper;
import com.obsdl.master.service.crud.DownloadTaskCrudService;
import com.obsdl.master.service.crud.ObsAccountCrudService;
import com.obsdl.master.service.crud.TaskObjectCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TaskService {

    private static final int DEFAULT_CONCURRENCY = 4;
    private final DownloadTaskCrudService downloadTaskCrudService;
    private final TaskObjectCrudService taskObjectCrudService;
    private final ObsAccountCrudService obsAccountCrudService;
    private final TaskObjectMapper taskObjectMapper;
    private final DownloadTaskMapper downloadTaskMapper;

    public TaskService(DownloadTaskCrudService downloadTaskCrudService,
                       TaskObjectCrudService taskObjectCrudService,
                       ObsAccountCrudService obsAccountCrudService,
                       TaskObjectMapper taskObjectMapper,
                       DownloadTaskMapper downloadTaskMapper) {
        this.downloadTaskCrudService = downloadTaskCrudService;
        this.taskObjectCrudService = taskObjectCrudService;
        this.obsAccountCrudService = obsAccountCrudService;
        this.taskObjectMapper = taskObjectMapper;
        this.downloadTaskMapper = downloadTaskMapper;
    }

    @Transactional
    public TaskCreateResponse create(TaskCreateRequest request) {
        if (request.selection().prefix() != null && !request.selection().prefix().isBlank()) {
            // TODO: 支持按 prefix 递归展开对象列表
            throw new BizException(40001, "selection.prefix 暂不支持，请改用 selection.objects");
        }
        if (obsAccountCrudService.getById(request.accountId()) == null) {
            throw new BizException(40403, "账号不存在");
        }

        List<String> objectKeys = request.selection().objects().stream()
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .distinct()
                .toList();
        if (objectKeys.isEmpty()) {
            throw new BizException(40001, "selection.objects 不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        DownloadTaskEntity task = new DownloadTaskEntity();
        task.setAccountId(request.accountId());
        task.setBucket(request.bucket());
        task.setConcurrency(DEFAULT_CONCURRENCY);
        task.setTotalObjects(objectKeys.size());
        task.setDoneObjects(0);
        task.setStatus("CREATED");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        downloadTaskCrudService.save(task);

        List<TaskObjectEntity> taskObjects = objectKeys.stream().map(objectKey -> {
            TaskObjectEntity entity = new TaskObjectEntity();
            entity.setTaskId(task.getId());
            entity.setObjectKey(objectKey);
            entity.setSize(0L);
            entity.setStatus("PENDING");
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            return entity;
        }).toList();
        taskObjectCrudService.saveBatch(taskObjects);
        return new TaskCreateResponse(task.getId());
    }

    @Transactional
    public TaskCreateResponse retryFromTask(Long sourceTaskId) {
        DownloadTaskEntity sourceTask = mustGetTask(sourceTaskId);

        List<TaskObjectEntity> failedObjects = taskObjectCrudService.lambdaQuery()
                .eq(TaskObjectEntity::getTaskId, sourceTaskId)
                .eq(TaskObjectEntity::getStatus, "FAILED")
                .list();
        if (failedObjects.isEmpty()) {
            throw new BizException(40001, "源任务无失败对象可重试");
        }

        LocalDateTime now = LocalDateTime.now();
        DownloadTaskEntity newTask = new DownloadTaskEntity();
        newTask.setAccountId(sourceTask.getAccountId());
        newTask.setBucket(sourceTask.getBucket());
        newTask.setConcurrency(sourceTask.getConcurrency() == null ? DEFAULT_CONCURRENCY : sourceTask.getConcurrency());
        newTask.setTotalObjects(failedObjects.size());
        newTask.setDoneObjects(0);
        newTask.setStatus("CREATED");
        newTask.setCreatedAt(now);
        newTask.setUpdatedAt(now);
        downloadTaskCrudService.save(newTask);

        List<TaskObjectEntity> retryObjects = failedObjects.stream()
                .map(sourceObject -> {
                    TaskObjectEntity retryObject = new TaskObjectEntity();
                    retryObject.setTaskId(newTask.getId());
                    retryObject.setObjectKey(sourceObject.getObjectKey());
                    retryObject.setSize(sourceObject.getSize() == null ? 0L : sourceObject.getSize());
                    retryObject.setEtag(sourceObject.getEtag());
                    retryObject.setStatus("PENDING");
                    retryObject.setCreatedAt(now);
                    retryObject.setUpdatedAt(now);
                    return retryObject;
                })
                .toList();
        taskObjectCrudService.saveBatch(retryObjects);
        return new TaskCreateResponse(newTask.getId());
    }

    @Transactional
    public List<TaskLeaseObjectResponse> lease(Long taskId, TaskLeaseRequest request) {
        DownloadTaskEntity task = mustGetTask(taskId);
        ObsAccountEntity account = obsAccountCrudService.getById(task.getAccountId());
        if (account == null) {
            throw new BizException(40403, "任务关联账号不存在");
        }

        // 并发安全最小保证：在同一事务中对 PENDING 对象执行 FOR UPDATE 锁定，再批量更新为 LEASED，避免多个 worker 重复领取同一对象。
        List<TaskObjectEntity> pendingObjects = taskObjectMapper.selectPendingForUpdate(taskId, request.count());
        if (pendingObjects.isEmpty()) {
            return List.of();
        }

        List<Long> ids = pendingObjects.stream().map(TaskObjectEntity::getId).toList();
        taskObjectMapper.leaseObjects(taskId, ids, request.workerId());

        return pendingObjects.stream().map(entity -> new TaskLeaseObjectResponse(
                entity.getObjectKey(),
                entity.getSize(),
                entity.getEtag(),
                account.getEndpoint(),
                account.getAccessKey(),
                account.getSecretKey(),
                task.getBucket()
        )).toList();
    }

    @Transactional
    public void report(Long taskId, TaskReportRequest request) {
        mustGetTask(taskId);

        // 并发安全最小保证：对目标对象 FOR UPDATE，确保状态迁移和 done_objects 计数在事务内串行化，防止重复累计。
        TaskObjectEntity object = taskObjectMapper.selectByTaskIdAndObjectKeyForUpdate(taskId, request.objectKey());
        if (object == null) {
            throw new BizException(40403, "任务对象不存在");
        }

        if (object.getLeasedBy() != null && !object.getLeasedBy().equals(request.workerId())) {
            throw new BizException(40001, "对象由其他 worker 租约持有");
        }

        String status = request.status().toUpperCase();
        if ("DONE".equals(status)) {
            if (!"DONE".equals(object.getStatus())) {
                object.setStatus("DONE");
                object.setLeasedBy(request.workerId());
                taskObjectMapper.updateById(object);
                downloadTaskMapper.increaseDoneObjects(taskId);
                downloadTaskMapper.markTaskDoneIfFinished(taskId);
            }
            return;
        }

        object.setStatus("FAILED");
        object.setLeasedBy(request.workerId());
        taskObjectMapper.updateById(object);
        log.warn("object reported failed, taskId={}, objectKey={}, workerId={}, error={} ",
                taskId, request.objectKey(), request.workerId(), request.errorMessage());
    }

    public TaskResponse getById(Long id) {
        DownloadTaskEntity task = downloadTaskCrudService.getById(id);
        if (task == null) {
            throw new BizException(40403, "任务不存在");
        }

        List<TaskObjectResponse> objects = listObjects(id);
        return new TaskResponse(
                task.getId(),
                task.getAccountId(),
                task.getBucket(),
                task.getStatus(),
                Objects.nonNull(task.getCreatedAt()) ? task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : Instant.now(),
                objects
        );
    }

    public List<TaskResponse> list() {
        return downloadTaskCrudService.lambdaQuery()
                .orderByDesc(DownloadTaskEntity::getId)
                .list()
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getAccountId(),
                        task.getBucket(),
                        task.getStatus(),
                        Objects.nonNull(task.getCreatedAt()) ? task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : Instant.now(),
                        listObjects(task.getId())
                ))
                .toList();
    }

    public List<TaskObjectResponse> listObjects(Long id) {
        if (downloadTaskCrudService.getById(id) == null) {
            throw new BizException(40403, "任务不存在");
        }
        return taskObjectCrudService.lambdaQuery()
                .eq(TaskObjectEntity::getTaskId, id)
                .list()
                .stream()
                .map(entity -> new TaskObjectResponse(entity.getObjectKey(), entity.getStatus(), entity.getSize() == null ? 0L : entity.getSize()))
                .toList();
    }

    private DownloadTaskEntity mustGetTask(Long taskId) {
        DownloadTaskEntity task = downloadTaskCrudService.getById(taskId);
        if (task == null) {
            throw new BizException(40403, "任务不存在");
        }
        return task;
    }
}
