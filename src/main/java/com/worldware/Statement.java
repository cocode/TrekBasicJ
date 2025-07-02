package com.worldware;

/**
 * Base interface for all BASIC statements
 */
public interface Statement {
    /**
     * Get the keyword for this statement (e.g., "PRINT", "IF", "GOTO")
     */
    String getKeyword();
    
    /**
     * Get the arguments/parameters for this statement
     */
    String getArgs();
    
    /**
     * Get a string representation of this statement for output/debugging
     */
    @Override
    String toString();
} 