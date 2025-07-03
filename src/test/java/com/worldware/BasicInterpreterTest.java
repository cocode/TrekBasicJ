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
            "115 END",
            "120 A=3",
            "130 B=2",
            "140 RETURN"
        );
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "A", 1); // Should NOT execute line 120 due to END
        assertValue(executor, "B", 2);
    }

    @Test
    public void testDimStatement() throws Exception {
        List<String> listing = Arrays.asList("100 DIM A(8), C(3, 2)");
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        Object arrAObj = executor.getSymbol("ARRAY:A");
        Object arrCObj = executor.getSymbol("ARRAY:C");
        assertNotNull(arrAObj);
        assertNotNull(arrCObj);
        Object[] arrA = (Object[]) arrAObj;
        assertEquals(8, arrA.length);

        Object[] arrC = (Object[]) arrCObj;
        assertEquals(3, arrC.length);
        Object firstRow = arrC[0];
        assertTrue(firstRow instanceof Object[]);
        assertEquals(2, ((Object[]) firstRow).length);
    }

    @Test
    public void testDefFunctionEvaluation() throws Exception {
        List<String> listing = Arrays.asList(
                "100 DEF FNA(X)=X^2+1",
                "110 Y=FNA(5)",
                "120 Z=FNA(7*7)"
        );
        Executor executor = runIt(listing);
        assertEquals(2, executor.getSymbolCount());
        assertValue(executor, "Y", 26);
        assertValue(executor, "Z", 2402);
    }

    @Test
    public void testNestedDefFunctions() throws Exception {
        List<String> listing = Arrays.asList(
                "100 DEF FNA(X)=X^2",
                "110 DEF FNB(X)=2*X+3",
                "120 DEF FNC(X)=FNA(3*X)+FNB(X+1)",
                "130 Z=FNC(3)"
        );
        Executor executor = runIt(listing);
        assertEquals(1, executor.getSymbolCount());
        assertValue(executor, "Z", 92);
    }

    @Test
    public void testBuiltinIntRndSgnExp() throws Exception {
        List<String> listing = Arrays.asList(
                "100 A=INT(1.99)",
                "110 B=RND(1)",
                "120 C=SGN(-3711)",
                "130 D=EXP(1)"
        );
        Executor executor = runIt(listing);
        assertEquals(4, executor.getSymbolCount());
        assertValue(executor, "A", 1);
        Object rnd = executor.getSymbol("B");
        assertTrue(((Number) rnd).doubleValue() > 0 && ((Number) rnd).doubleValue() < 1.0);
        assertValue(executor, "C", -1);
        assertEquals(Math.E, ((Number) executor.getSymbol("D")).doubleValue(), 1e-5);
    }

    @Test
    public void testStringAndNumericExpressions() throws Exception {
        List<String> listing = Arrays.asList(
                "100 A = 2+1",
                "110 B = 4",
                "120 C = A + B",
                "130 D$ = \"ABC\"",
                "140 E$ = D$ + \"DEF\""
        );
        Executor executor = runIt(listing);
        assertEquals(5, executor.getSymbolCount());
        assertValue(executor, "A", 3);
        assertValue(executor, "B", 4);
        assertValue(executor, "C", 7);
        assertValue(executor, "D$", "ABC");
        assertValue(executor, "E$", "ABCDEF");
    }
} 