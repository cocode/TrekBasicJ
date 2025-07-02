package com.worldware;

/**
 * Exception thrown for internal interpreter errors
 */
public class BasicInternalError extends RuntimeException {
    
    public BasicInternalError(String message) {
        super(message);
    }
} 