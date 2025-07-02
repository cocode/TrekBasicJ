package com.worldware;

/**
 * Represents a FOR statement
 */
public class ForStatement extends BasicStatement {
    
    public ForStatement(String keyword, String args) {
        super(keyword, args);
    }
    
    /**
     * Get the loop variable
     */
    public String getIndexVariable() {
        String args = getArgs();
        int equalsIndex = args.indexOf('=');
        if (equalsIndex == -1) {
            return "";
        }
        return args.substring(0, equalsIndex).trim();
    }
    
    /**
     * Get the start expression
     */
    public String getStartExpression() {
        String args = getArgs();
        int equalsIndex = args.indexOf('=');
        int toIndex = args.toUpperCase().indexOf("TO");
        if (equalsIndex == -1 || toIndex == -1) {
            return "";
        }
        return args.substring(equalsIndex + 1, toIndex).trim();
    }
    
    /**
     * Get the end expression
     */
    public String getEndExpression() {
        String args = getArgs();
        int toIndex = args.toUpperCase().indexOf("TO");
        int stepIndex = args.toUpperCase().indexOf("STEP");
        if (toIndex == -1) {
            return "";
        }
        int endIndex = stepIndex != -1 ? stepIndex : args.length();
        return args.substring(toIndex + 2, endIndex).trim();
    }
    
    /**
     * Get the step expression (defaults to "1" if not specified)
     */
    public String getStepExpression() {
        String args = getArgs();
        int stepIndex = args.toUpperCase().indexOf("STEP");
        if (stepIndex == -1) {
            return "1";
        }
        return args.substring(stepIndex + 4).trim();
    }
} 