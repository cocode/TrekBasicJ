package com.worldware;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Partial translation of python_tests/test_basic.py.
 * Currently only covers the trace-file generation case.
 */
public class BasicTest extends TestCaseBase {

    private static final String TRACE_FILE_NAME = "tracefile.txt";

    @Test
    public void testTraceFileGenerated() throws Exception {
        // BASIC listing (simple – just enough to exercise a few statements)
        List<String> listing = List.of(
                "100 PRINT:PRINT:PRINT:PRINT",
                "110 J=3+2",
                "120 PRINTJ"
        );

        // Ensure any previous run is cleared
        File trace = new File(TRACE_FILE_NAME);
        if (trace.exists()) trace.delete();

        // Run with tracing enabled (our TestCaseBase helper forwards the flag)
        Program program = BasicLoader.tokenize(listing);
        Executor executor = new Executor(program, true); // true => trace enabled
        executor.runProgram();
        executor.close();

        // Restore stdout capture
        restoreOutput();

        // Verify the trace file now exists and is non-empty
        assertTrue(trace.exists(), "Trace file should be created when trace flag is on");
        assertTrue(trace.length() > 0, "Trace file should contain data");
    }
} 