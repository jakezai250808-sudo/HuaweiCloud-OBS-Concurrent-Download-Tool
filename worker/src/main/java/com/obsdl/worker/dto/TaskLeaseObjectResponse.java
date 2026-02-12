package com.obsdl.worker.dto;

public record TaskLeaseObjectResponse(
        String objectKey,
        Long size,
        String etag,
        String endpoint,
        String ak,
        String sk,
        String bucket
) {
}
