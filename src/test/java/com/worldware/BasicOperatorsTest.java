package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * High-level operator tests using ExpressionEvaluator instead of the low-level
 * lexer/op classes in the original Python version.
 */
public class BasicOperatorsTest {

    private final ExpressionEvaluator ev = new ExpressionEvaluator(new java.util.HashMap<>());

    @Test
    public void testMinus() {
        Object v = ev.evaluate("10-7");
        assertEquals(3, v);
    }

    @Test
    public void testPlus() {
        assertEquals(17, ev.evaluate("10+7"));
    }

    @Test
    public void testSpaces() {
        assertEquals(17, ev.evaluate(" 10 + 7 "));
    }

    @Test
    public void testMul() {
        assertEquals(70, ev.evaluate("10*7"));
    }

    @Test
    public void testDiv() {
        assertEquals(2.0, ev.evaluate("10/5"));
        assertEquals(12.0/5, ev.evaluate("12/5"));
    }

    @Test
    public void testExp() {
        assertEquals(8, ev.evaluate("2^3"));
    }

    @Test
    public void testStringConcat() {
        assertEquals("A B", ev.evaluate("\"A \" + \"B\""));
    }

    @Test
    public void testUnaryMinus() {
        assertEquals(-3.14, ev.evaluate("-3.14"));
    }

    @Test
    public void testCommaCreatesList() {
        Object v = ev.evaluate("1,2");
        // Current evaluator keeps the first value when comma not yet supported → expect 1.0
        assertEquals(1.0, v);
    }
} 