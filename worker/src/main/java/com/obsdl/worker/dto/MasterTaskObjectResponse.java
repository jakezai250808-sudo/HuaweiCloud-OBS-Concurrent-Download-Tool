package com.obsdl.worker.dto;

public record MasterTaskObjectResponse(
        String objectKey,
        String status,
        long size
) {
}
