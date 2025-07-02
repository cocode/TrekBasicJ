package com.worldware;

/**
 * Represents a PRINT statement
 */
public class PrintStatement extends BasicStatement {
    
    public PrintStatement(String keyword, String args) {
        super(keyword, args);
    }
    
    @Override
    public String toString() {
        if (getArgs().isEmpty()) {
            return getKeyword();
        } else {
            return getKeyword() + " " + getArgs();
        }
    }
} 