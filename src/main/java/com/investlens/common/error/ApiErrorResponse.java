package com.investlens.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return new ApiErrorResponse(code.name(), message, path, Instant.now(), Map.of());
    }
}
