package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for small utility helpers that parallel python_tests/test_basic_utils.py.
 * Only the parts already implemented in the Java port are exercised – advanced
 * IF / THEN / ELSE aware splitting will be added later.
 */
public class BasicUtilsTest {

    /**
     * Basic test for BasicLoader.smartSplit(): should split on ':' that are not
     * inside string literals.  Mirrors python test_smart_split().
     */
    @Test
    public void testSmartSplitSimplePrints() {
        String line = "PRINT\"YOUR MISSION: BEGINS\":PRINT\"AND ENDS\"";
        List<String> parts = BasicLoader.smartSplit(line, ':');
        assertEquals(2, parts.size());
        assertEquals("PRINT\"YOUR MISSION: BEGINS\"", parts.get(0));
        assertEquals("PRINT\"AND ENDS\"", parts.get(1));
    }

    /**
     * Confirm that IF…THEN without ELSE still splits at top-level ':' after the
     * THEN-part (Executor and BasicLoader rely on this behaviour).
     */
    @Test
    public void testSmartSplitIfThenNoElse() {
        String line = "IF A>0 THEN PRINT \"Positive\": B=1";
        List<String> parts = BasicLoader.smartSplit(line, ':');
        assertEquals(2, parts.size());
        assertEquals("IF A>0 THEN PRINT \"Positive\"", parts.get(0));
        // Note leading space preserved (matches original python expectation)
        assertEquals(" B=1", parts.get(1));
    }
} 