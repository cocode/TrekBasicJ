package com.worldware;

/**
 * Represents an IF statement
 */
public class IfStatement extends BasicStatement {
    
    public IfStatement(String keyword, String args) {
        super(keyword, args);
    }
    
    /**
     * Get the condition expression
     */
    public String getCondition() {
        return getArgs();
    }
} 