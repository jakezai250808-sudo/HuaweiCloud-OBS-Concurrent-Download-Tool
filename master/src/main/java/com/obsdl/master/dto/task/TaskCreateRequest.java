package com.obsdl.master.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TaskCreateRequest(
        @NotBlank @Size(max = 64) String accountName,
        @NotBlank @Size(max = 64) String bucket,
        @NotEmpty List<@NotBlank String> objectKeys
) {
}
