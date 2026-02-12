package com.obsdl.master.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TaskCreateRequest(
        @NotNull Long accountId,
        @NotBlank @Size(max = 64) String bucket,
        @NotNull @Valid Selection selection
) {

    public record Selection(
            @NotEmpty List<@NotBlank String> objects,
            String prefix
    ) {
    }
}
