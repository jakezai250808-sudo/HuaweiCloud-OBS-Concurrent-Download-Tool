package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.worker.WorkerHeartbeatRequest;
import com.obsdl.master.dto.worker.WorkerRegisterRequest;
import com.obsdl.master.dto.worker.WorkerResponse;
import com.obsdl.master.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping("/register")
    @Operation(summary = "Worker 注册")
    public ApiResponse<WorkerResponse> register(@Valid @RequestBody WorkerRegisterRequest request) {
        return ApiResponse.success(workerService.register(request));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Worker 心跳")
    public ApiResponse<WorkerResponse> heartbeat(@Valid @RequestBody WorkerHeartbeatRequest request) {
        return ApiResponse.success(workerService.heartbeat(request));
    }
}
