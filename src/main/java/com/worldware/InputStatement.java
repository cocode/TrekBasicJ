package com.worldware;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents an INPUT statement in BASIC
 * Handles syntax like: INPUT A, INPUT "Prompt"; A, INPUT A,B,C
 */
public class InputStatement extends BasicStatement {
    private String prompt;
    private List<String> variables;
    
    public InputStatement(String keyword, String args) throws BasicSyntaxError {
        super(keyword, args);
        parseInputStatement(args);
    }
    
    private void parseInputStatement(String args) throws BasicSyntaxError {
        args = args.trim();
        
        // Handle prompt string (e.g., INPUT "Enter value"; A)
        if (args.startsWith("\"")) {
            int endQuote = args.indexOf("\"", 1);
            if (endQuote == -1) {
                throw new BasicSyntaxError("Unterminated string in INPUT statement");
            }
            
            prompt = args.substring(1, endQuote);
            
            // Check for semicolon or comma after prompt
            String remainder = args.substring(endQuote + 1).trim();
            if (remainder.startsWith(";") || remainder.startsWith(",")) {
                remainder = remainder.substring(1).trim();
            }
            
            if (!remainder.isEmpty()) {
                parseVariables(remainder);
            } else {
                throw new BasicSyntaxError("INPUT statement requires variable(s) after prompt");
            }
        } else {
            // No prompt, just variables
            prompt = null;
            parseVariables(args);
        }
    }
    
    private void parseVariables(String variableList) throws BasicSyntaxError {
        variables = new ArrayList<>();
        
        if (variableList.trim().isEmpty()) {
            throw new BasicSyntaxError("INPUT statement requires at least one variable");
        }
        
        String[] varNames = variableList.split(",");
        for (String varName : varNames) {
            String trimmed = varName.trim();
            if (trimmed.isEmpty()) {
                throw new BasicSyntaxError("Empty variable name in INPUT statement");
            }
            
            // Validate variable name (basic validation)
            if (!trimmed.matches("[A-Za-z][A-Za-z0-9]*\\$?")) {
                throw new BasicSyntaxError("Invalid variable name in INPUT: " + trimmed);
            }
            
            variables.add(trimmed.toUpperCase());
        }
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public List<String> getVariables() {
        return variables;
    }
    
    public boolean hasPrompt() {
        return prompt != null;
    }
} 