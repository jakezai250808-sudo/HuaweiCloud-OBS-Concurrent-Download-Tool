package com.obsdl.master.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank(message = "name 不能为空") @Size(max = 64) String name,
        @NotBlank(message = "accessKey 不能为空") @Size(max = 128) String accessKey,
        @NotBlank(message = "secretKey 不能为空") @Size(max = 128) String secretKey,
        @NotBlank(message = "endpoint 不能为空") @Size(max = 255) String endpoint,
        @NotBlank(message = "bucket 不能为空") @Size(max = 64) String bucket
) {
}
