package com.medislot.exception;

import org.springframework.http.HttpStatus;

/** Base exception for expected business errors (e.g. SLOT_UNAVAILABLE, NO_HOSPITALS_FOUND). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }

    public String getCode() { return code; }
}