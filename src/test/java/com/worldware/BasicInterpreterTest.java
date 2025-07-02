package com.worldware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicInterpreterTest extends TestCaseBase {

    @AfterEach
    public void tearDown() {
        restoreOutput();
    }

    @Test
    public void testAssignment() throws Exception {
        // Test basic variable assignment
        List<String> listing = Arrays.asList("100 Z$=\"Fred\"");
        Executor executor = runIt(listing);
        assertEquals(1, executor.getSymbolCount());
        assertEquals("Fred", executor.getSymbol("Z$"));
    }

    @Test
    public void testAssignmentCase() throws Exception {
        // Test that variable names are case insensitive
        List<String> listing = Arrays.asList("100 z$=\"Fred\"");
        Executor executor = runIt(listing);
        assertEquals(1, executor.getSymbolCount());
        assertEquals("Fred", executor.getSymbol("Z$")); // Should be accessible as uppercase
    }

    @Test
    public void testMultipleAssignments() throws Exception {
        List<String> listing = Arrays.asList(
            "100 Z$=\"FrEd\"",
            "110 z$=\"fReD\""
        );
        Executor executor = runIt(listing);
        assertEquals(1, executor.getSymbolCount());
        // Second value should overwrite first, if variables are case-insensitive
        assertEquals("fReD", executor.getSymbol("Z$"));
    }

    @Test
    public void testNumericAssignment() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=5",
            "110 B=6"
        );
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "A", 5);
        assertValue(executor, "B", 6);
    }

    @Test
    public void testPrintString() throws Exception {
        List<String> listing = Arrays.asList("100 PRINT \"Hello World\"");
        ExecutorOutput result = runItCapture(listing);
        String output = result.output.trim();
        assertEquals("Hello World", output);
    }

    @Test
    public void testPrintVariable() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=42",
            "110 PRINT A"
        );
        ExecutorOutput result = runItCapture(listing);
        String output = result.output.trim();
        assertEquals("42", output);
    }

    @Test
    public void testGoto() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=1",
            "110 GOTO 130",
            "120 A=2",
            "130 B=3"
        );
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "A", 1); // Should not be changed to 2
        assertValue(executor, "B", 3);
    }

    @Test
    public void testEnd() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=1",
            "110 END",
            "120 A=2"
        );
        Executor executor = runIt(listing);
        RunStatus status = executor.runProgram();
        assertEquals(RunStatus.END_CMD, status);
        assertEquals(1, executor.getSymbolCount());
        assertValue(executor, "A", 1); // Should not execute line 120
    }

    @Test
    public void testMultipleStatementsOnLine() throws Exception {
        List<String> listing = Arrays.asList("100 A=5:B=6");
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "A", 5);
        assertValue(executor, "B", 6);
    }

    @Test
    public void testGosubReturn() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=1",
            "110 GOSUB 130",
            "120 A=3",
            "130 B=2",
            "140 RETURN"
        );
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "A", 3); // Should execute after return
        assertValue(executor, "B", 2);
    }
} 