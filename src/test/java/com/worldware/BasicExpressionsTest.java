package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Core expression-evaluation tests reflecting python_tests/test_basic_expressions.py
 * for the functionality currently implemented by ExpressionEvaluator.
 */
public class BasicExpressionsTest {

    private final ExpressionEvaluator ev = new ExpressionEvaluator(new HashMap<>());

    private static void assertNumberEquals(double expected, Object actual) {
        assertTrue(actual instanceof Number,
                "Expected a numeric result but got " + actual);
        assertEquals(expected, ((Number) actual).doubleValue(), 1e-9);
    }

    @Test
    public void testNumericLiteral() {
        assertNumberEquals(1, ev.evaluate("1"));
    }

    @Test
    public void testStringLiteral() {
        assertEquals("ABC", ev.evaluate("\"ABC\""));
    }

    @Test
    public void testSimpleArithmetic() {
        assertNumberEquals(-2.0, ev.evaluate("1-3"));
        assertNumberEquals(-5.0, ev.evaluate("1-3*2"));
        assertNumberEquals(-5.0, ev.evaluate(" 1 - 3 * 2 "));
    }

    @Test
    public void testPrecedencePower() {
        assertNumberEquals(160, ev.evaluate("1 + 2 * 3 ^ 4 - 3"));
        assertNumberEquals(64,  ev.evaluate("2^3^2"));
    }

    @Test
    public void testParentheses() {
        assertNumberEquals(4,  ev.evaluate("(7-3)"));
        assertNumberEquals(8,  ev.evaluate("(7-3) * 2"));
        assertNumberEquals(9,  ev.evaluate("1 + (7-3) * 2"));
        assertNumberEquals(25, ev.evaluate("(7-3) * (2*4) - (2+5)"));
        assertNumberEquals(4,  ev.evaluate("(7-3) * ((2*4) - (2+5))"));
    }

    @Test
    public void testUnaryMinus() {
        assertNumberEquals(-9.0, ev.evaluate("-9"));
        assertNumberEquals(-27.0, ev.evaluate("-9*3"));
        assertNumberEquals(-80.0, ev.evaluate("10*-8"));
        assertNumberEquals(-11.0, ev.evaluate("-(8+3)"));
        assertNumberEquals(44.0, ev.evaluate("-(2+3)*-(4+5)+-1"));
    }
} 