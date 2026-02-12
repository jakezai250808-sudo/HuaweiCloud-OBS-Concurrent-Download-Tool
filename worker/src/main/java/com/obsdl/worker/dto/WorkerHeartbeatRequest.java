package com.obsdl.worker.dto;

public record WorkerHeartbeatRequest(
        String workerId,
        Integer activeTasks,
        Integer queuedTasks
) {
}
