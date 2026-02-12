package com.obsdl.master.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkerHeartbeatRequest(
        @NotBlank String workerId,
        @NotNull Integer activeTasks,
        @NotNull Integer queuedTasks
) {
}
