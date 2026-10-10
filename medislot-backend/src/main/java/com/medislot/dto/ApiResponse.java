package com.medislot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Standard envelope for every API response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, String code, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, data, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, message, code, null, Instant.now());
    }
}