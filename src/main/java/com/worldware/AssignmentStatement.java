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
    
    /**
     * Check if this is an array assignment (e.g., A(1) = 5)
     */
    public boolean isArrayAssignment() {
        String variable = getVariable();
        return variable.contains("(") && variable.contains(")");
    }
    
    /**
     * Get the array name (e.g., "A" from "A(1)")
     */
    public String getArrayName() {
        if (!isArrayAssignment()) {
            return null;
        }
        
        String variable = getVariable();
        int openParen = variable.indexOf('(');
        return variable.substring(0, openParen).trim().toUpperCase();
    }
    
    /**
     * Get the array indices (e.g., "1" from "A(1)" or "1,2" from "C(1,2)")
     */
    public String getArrayIndices() {
        if (!isArrayAssignment()) {
            return null;
        }
        
        String variable = getVariable();
        int openParen = variable.indexOf('(');
        int closeParen = variable.lastIndexOf(')');
        
        if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
            return variable.substring(openParen + 1, closeParen).trim();
        }
        
        return null;
    }

    @Override
    public String toString() {
        return getArgs(); // For LET statements, just show the assignment
    }
} 