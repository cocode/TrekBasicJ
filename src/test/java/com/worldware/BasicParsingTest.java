package com.worldware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Translation of core parts of python_tests/test_basic_parsing.py that match
 * the current Java implementation.
 */
public class BasicParsingTest {

    @Test
    public void testForParsing() throws Exception {
        Statement stmt = BasicLoader.parseStatement("FOR I=1 TO 10 STEP 2");
        assertTrue(stmt instanceof ForStatement);
        ForStatement p = (ForStatement) stmt;
        assertEquals("I", p.getIndexVariable());
        assertEquals("1", p.getStartExpression());
        assertEquals("10", p.getEndExpression());
        assertEquals("2", p.getStepExpression());

        stmt = BasicLoader.parseStatement("FOR I5=100TOX(3)STEP-Y");
        p = (ForStatement) stmt;
        assertEquals("I5", p.getIndexVariable());
        assertEquals("100", p.getStartExpression());
        assertEquals("X(3)", p.getEndExpression());
        assertEquals("-Y", p.getStepExpression());
    }

    @Test
    public void testNextParsing() throws Exception {
        Statement stmt = BasicLoader.parseStatement("NEXT J");
        assertEquals("NEXT", stmt.getKeyword());
        assertEquals("J", stmt.getArgs().trim());
    }

    @Test
    public void testGotoParsing() throws Exception {
        Statement stmt = BasicLoader.parseStatement("GOTO 100");
        assertEquals("GOTO", stmt.getKeyword());
        assertEquals("100", stmt.getArgs().trim());

        stmt = BasicLoader.parseStatement("GOSUB 2137");
        assertEquals("GOSUB", stmt.getKeyword());
        assertEquals("2137", stmt.getArgs().trim());
    }
} 