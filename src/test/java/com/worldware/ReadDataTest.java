package com.worldware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReadDataTest extends TestCaseBase {

    @AfterEach
    public void tearDown() {
        restoreOutput();
    }

    @Test
    public void testSimpleReadData() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA 10,20,30",
            "110 READ A,B,C"
        );
        Executor executor = runIt(listing);
        assertEquals(10, executor.getSymbol("A"));
        assertEquals(20, executor.getSymbol("B"));
        assertEquals(30, executor.getSymbol("C"));
    }

    @Test
    public void testStringData() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA \"Hello\",\"World\"",
            "110 READ A$,B$"
        );
        Executor executor = runIt(listing);
        assertEquals("Hello", executor.getSymbol("A$"));
        assertEquals("World", executor.getSymbol("B$"));
    }

    @Test
    public void testMixedDataTypes() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA 42,\"Forty-two\",3.14",
            "110 READ NUM,STR,PI"
        );
        Executor executor = runIt(listing);
        assertEquals(42, executor.getSymbol("NUM"));
        assertEquals("Forty-two", executor.getSymbol("STR"));
        assertEquals(3.14, (Double) executor.getSymbol("PI"), 0.001);
    }

    @Test
    public void testMultipleDataStatements() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA 1,2",
            "110 DATA 3,4",
            "120 READ A,B,C,D"
        );
        Executor executor = runIt(listing);
        assertEquals(1, executor.getSymbol("A"));
        assertEquals(2, executor.getSymbol("B"));
        assertEquals(3, executor.getSymbol("C"));
        assertEquals(4, executor.getSymbol("D"));
    }

    @Test
    public void testRestore() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA 10,20",
            "110 READ A,B",
            "120 RESTORE",
            "130 READ C,D"
        );
        Executor executor = runIt(listing);
        assertEquals(10, executor.getSymbol("A"));
        assertEquals(20, executor.getSymbol("B"));
        assertEquals(10, executor.getSymbol("C"));
        assertEquals(20, executor.getSymbol("D"));
    }

    @Test
    public void testOutOfData() throws Exception {
        List<String> listing = Arrays.asList(
            "100 DATA 10",
            "110 READ A,B"
        );
        assertThrows(BasicRuntimeError.class, () -> {
            runIt(listing);
        });
    }
} 