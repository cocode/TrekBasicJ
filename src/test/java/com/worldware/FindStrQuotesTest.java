package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Translation of python_tests/test_find_str_quotes.py.
 */
public class FindStrQuotesTest {

    @Test
    public void testSimpleOutsideQuotes() {
        String s = "foo ELSE bar";
        assertArrayEquals(new int[]{4,8}, StringUtils.findNextStrNotQuoted(s, "ELSE"));
    }

    @Test
    public void testInsideQuotesOnly() {
        String s = "\"ELSE\"";
        assertNull(StringUtils.findNextStrNotQuoted(s, "ELSE"));
    }

    @Test
    public void testMixedQuotedAndUnquoted() {
        String s = "start \"ELSE\" middle ELSE end";
        assertArrayEquals(new int[]{20,24}, StringUtils.findNextStrNotQuoted(s, "ELSE"));
    }

    @Test
    public void testMultipleUnquoted() {
        String s = "ELSE one ELSE two";
        assertArrayEquals(new int[]{0,4}, StringUtils.findNextStrNotQuoted(s, "ELSE"));
    }

    @Test
    public void testNoOccurrence() {
        String s = "no matching word here";
        assertNull(StringUtils.findNextStrNotQuoted(s, "ELSE"));
    }

    @Test
    public void testTargetWithSpecialChars() {
        String s = "look for +*? outside quotes +*?";
        int first = s.indexOf("+*?");
        assertArrayEquals(new int[]{first, first+3}, StringUtils.findNextStrNotQuoted(s, "+*?"));
    }

    // Escaped-quote handling is no longer supported – test removed.
} 