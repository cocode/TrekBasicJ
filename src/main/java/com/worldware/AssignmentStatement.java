package com.worldware;

/**
 * Represents an assignment statement (LET X = expression)
 */
public class AssignmentStatement extends BasicStatement {
    
    public AssignmentStatement(String keyword, String args) {
        super(keyword, args);
    }
    
    /**
     * Get the variable being assigned to
     */
    public String getVariable() {
        String args = getArgs();
        int equalsIndex = args.indexOf('=');
        if (equalsIndex == -1) {
            return "";
        }
        return args.substring(0, equalsIndex).trim();
    }
    
    /**
     * Get the expression being assigned
     */
    public String getExpression() {
        String args = getArgs();
        int equalsIndex = args.indexOf('=');
        if (equalsIndex == -1) {
            return "";
        }
        return args.substring(equalsIndex + 1).trim();
    }
    
    @Override
    public String toString() {
        return getArgs(); // For LET statements, just show the assignment
    }
} 