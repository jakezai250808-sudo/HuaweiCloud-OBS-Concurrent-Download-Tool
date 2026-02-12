package com.obsdl.master.dto.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskLeaseRequest(
        @NotBlank(message = "workerId 不能为空")
        String workerId,
        @NotNull(message = "count 不能为空")
        @Min(value = 1, message = "count 必须大于 0")
        Integer count
) {
}
