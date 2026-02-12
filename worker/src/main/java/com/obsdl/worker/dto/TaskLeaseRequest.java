package com.obsdl.worker.dto;

public record TaskLeaseRequest(
        String workerId,
        Integer count
) {
}
