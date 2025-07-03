package com.worldware;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a DIM statement for array declarations
 */
public class DimStatement extends BasicStatement {
    private final List<ArrayDeclaration> arrayDeclarations;
    
    public DimStatement(String keyword, String args) throws BasicSyntaxError {
        super(keyword, args);
        this.arrayDeclarations = parseArrayDeclarations(args);
    }
    
    private List<ArrayDeclaration> parseArrayDeclarations(String args) throws BasicSyntaxError {
        List<ArrayDeclaration> declarations = new ArrayList<>();
        
        // Split by commas to handle multiple arrays in one DIM statement
        // But don't split commas inside parentheses
        List<String> parts = smartSplitDeclarations(args);
        
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                declarations.add(parseArrayDeclaration(part));
            }
        }
        
        return declarations;
    }
    
    private List<String> smartSplitDeclarations(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '(') {
                parenDepth++;
                current.append(c);
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0) {
                // Split here - comma outside parentheses
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        
        return parts;
    }
    
    private ArrayDeclaration parseArrayDeclaration(String declaration) throws BasicSyntaxError {
        // Parse something like "A(8)" or "C(3,2)"
        int openParen = declaration.indexOf('(');
        int closeParen = declaration.lastIndexOf(')');
        
        if (openParen == -1 || closeParen == -1 || openParen >= closeParen) {
            throw new BasicSyntaxError("Invalid array declaration: " + declaration);
        }
        
        String arrayName = declaration.substring(0, openParen).trim().toUpperCase();
        String dimensionsPart = declaration.substring(openParen + 1, closeParen).trim();
        
        // Parse dimensions
        String[] dimensionStrs = dimensionsPart.split(",");
        List<Integer> dimensions = new ArrayList<>();
        
        for (String dimStr : dimensionStrs) {
            try {
                int dimension = Integer.parseInt(dimStr.trim());
                dimensions.add(dimension);
            } catch (NumberFormatException e) {
                throw new BasicSyntaxError("Invalid array dimension: " + dimStr);
            }
        }
        
        return new ArrayDeclaration(arrayName, dimensions);
    }
    
    public List<ArrayDeclaration> getArrayDeclarations() {
        return arrayDeclarations;
    }
    
    public static class ArrayDeclaration {
        private final String name;
        private final List<Integer> dimensions;
        
        public ArrayDeclaration(String name, List<Integer> dimensions) {
            this.name = name;
            this.dimensions = dimensions;
        }
        
        public String getName() {
            return name;
        }
        
        public List<Integer> getDimensions() {
            return dimensions;
        }
        
        /**
         * Create a multi-dimensional array with the specified dimensions
         */
        public Object createArray() {
            boolean isString = name.endsWith("$");
            return createArrayRecursive(dimensions, 0, isString);
        }
        
        private Object createArrayRecursive(List<Integer> dims, int index, boolean isString) {
            if (index >= dims.size()) {
                return isString ? "" : 0;
            }
            
            int size = dims.get(index) + 1 - Dialect.ARRAY_OFFSET;
            Object[] array = new Object[size];
            
            for (int i = 0; i < size; i++) {
                array[i] = createArrayRecursive(dims, index + 1, isString);
            }
            
            return array;
        }
    }
} 