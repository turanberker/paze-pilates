package com.paze.pilates.dto;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
