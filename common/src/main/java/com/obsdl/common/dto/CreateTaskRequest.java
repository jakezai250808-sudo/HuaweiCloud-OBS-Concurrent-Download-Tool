package com.obsdl.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "bucket is required")
    private String bucket;

    @NotBlank(message = "objectKey is required")
    private String objectKey;

    @Min(value = 1, message = "concurrency must be >= 1")
    private int concurrency = 4;
}
