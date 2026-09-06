package com.homedashboard.exception;

import org.springframework.http.HttpStatus;

public class MeasurementNotFoundException extends BaseException {

    public MeasurementNotFoundException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }
}
