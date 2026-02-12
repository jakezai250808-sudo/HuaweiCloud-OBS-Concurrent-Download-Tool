package com.obsdl.master.dto.task;

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
