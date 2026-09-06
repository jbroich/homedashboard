package com.homedashboard.web;

import java.time.Instant;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.homedashboard.exception.BaseException;
import com.homedashboard.model.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(ex, request);
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String parameterName = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : "unknown";

        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("INVALID_REQUEST_PARAMETER");
        errorResponse.setMessage("Invalid value for parameter '" + parameterName + "'.");
        errorResponse.setPath(path);
        errorResponse.setStatus(status.value());
        errorResponse.setTimestamp(Instant.now());
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    private ErrorResponse buildErrorResponse(BaseException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError(ex.getErrorCode());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setStatus(ex.getHttpStatus().value());
        errorResponse.setTimestamp(Instant.now());
        return errorResponse;
    }
}
