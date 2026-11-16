package com.promoquoter.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for business errors that map to a specific HTTP status and a stable, machine-readable code.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    protected ApiException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
