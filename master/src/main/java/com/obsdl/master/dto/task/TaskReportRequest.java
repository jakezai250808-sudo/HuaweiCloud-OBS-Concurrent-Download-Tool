package com.obsdl.master.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TaskReportRequest(
        @NotBlank(message = "workerId 不能为空")
        String workerId,
        @NotBlank(message = "objectKey 不能为空")
        String objectKey,
        @NotBlank(message = "status 不能为空")
        @Pattern(regexp = "done|failed", message = "status 仅支持 done/failed")
        String status,
        String errorMessage
) {
}
