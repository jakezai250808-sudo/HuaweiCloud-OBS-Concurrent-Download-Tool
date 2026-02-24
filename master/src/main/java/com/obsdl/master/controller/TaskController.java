package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.task.*;
import com.obsdl.master.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "创建下载任务")
    public ApiResponse<TaskCreateResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "基于已有任务重建重试任务（仅失败对象）")
    public ApiResponse<TaskCreateResponse> retry(@PathVariable("id") Long id) {
        return ApiResponse.success(taskService.retryFromTask(id));
    }

    @GetMapping
    @Operation(summary = "查询任务列表")
    public ApiResponse<List<TaskResponse>> list() {
        return ApiResponse.success(taskService.list());
    }


    @PostMapping("/{id}/lease")
    @Operation(summary = "Worker 租约领取任务对象")
    public ApiResponse<List<TaskLeaseObjectResponse>> lease(@PathVariable("id") Long id,
                                                            @Valid @RequestBody TaskLeaseRequest request) {
        return ApiResponse.success(taskService.lease(id, request));
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Worker 上报对象下载结果")
    public ApiResponse<Void> report(@PathVariable("id") Long id, @Valid @RequestBody TaskReportRequest request) {
        taskService.report(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ApiResponse<TaskResponse> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(taskService.getById(id));
    }

    @GetMapping("/{id}/objects")
    @Operation(summary = "获取任务对象列表（占位）", description = "TODO: 接入 OBS Java SDK，返回真实对象列表与元数据")
    public ApiResponse<List<TaskObjectResponse>> listObjects(@PathVariable("id") Long id) {
        return ApiResponse.success(taskService.listObjects(id));
    }
}
