package com.worldware;

/**
 * Exception thrown for BASIC syntax errors
 */
public class BasicSyntaxError extends BasicError {
    
    public BasicSyntaxError(String message) {
        super(message);
    }

    public BasicSyntaxError(String message, Integer lineNumber) {
        super(message, lineNumber);
    }
} 