package com.obsdl.worker.dto;

public record ApiResponse<T>(
        Integer code,
        String message,
        T data
) {
}
