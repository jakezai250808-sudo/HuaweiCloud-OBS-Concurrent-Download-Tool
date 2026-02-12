package com.obsdl.master.service;

import com.obsdl.common.dto.CreateTaskRequest;
import com.obsdl.common.dto.TaskResponse;
import com.obsdl.common.enums.TaskStatus;
import com.obsdl.master.entity.DownloadTaskEntity;
import com.obsdl.master.entity.TaskObjectEntity;
import com.obsdl.master.service.crud.DownloadTaskCrudService;
import com.obsdl.master.service.crud.TaskObjectCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final DownloadTaskCrudService downloadTaskCrudService;
    private final TaskObjectCrudService taskObjectCrudService;

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse createTask(CreateTaskRequest request) {
        DownloadTaskEntity taskEntity = new DownloadTaskEntity();
        taskEntity.setBucket(request.getBucket());
        taskEntity.setConcurrency(request.getConcurrency());
        taskEntity.setStatus(TaskStatus.CREATED.name());
        downloadTaskCrudService.save(taskEntity);

        TaskObjectEntity objectEntity = new TaskObjectEntity();
        objectEntity.setTaskId(taskEntity.getId());
        objectEntity.setObjectKey(request.getObjectKey());
        objectEntity.setStatus(TaskStatus.CREATED.name());
        taskObjectCrudService.save(objectEntity);

        return TaskResponse.builder()
                .taskId(taskEntity.getId())
                .bucket(taskEntity.getBucket())
                .objectKey(objectEntity.getObjectKey())
                .concurrency(taskEntity.getConcurrency())
                .status(TaskStatus.CREATED)
                .build();
    }
}
