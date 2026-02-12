package com.obsdl.master.service;

import com.obsdl.master.dto.task.TaskCreateRequest;
import com.obsdl.master.dto.task.TaskObjectResponse;
import com.obsdl.master.dto.task.TaskResponse;
import com.obsdl.master.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TaskService {

    private final AtomicLong taskIdGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, TaskResponse> tasks = new ConcurrentHashMap<>();

    public TaskResponse create(TaskCreateRequest request) {
        long id = taskIdGenerator.getAndIncrement();
        List<TaskObjectResponse> objects = request.objectKeys().stream()
                .map(key -> new TaskObjectResponse(key, "PENDING", 0L))
                .toList();
        TaskResponse response = new TaskResponse(
                id,
                request.accountName(),
                request.bucket(),
                "CREATED",
                Instant.now(),
                objects
        );
        tasks.put(id, response);
        return response;
    }

    public TaskResponse getById(Long id) {
        TaskResponse task = tasks.get(id);
        if (task == null) {
            throw new BizException(40403, "任务不存在");
        }
        return task;
    }

    public List<TaskObjectResponse> listObjects(Long id) {
        return getById(id).objects();
    }
}
