package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;
import com.obsdl.master.service.ObsBrowserService;
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

    private final ObsBrowserService obsBrowserService;

    public ObsMockController(ObsBrowserService obsBrowserService) {
        this.obsBrowserService = obsBrowserService;
    }

    @GetMapping("/buckets")
    @Operation(summary = "按账号列举 Buckets", description = "从数据库按账号查询 bucket 列表")
    public ApiResponse<BucketListResponse> listBuckets(@RequestParam("accountId") Long accountId) {
        return ApiResponse.success(obsBrowserService.listBuckets(accountId));
    }

    @GetMapping("/objects")
    @Operation(summary = "列举 Objects（mock）", description = "基于 mock 数据按 prefix + delimiter 返回目录与对象")
    public ApiResponse<ObsObjectListingResponse> listObjects(
            @RequestParam("bucket") @NotBlank String bucket,
            @RequestParam(value = "prefix", defaultValue = "") String prefix,
            @RequestParam(value = "delimiter", defaultValue = "/") String delimiter,
            @RequestParam(value = "marker", required = false) String marker,
            @RequestParam(value = "continuationToken", required = false) String continuationToken
    ) {
        return ApiResponse.success(obsBrowserService.listObjects(bucket, prefix, delimiter, marker, continuationToken));
    }
}
