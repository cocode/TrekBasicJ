package com.worldware;

/**
 * Represents an IF...THEN statement
 */
public class IfThenStatement extends IfStatement {
    private final String thenStatements;
    
    public IfThenStatement(String keyword, String condition, String thenStatements) {
        super(keyword, condition);
        this.thenStatements = thenStatements;
    }
    
    /**
     * Get the condition expression
     */
    @Override
    public String getCondition() {
        return getArgs();
    }
    
    /**
     * Get the statements to execute if condition is true
     */
    public String getThenStatements() {
        return thenStatements;
    }
    
    @Override
    public String toString() {
        return getKeyword() + " " + getCondition() + " THEN " + thenStatements;
    }
} 