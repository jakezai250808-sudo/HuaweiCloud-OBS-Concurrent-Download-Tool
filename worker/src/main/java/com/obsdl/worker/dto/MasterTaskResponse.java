package com.obsdl.worker.dto;

import java.util.List;

public record MasterTaskResponse(
        Long id,
        Long accountId,
        String bucket,
        String status,
        String createdAt,
        List<MasterTaskObjectResponse> objects
) {
}
