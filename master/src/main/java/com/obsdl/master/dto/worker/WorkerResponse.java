package com.obsdl.master.dto.worker;

import java.time.Instant;

public record WorkerResponse(
        String workerId,
        String host,
        Integer port,
        Integer maxConcurrency,
        Integer activeTasks,
        Integer queuedTasks,
        Instant lastHeartbeat
) {
}
