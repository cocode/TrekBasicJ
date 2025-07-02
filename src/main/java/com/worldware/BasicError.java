package com.worldware;

/**
 * Base exception for all BASIC interpreter errors
 */
public class BasicError extends Exception {
    protected final String message;
    protected final Integer lineNumber;

    public BasicError(String message) {
        this(message, null);
    }

    public BasicError(String message, Integer lineNumber) {
        super(message);
        this.message = message;
        this.lineNumber = lineNumber;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
} 