package com.obsdl.master.service;

import com.obsdl.common.dto.CreateTaskRequest;
import com.obsdl.common.dto.TaskResponse;
import com.obsdl.common.enums.TaskStatus;
import com.obsdl.common.util.IdGenerator;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    public TaskResponse createTask(CreateTaskRequest request) {
        return TaskResponse.builder()
                .taskId(IdGenerator.nextId())
                .bucket(request.getBucket())
                .objectKey(request.getObjectKey())
                .concurrency(request.getConcurrency())
                .status(TaskStatus.CREATED)
                .build();
    }
}
