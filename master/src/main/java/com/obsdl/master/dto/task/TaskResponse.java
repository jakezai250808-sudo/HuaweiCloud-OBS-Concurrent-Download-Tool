package com.obsdl.master.dto.task;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long id,
        Long accountId,
        String bucket,
        String status,
        Instant createdAt,
        List<TaskObjectResponse> objects
) {
}
