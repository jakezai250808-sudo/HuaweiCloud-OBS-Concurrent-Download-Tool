package com.obsdl.worker.controller;

import com.obsdl.common.dto.CreateTaskRequest;
import com.obsdl.common.dto.TaskResponse;
import com.obsdl.worker.service.DownloadWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker/tasks")
@RequiredArgsConstructor
public class WorkerTaskController {

    private final DownloadWorkerService downloadWorkerService;

    @PostMapping
    public TaskResponse consume(@RequestBody CreateTaskRequest request) {
        return downloadWorkerService.accept(request);
    }
}
