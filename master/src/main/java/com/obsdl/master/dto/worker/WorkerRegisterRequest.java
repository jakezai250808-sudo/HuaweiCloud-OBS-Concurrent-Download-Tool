package com.obsdl.master.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkerRegisterRequest(
        @NotBlank @Size(max = 64) String workerId,
        @NotBlank @Size(max = 64) String host,
        @NotNull Integer port,
        @NotNull Integer maxConcurrency
) {
}
