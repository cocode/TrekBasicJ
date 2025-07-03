package com.worldware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BuiltinFunctionsTest extends TestCaseBase {

    @AfterEach
    public void tearDown() {
        restoreOutput();
    }

    @Test
    public void testIntFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=INT(3.7)",
            "110 B=INT(-2.3)",
            "120 C=INT(5)"
        );
        Executor executor = runIt(listing);
        assertEquals(3, toInt(executor.getSymbol("A")));
        assertEquals(-3, toInt(executor.getSymbol("B")));
        assertEquals(5, toInt(executor.getSymbol("C")));
    }

    @Test
    public void testAbsFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=ABS(-5.5)",
            "110 B=ABS(4.2)",
            "120 C=ABS(0)"
        );
        Executor executor = runIt(listing);
        Object a = executor.getSymbol("A");
        Object b = executor.getSymbol("B");
        Object c = executor.getSymbol("C");
        
        assertEquals(5.5, toDouble(a), 0.001);
        assertEquals(4.2, toDouble(b), 0.001);
        assertEquals(0.0, toDouble(c), 0.001);
    }

    @Test
    public void testSgnFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=SGN(7)",
            "110 B=SGN(-3)",
            "120 C=SGN(0)"
        );
        Executor executor = runIt(listing);
        assertEquals(1, toInt(executor.getSymbol("A")));
        assertEquals(-1, toInt(executor.getSymbol("B")));
        assertEquals(0, toInt(executor.getSymbol("C")));
    }

    @Test
    public void testSqrFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=SQR(16)",
            "110 B=SQR(2)"
        );
        Executor executor = runIt(listing);
        assertEquals(4.0, (Double) executor.getSymbol("A"), 0.001);
        assertEquals(Math.sqrt(2), (Double) executor.getSymbol("B"), 0.001);
    }

    @Test
    public void testTrigFunctions() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=SIN(0)",
            "110 B=COS(0)",
            "120 C=TAN(0)",
            "130 D=ATN(1)"
        );
        Executor executor = runIt(listing);
        assertEquals(0.0, (Double) executor.getSymbol("A"), 0.001);
        assertEquals(1.0, (Double) executor.getSymbol("B"), 0.001);
        assertEquals(0.0, (Double) executor.getSymbol("C"), 0.001);
        assertEquals(Math.PI/4, (Double) executor.getSymbol("D"), 0.001);
    }

    @Test
    public void testExpLogFunctions() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=EXP(1)",
            "110 B=LOG(2.718282)"
        );
        Executor executor = runIt(listing);
        assertEquals(Math.E, (Double) executor.getSymbol("A"), 0.001);
        assertEquals(1.0, (Double) executor.getSymbol("B"), 0.01);
    }

    @Test
    public void testRndFunction() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=RND(1)",
            "110 B=RND(10)"
        );
        Executor executor = runIt(listing);
        
        Double a = (Double) executor.getSymbol("A");
        Double b = (Double) executor.getSymbol("B");
        
        // RND(1) should return a value between 0 and 1
        assertTrue(a >= 0.0 && a <= 1.0, "RND(1) should be between 0 and 1");
        
        // RND(10) should return a value between 0 and 10
        assertTrue(b >= 0.0 && b <= 10.0, "RND(10) should be between 0 and 10");
    }

    @Test
    public void testFunctionInExpression() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=INT(3.7)+ABS(-2)",
            "110 B=SQR(16)*2"
        );
        Executor executor = runIt(listing);
        assertEquals(5.0, toDouble(executor.getSymbol("A"))); // 3 + 2 = 5
        assertEquals(8.0, toDouble(executor.getSymbol("B"))); // 4 * 2 = 8
    }

    @Test
    public void testNestedFunctions() throws Exception {
        List<String> listing = Arrays.asList(
            "100 A=ABS(SGN(-5))"
        );
        Executor executor = runIt(listing);
        assertEquals(1.0, toDouble(executor.getSymbol("A")), 0.001); // ABS(-1) = 1
    }

    @Test
    public void testAbsFunctionWithExpression() throws Exception {
        List<String> listing = Arrays.asList(
            "100 P=ABS(-2*6^2)"
        );
        Executor executor = runIt(listing);
        assertEquals(72.0, toDouble(executor.getSymbol("P")));
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return Double.parseDouble(obj.toString());
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(obj.toString());
    }
} 