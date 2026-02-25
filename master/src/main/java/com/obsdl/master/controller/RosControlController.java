package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.ros.RosStartRequest;
import com.obsdl.master.dto.ros.RosStatusResponse;
import com.obsdl.master.service.ApiTokenService;
import com.obsdl.master.service.RosControlService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ros")
public class RosControlController {

    private final RosControlService rosControlService;
    private final ApiTokenService apiTokenService;

    public RosControlController(RosControlService rosControlService, ApiTokenService apiTokenService) {
        this.rosControlService = rosControlService;
        this.apiTokenService = apiTokenService;
    }

    @PostMapping("/start")
    @Operation(summary = "启动 ROS 播放与 rosbridge")
    public ApiResponse<RosStatusResponse> start(@Valid @RequestBody RosStartRequest request) {
        apiTokenService.reloadTokensIfNeeded();
        return ApiResponse.success(rosControlService.start(request));
    }

    @PostMapping("/stop")
    @Operation(summary = "停止 ROS 播放与 rosbridge")
    public ApiResponse<RosStatusResponse> stop() {
        apiTokenService.reloadTokensIfNeeded();
        return ApiResponse.success(rosControlService.stop());
    }

    @GetMapping("/status")
    @Operation(summary = "查询 ROS 控制状态")
    public ApiResponse<RosStatusResponse> status() {
        apiTokenService.reloadTokensIfNeeded();
        return ApiResponse.success(rosControlService.status());
    }
}
