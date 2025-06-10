package com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        String message;
        HttpStatus status;

        if (ex instanceof ResourceNotFoundException) {
            message = ex.getMessage();
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof InvalidInputException) {
            message = ex.getMessage();
            status = HttpStatus.BAD_REQUEST;
        } else {
            message = "An unexpected error occurred: " + ex.getMessage();
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(message, status);
    }
}
