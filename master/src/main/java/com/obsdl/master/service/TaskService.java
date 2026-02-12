package com.obsdl.master.service;

import com.obsdl.master.dto.task.TaskCreateRequest;
import com.obsdl.master.dto.task.TaskCreateResponse;
import com.obsdl.master.dto.task.TaskObjectResponse;
import com.obsdl.master.dto.task.TaskResponse;
import com.obsdl.master.entity.DownloadTaskEntity;
import com.obsdl.master.entity.TaskObjectEntity;
import com.obsdl.master.exception.BizException;
import com.obsdl.master.service.crud.DownloadTaskCrudService;
import com.obsdl.master.service.crud.TaskObjectCrudService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class TaskService {

    private static final int DEFAULT_CONCURRENCY = 4;
    private final DownloadTaskCrudService downloadTaskCrudService;
    private final TaskObjectCrudService taskObjectCrudService;

    public TaskService(DownloadTaskCrudService downloadTaskCrudService,
                       TaskObjectCrudService taskObjectCrudService) {
        this.downloadTaskCrudService = downloadTaskCrudService;
        this.taskObjectCrudService = taskObjectCrudService;
    }

    @Transactional
    public TaskCreateResponse create(TaskCreateRequest request) {
        if (request.selection().prefix() != null && !request.selection().prefix().isBlank()) {
            // TODO: 支持按 prefix 递归展开对象列表
            throw new BizException(40001, "selection.prefix 暂不支持，请改用 selection.objects");
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
}
