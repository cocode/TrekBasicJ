package com.worldware;

/**
 * Represents a DEF FN statement in BASIC
 * Handles syntax like: DEF FNA(X)=X*X, DEF FNS$(A$)=LEFT$(A$,5)
 */
public class DefStatement extends BasicStatement {
    private String functionName;
    private String parameterName;
    private String expression;
    
    public DefStatement(String keyword, String args) throws BasicSyntaxError {
        super(keyword, args);
        parseDefStatement(args);
    }
    
    private void parseDefStatement(String args) throws BasicSyntaxError {
        args = args.trim();
        
        // Parse DEF FNname(parameter)=expression
        // Example: DEF FNA(X)=X*X
        
        // Find the equals sign
        int equalsIndex = args.indexOf('=');
        if (equalsIndex == -1) {
            throw new BasicSyntaxError("DEF statement must contain '='");
        }
        
        String beforeEquals = args.substring(0, equalsIndex).trim();
        this.expression = args.substring(equalsIndex + 1).trim();
        
        // Parse function name and parameter
        // Format: FNname(parameter)
        int openParen = beforeEquals.indexOf('(');
        int closeParen = beforeEquals.indexOf(')');
        
        if (openParen == -1 || closeParen == -1 || closeParen <= openParen) {
            throw new BasicSyntaxError("Invalid DEF function syntax: " + args);
        }
        
        this.functionName = beforeEquals.substring(0, openParen).trim().toUpperCase();
        this.parameterName = beforeEquals.substring(openParen + 1, closeParen).trim().toUpperCase();
        
        // Validate function name starts with FN
        if (!functionName.startsWith("FN")) {
            throw new BasicSyntaxError("User-defined function must start with FN: " + functionName);
        }
        
        // Validate parameter name
        if (!parameterName.matches("[A-Z][A-Z0-9]*\\$?")) {
            throw new BasicSyntaxError("Invalid parameter name: " + parameterName);
        }
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public String getExpression() {
        return expression;
    }
    
    @Override
    public String toString() {
        return "DEF " + functionName + "(" + parameterName + ")=" + expression;
    }
} 