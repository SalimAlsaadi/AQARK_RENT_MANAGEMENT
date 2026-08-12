package com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}