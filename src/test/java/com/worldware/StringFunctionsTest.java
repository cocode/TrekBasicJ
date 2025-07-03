package com.worldware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class StringFunctionsTest extends TestCaseBase {

    @AfterEach
    public void tearDown() {
        restoreOutput();
    }

    @Test
    public void testLenFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"Hello World\"",
            "110 B=LEN(A$)"
        );
        Executor executor = runIt(listing);
        assertEquals(11, executor.getSymbol("B"));
    }

    @Test
    public void testLeftFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"Hello World\"",
            "110 B$=LEFT$(A$,5)"
        );
        Executor executor = runIt(listing);
        assertEquals("Hello", executor.getSymbol("B$"));
    }

    @Test
    public void testRightFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"Hello World\"",
            "110 B$=RIGHT$(A$,5)"
        );
        Executor executor = runIt(listing);
        assertEquals("World", executor.getSymbol("B$"));
    }

    @Test
    public void testMidFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"Hello World\"",
            "110 B$=MID$(A$,7,5)"
        );
        Executor executor = runIt(listing);
        assertEquals("World", executor.getSymbol("B$"));
    }

    @Test
    public void testMidFunctionWithoutLength() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"Hello World\"",
            "110 B$=MID$(A$,7)"
        );
        Executor executor = runIt(listing);
        assertEquals("World", executor.getSymbol("B$"));
    }

    @Test
    public void testEmptyString() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"\"",
            "110 B=LEN(A$)",
            "120 C$=LEFT$(A$,5)",
            "130 D$=RIGHT$(A$,5)",
            "140 E$=MID$(A$,1,5)"
        );
        Executor executor = runIt(listing);
        assertEquals(0, executor.getSymbol("B"));
        assertEquals("", executor.getSymbol("C$"));
        assertEquals("", executor.getSymbol("D$"));
        assertEquals("", executor.getSymbol("E$"));
    }

    @Test
    public void testBoundaryConditions() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A$=\"ABC\"",
            "110 B$=LEFT$(A$,5)",
            "120 C$=RIGHT$(A$,5)",
            "130 D$=MID$(A$,5,3)"
        );
        Executor executor = runIt(listing);
        assertEquals("ABC", executor.getSymbol("B$"));
        assertEquals("ABC", executor.getSymbol("C$"));
        assertEquals("", executor.getSymbol("D$"));
    }
} 