package com.obsdl.master.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 128) String accessKey,
        @NotBlank @Size(max = 128) String secretKey,
        @NotBlank @Size(max = 255) String endpoint,
        @NotBlank @Size(max = 64) String bucket
) {
}
