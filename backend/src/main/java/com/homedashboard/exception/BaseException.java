package com.homedashboard.exception;

import org.springframework.http.HttpStatus;

public class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public BaseException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public HttpStatus getHttpStatus() {
        return this.status;
    }
}
