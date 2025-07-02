package com.worldware;

/**
 * Exception thrown when referencing an undefined symbol
 */
public class UndefinedSymbol extends BasicRuntimeError {
    
    public UndefinedSymbol(String message) {
        super(message);
    }
} 