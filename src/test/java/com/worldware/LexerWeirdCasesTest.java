package com.worldware;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the BASIC loader correctly tokenises statements where keywords
 * and identifiers are run together without spaces – a common quirk of
 * classic BASIC dialects.
 */
public class LexerWeirdCasesTest extends TestCaseBase {

    @Test
    public void testIfWithoutSpaces() throws Exception {
        ProgramLine line = BasicLoader.tokenizeLine("10 IFX=100THENC=A");
        List<Statement> stmts = line.getStmts();
        assertEquals(1, stmts.size(), "Should parse exactly one statement");
        Statement stmt = stmts.get(0);
        assertTrue(stmt instanceof IfThenStatement, "Statement should be IfThenStatement");

        IfThenStatement ifStmt = (IfThenStatement) stmt;
        assertEquals("X=100", ifStmt.getCondition());
        assertEquals("C=A", ifStmt.getThenStatements());
    }

    @Test
    public void testPrintWithoutSpaceBeforeString() throws Exception {
        ProgramLine line = BasicLoader.tokenizeLine("20 PRINT\"HELLO\"");
        List<Statement> stmts = line.getStmts();
        assertEquals(1, stmts.size());
        Statement stmt = stmts.get(0);
        assertEquals("PRINT", stmt.getKeyword());
        assertEquals("\"HELLO\"", stmt.getArgs());
    }

    @Test
    public void testNextWithoutSpace() throws Exception {
        ProgramLine line = BasicLoader.tokenizeLine("30 NEXTI");
        List<Statement> stmts = line.getStmts();
        assertEquals(1, stmts.size());
        Statement stmt = stmts.get(0);
        assertEquals("NEXT", stmt.getKeyword());
        assertEquals("I", stmt.getArgs());
    }
} 