package com.worldware;

/**
 * Exception thrown for BASIC runtime errors
 */
public class BasicRuntimeError extends BasicError {
    
    public BasicRuntimeError(String message) {
        super(message);
    }

    public BasicRuntimeError(String message, Integer lineNumber) {
        super(message, lineNumber);
    }
} 