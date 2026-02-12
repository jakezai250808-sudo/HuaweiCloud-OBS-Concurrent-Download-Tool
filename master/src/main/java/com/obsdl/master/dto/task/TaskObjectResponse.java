package com.obsdl.master.dto.task;

public record TaskObjectResponse(
        String objectKey,
        String status,
        long size
) {
}
