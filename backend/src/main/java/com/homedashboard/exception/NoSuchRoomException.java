package com.homedashboard.exception;

import org.springframework.http.HttpStatus;

public class NoSuchRoomException extends BaseException {

    public NoSuchRoomException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }
}
