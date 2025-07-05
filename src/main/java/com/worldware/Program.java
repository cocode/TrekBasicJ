package com.worldware;

import java.util.*;

/**
 * Encapsulates a BASIC program as a collection of ProgramLine objects.
 * Provides methods for navigation, line lookup, and program modification
 * while hiding the internal list implementation details.
 */
public class Program implements java.lang.Iterable<ProgramLine> {
    private final List<ProgramLine> lines;
    private final Map<Integer, Integer> lineToIndex;

    /**
     * Initialize with a list of ProgramLine objects.
     * Lines should be in ascending line number order.
     */
    public Program(List<ProgramLine> programLines) {
        // Store the lines
        this.lines = new ArrayList<>(programLines);
        
        // Build line number to index mapping for fast lookup
        this.lineToIndex = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            lineToIndex.put(lines.get(i).getLine(), i);
        }
    }

    /**
     * Get line by index in the program
     */
    public ProgramLine getLine(int index) {
        return lines.get(index);
    }

    /**
     * Get the index of the next line, or null if at end of program
     */
    public Integer getNextIndex(int currentIndex) {
        int nextIndex = currentIndex + 1;
        return nextIndex < lines.size() ? nextIndex : null;
    }

    /**
     * Get the next statement location after the current one
     */
    public ControlLocation getNextStatementLocation(int currentIndex, int currentOffset) {
        ProgramLine currentLine = lines.get(currentIndex);
        int nextOffset = currentOffset + 1;
        
        if (nextOffset < currentLine.getStmts().size()) {
            // More statements on current line
            return new ControlLocation(currentIndex, nextOffset);
        } else {
            // Move to next line
            Integer nextIndex = getNextIndex(currentIndex);
            if (nextIndex != null) {
                return new ControlLocation(nextIndex, 0);
            } else {
                return null; // End of program
            }
        }
    }

    /**
     * Find the index of a line by line number
     */
    public int findLineIndex(int lineNumber) throws BasicSyntaxError {
        Integer index = lineToIndex.get(lineNumber);
        if (index == null) {
            throw new BasicSyntaxError("Line " + lineNumber + " not found");
        }
        return index;
    }

    /**
     * Get a range of lines as strings for display
     */
    public List<String> getLinesRange(int startIndex, Integer count) {
        List<String> result = new ArrayList<>();
        int endIndex = count != null ? Math.min(startIndex + count, lines.size()) : lines.size();
        
        for (int i = startIndex; i < endIndex; i++) {
            result.add(lines.get(i).getSource());
        }
        return result;
    }

    /**
     * Get the number of lines in the program
     */
    public int size() {
        return lines.size();
    }

    /**
     * Check if the program is empty
     */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * Get an iterator over the program lines
     */
    public Iterator<ProgramLine> iterator() {
        return lines.iterator();
    }

    /**
     * Get a program line by index (for array-like access)
     */
    public ProgramLine get(int index) {
        return lines.get(index);
    }
} 