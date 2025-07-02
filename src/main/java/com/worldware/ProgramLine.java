package com.worldware;

import java.util.List;
import java.util.Objects;

/**
 * Represents one line in a BASIC program, which may be composed of multiple statements
 */
public class ProgramLine {
    private final int line;          // The line number, e.g., "100" in "100 PRINT:PRINT:END"
    private final List<Statement> stmts;  // A list of statements
    private final String source;     // The original line as a string

    public ProgramLine(int line, List<Statement> stmts, String source) {
        this.line = line;
        this.stmts = List.copyOf(stmts);  // Make immutable copy
        this.source = source;
    }

    public int getLine() {
        return line;
    }

    public List<Statement> getStmts() {
        return stmts;
    }

    public String getSource() {
        return source;
    }

    /**
     * Compare equality based on (line, stmts), ignoring source
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramLine that = (ProgramLine) o;
        return line == that.line && Objects.equals(stmts, that.stmts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, stmts);
    }

    @Override
    public String toString() {
        StringBuilder stmtsStr = new StringBuilder();
        for (int i = 0; i < stmts.size(); i++) {
            if (i > 0) stmtsStr.append(", ");
            stmtsStr.append(stmts.get(i));
        }
        return String.format("ProgramLine(line=%d, stmts=[%s], source=\"%s\")", 
                           line, stmtsStr, source);
    }
} 