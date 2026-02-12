package com.obsdl.master.dto.account;

public record AccountResponse(
        Long id,
        String name,
        String accessKey,
        String endpoint,
        String bucket
) {
}
