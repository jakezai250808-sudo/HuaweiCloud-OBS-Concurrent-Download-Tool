package com.obsdl.worker.dto;

public record TaskReportRequest(
        String workerId,
        String objectKey,
        String status,
        String errorMessage
) {
}
