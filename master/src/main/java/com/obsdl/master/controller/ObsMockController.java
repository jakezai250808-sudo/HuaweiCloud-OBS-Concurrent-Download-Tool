package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObjectListResponse;
import com.obsdl.master.service.ObsMockService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/obs")
@Validated
public class ObsMockController {

    private final ObsMockService obsMockService;

    public ObsMockController(ObsMockService obsMockService) {
        this.obsMockService = obsMockService;
    }

    @GetMapping("/buckets")
    @Operation(summary = "列举 Buckets（占位）", description = "TODO: 接入 OBS Java SDK，返回账号下真实 buckets")
    public ApiResponse<BucketListResponse> listBuckets() {
        return ApiResponse.success(obsMockService.listBuckets());
    }

    @GetMapping("/objects")
    @Operation(summary = "列举 Objects（占位）", description = "TODO: 接入 OBS Java SDK，返回 bucket 下真实对象列表")
    public ApiResponse<ObjectListResponse> listObjects(@RequestParam("bucket") @NotBlank String bucket) {
        return ApiResponse.success(obsMockService.listObjects(bucket));
    }
}
