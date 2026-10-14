package com.paze.pilates.dto;

public class BusinessException extends RuntimeException {

    private final transient Object[] args;

    public BusinessException(String message) {
        this(message, new Object[0]);
    }

    /** @param args mesaj anahtarındaki {0}, {1} yer tutucuları için değerler */
    public BusinessException(String message, Object... args) {
        super(message);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
