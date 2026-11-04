package com.agroprecision.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to do that."); }
    public static ApiException bad(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "Record not found."); }
}
