package com.obsdl.master.dto.ros;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RosStartRequest(
        @NotNull RosVersion rosVersion,
        @NotBlank String bagPath,
        @NotNull Boolean loop,
        @NotNull Boolean useSimTime,
        @NotNull @DecimalMin(value = "0.1") Double rate,
        @NotNull @Positive Integer port
) {
}
