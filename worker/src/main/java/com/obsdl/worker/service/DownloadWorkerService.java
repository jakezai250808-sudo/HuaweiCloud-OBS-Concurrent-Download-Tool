package com.obsdl.worker.service;

import com.obsdl.common.dto.CreateTaskRequest;
import com.obsdl.common.dto.TaskResponse;
import com.obsdl.common.enums.TaskStatus;
import com.obsdl.common.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class DownloadWorkerService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public TaskResponse accept(CreateTaskRequest request) {
        long taskId = IdGenerator.nextId();
        CompletableFuture.runAsync(() -> {
            log.info("Start processing task {} for {}/{}", taskId, request.getBucket(), request.getObjectKey());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Task {} interrupted", taskId);
            }
        }, executorService);

        return TaskResponse.builder()
                .taskId(taskId)
                .bucket(request.getBucket())
                .objectKey(request.getObjectKey())
                .concurrency(request.getConcurrency())
                .status(TaskStatus.RUNNING)
                .build();
    }
}
