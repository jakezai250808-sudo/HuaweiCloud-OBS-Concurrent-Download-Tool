package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.task.TaskCreateRequest;
import com.obsdl.master.dto.task.TaskObjectResponse;
import com.obsdl.master.dto.task.TaskResponse;
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
    public ApiResponse<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ApiResponse<TaskResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(taskService.getById(id));
    }

    @GetMapping("/{id}/objects")
    @Operation(summary = "获取任务对象列表（占位）", description = "TODO: 接入 OBS Java SDK，返回真实对象列表与元数据")
    public ApiResponse<List<TaskObjectResponse>> listObjects(@PathVariable Long id) {
        return ApiResponse.success(taskService.listObjects(id));
    }
}
