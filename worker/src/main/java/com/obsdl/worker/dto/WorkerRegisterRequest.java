package com.obsdl.worker.dto;

public record WorkerRegisterRequest(
        String workerId,
        String host,
        Integer port,
        Integer maxConcurrency
) {
}
