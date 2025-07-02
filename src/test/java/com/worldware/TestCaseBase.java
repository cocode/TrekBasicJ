package com.worldware;

import org.junit.jupiter.api.BeforeEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for BASIC interpreter tests
 */
public class TestCaseBase {
    
    protected ByteArrayOutputStream outputCapture;
    protected PrintStream originalOut;
    
    @BeforeEach
    public void setUp() {
        // Set up output capture for tests that check PRINT statements
        outputCapture = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputCapture));
    }
    
    protected void restoreOutput() {
        System.setOut(originalOut);
    }
    
    protected String getCapturedOutput() {
        return outputCapture.toString();
    }
    
    /**
     * Assert that a symbol has the expected value
     */
    protected void assertValue(Executor executor, String symbol, Object expectedValue) {
        Object value = executor.getSymbol(symbol);
        if (!expectedValue.equals(value)) {
            // Print the full symbol table for debugging
            System.err.println("ASSERTION FAILED: Expected " + symbol + " = " + expectedValue + ", but got " + value);
            System.err.println("Full symbol table:");
            Map<String, Object> symbols = executor.getSymbols();
            for (Map.Entry<String, Object> entry : symbols.entrySet()) {
                System.err.println("  " + entry.getKey() + " = " + entry.getValue());
            }
        }
        assertEquals(expectedValue, value);
    }
    
    /**
     * Verify the symbol table contains the values passed in
     */
    protected void assertValues(Executor executor, Map<String, Object> expectedValues) {
        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            assertValue(executor, entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Run a BASIC program from a list of source lines
     */
    protected Executor runIt(List<String> listing) throws Exception {
        return runIt(listing, false);
    }
    
    /**
     * Run a BASIC program from a list of source lines with optional tracing
     */
    protected Executor runIt(List<String> listing, boolean trace) throws Exception {
        Program program = BasicLoader.tokenize(listing);
        assertEquals(listing.size(), program.size());
        Executor executor = new Executor(program, trace);
        executor.runProgram();
        return executor;
    }
    
    /**
     * Run a program and capture its output
     */
    protected ExecutorOutput runItCapture(List<String> listing) throws Exception {
        Executor executor = runIt(listing);
        String programOutput = getCapturedOutput();
        return new ExecutorOutput(executor, programOutput);
    }
    
    /**
     * Run a program and verify it raises a BasicSyntaxError
     */
    protected void runItSyntaxError(List<String> listing) {
        assertThrows(BasicSyntaxError.class, () -> runIt(listing));
    }
    
    /**
     * Helper class to hold executor and captured output
     */
    protected static class ExecutorOutput {
        public final Executor executor;
        public final String output;
        
        public ExecutorOutput(Executor executor, String output) {
            this.executor = executor;
            this.output = output;
        }
    }
} 