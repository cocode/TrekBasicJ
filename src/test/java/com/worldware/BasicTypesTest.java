package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicTypesTest {

    @Test
    public void testBasicStatement() {
        BasicStatement stmt = new BasicStatement("PRINT", "\"Hello World\"");
        assertEquals("PRINT", stmt.getKeyword());
        assertEquals("\"Hello World\"", stmt.getArgs());
        assertEquals("PRINT \"Hello World\"", stmt.toString());
    }

    @Test
    public void testBasicStatementNoArgs() {
        BasicStatement stmt = new BasicStatement("END", "");
        assertEquals("END", stmt.getKeyword());
        assertEquals("", stmt.getArgs());
        assertEquals("END", stmt.toString());
    }

    @Test
    public void testProgramLine() {
        List<Statement> statements = Arrays.asList(
            new BasicStatement("PRINT", "\"Hello\""),
            new BasicStatement("END", "")
        );
        ProgramLine line = new ProgramLine(100, statements, "100 PRINT \"Hello\":END");
        
        assertEquals(100, line.getLine());
        assertEquals(2, line.getStmts().size());
        assertEquals("100 PRINT \"Hello\":END", line.getSource());
        assertTrue(line.toString().contains("ProgramLine(line=100"));
    }

    @Test
    public void testProgram() {
        List<ProgramLine> lines = Arrays.asList(
            new ProgramLine(10, Arrays.asList(new BasicStatement("PRINT", "\"Line 10\"")), "10 PRINT \"Line 10\""),
            new ProgramLine(20, Arrays.asList(new BasicStatement("END", "")), "20 END")
        );
        Program program = new Program(lines);
        
        assertEquals(2, program.size());
        assertFalse(program.isEmpty());
        assertEquals(10, program.getLine(0).getLine());
        assertEquals(20, program.getLine(1).getLine());
    }

    @Test
    public void testControlLocation() {
        ControlLocation loc = new ControlLocation(5, 2);
        assertEquals(Integer.valueOf(5), loc.getIndex());
        assertEquals(2, loc.getOffset());
        
        ControlLocation loc2 = new ControlLocation(5, 2);
        assertEquals(loc, loc2);
        assertEquals(loc.hashCode(), loc2.hashCode());
    }

    @Test
    public void testRunStatus() {
        assertEquals("RUN", RunStatus.RUN.name());
        assertEquals("END_OF_PROGRAM", RunStatus.END_OF_PROGRAM.name());
    }

    @Test
    public void testExceptions() {
        BasicSyntaxError syntaxError = new BasicSyntaxError("Syntax error", 10);
        assertEquals("Syntax error", syntaxError.getMessage());
        assertEquals(Integer.valueOf(10), syntaxError.getLineNumber());
        
        BasicRuntimeError runtimeError = new BasicRuntimeError("Runtime error");
        assertEquals("Runtime error", runtimeError.getMessage());
        assertNull(runtimeError.getLineNumber());
    }
} 