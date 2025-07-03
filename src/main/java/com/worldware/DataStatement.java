package com.worldware;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a DATA statement in BASIC
 * Handles syntax like: DATA 1,2,3, DATA "Hello","World"
 */
public class DataStatement extends BasicStatement {
    private List<Object> dataValues;
    
    public DataStatement(String keyword, String args) throws BasicSyntaxError {
        super(keyword, args);
        parseDataValues(args);
    }
    
    private void parseDataValues(String args) throws BasicSyntaxError {
        dataValues = new ArrayList<>();
        
        if (args.trim().isEmpty()) {
            return; // Empty DATA statement is valid
        }
        
        // Parse comma-separated values
        String[] parts = args.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new BasicSyntaxError("Empty value in DATA statement");
            }
            
            // Parse string literals
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                dataValues.add(trimmed.substring(1, trimmed.length() - 1));
            } else {
                // Parse numeric values
                try {
                    if (trimmed.contains(".")) {
                        dataValues.add(Double.parseDouble(trimmed));
                    } else {
                        dataValues.add(Integer.parseInt(trimmed));
                    }
                } catch (NumberFormatException e) {
                    throw new BasicSyntaxError("Invalid numeric value in DATA: " + trimmed);
                }
            }
        }
    }
    
    public List<Object> getDataValues() {
        return dataValues;
    }
} 