package com.homedashboard.exception;

import org.springframework.http.HttpStatus;

public class NoSuchRangeException extends BaseException{

    public NoSuchRangeException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }
}
