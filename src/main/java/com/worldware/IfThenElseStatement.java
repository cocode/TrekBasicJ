package com.worldware;

/**
 * Represents an IF...THEN...ELSE statement
 */
public class IfThenElseStatement extends IfThenStatement {
    private final String elseStatements;
    
    public IfThenElseStatement(String keyword, String condition, String thenStatements, String elseStatements) {
        super(keyword, condition, thenStatements);
        this.elseStatements = elseStatements;
    }
    
    /**
     * Get the statements to execute if condition is false
     */
    public String getElseStatements() {
        return elseStatements;
    }
    
    @Override
    public String toString() {
        return getKeyword() + " " + getCondition() + " THEN " + getThenStatements() + " ELSE " + elseStatements;
    }
} 