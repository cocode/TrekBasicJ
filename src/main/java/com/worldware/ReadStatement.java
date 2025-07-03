package com.worldware;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a READ statement in BASIC
 * Handles syntax like: READ A, READ A,B,C$, READ A$
 */
public class ReadStatement extends BasicStatement {
    private List<String> variables;
    
    public ReadStatement(String keyword, String args) throws BasicSyntaxError {
        super(keyword, args);
        parseVariables(args);
    }
    
    private void parseVariables(String args) throws BasicSyntaxError {
        variables = new ArrayList<>();
        
        if (args.trim().isEmpty()) {
            throw new BasicSyntaxError("READ statement requires at least one variable");
        }
        
        // Parse comma-separated variables
        String[] parts = args.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new BasicSyntaxError("Empty variable name in READ statement");
            }
            
            // Validate variable name
            if (!trimmed.matches("[A-Za-z][A-Za-z0-9_]*\\$?")) {
                throw new BasicSyntaxError("Invalid variable name in READ: " + trimmed);
            }
            
            variables.add(trimmed.toUpperCase());
        }
    }
    
    public List<String> getVariables() {
        return variables;
    }
} 