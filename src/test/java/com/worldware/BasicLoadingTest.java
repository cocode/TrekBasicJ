package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicLoadingTest {

    @Test
    public void testTokenizeRem() throws Exception {
        String line = "10 REM SUPER STARTREK - MAY 16,1978 - REQUIRES 24K MEMORY";
        ProgramLine result = BasicLoader.tokenizeLine(line);
        assertNotNull(result);
        assertEquals(10, result.getLine());
        assertEquals(1, result.getStmts().size());
        
        Statement stmt = result.getStmts().get(0);
        assertEquals("REM", stmt.getKeyword());
        assertEquals("SUPER STARTREK - MAY 16,1978 - REQUIRES 24K MEMORY", stmt.getArgs());
    }

    @Test
    public void testTokenizeFor() throws Exception {
        String line = "820 FORI=1TO8";
        ProgramLine result = BasicLoader.tokenizeLine(line);
        assertNotNull(result);
        assertEquals(820, result.getLine());
        assertEquals(1, result.getStmts().size());
        
        Statement stmt = result.getStmts().get(0);
        assertEquals("FOR", stmt.getKeyword());
        assertTrue(stmt instanceof ForStatement);
        
        ForStatement forStmt = (ForStatement) stmt;
        assertEquals("I", forStmt.getIndexVariable());
        assertEquals("1", forStmt.getStartExpression());
        assertEquals("8", forStmt.getEndExpression());
        assertEquals("1", forStmt.getStepExpression());
    }

    @Test
    public void testTokenizeAssignment() throws Exception {
        String line = "370 T=25";
        ProgramLine result = BasicLoader.tokenizeLine(line);
        assertNotNull(result);
        assertEquals(370, result.getLine());
        assertEquals(1, result.getStmts().size());
        
        Statement stmt = result.getStmts().get(0);
        assertEquals("LET", stmt.getKeyword());
        assertTrue(stmt instanceof AssignmentStatement);
        
        AssignmentStatement assignment = (AssignmentStatement) stmt;
        assertEquals("T", assignment.getVariable());
        assertEquals("25", assignment.getExpression());
    }

    @Test
    public void testMultipleStatements() throws Exception {
        String line = "100 A=5:B=6:C=7";
        ProgramLine result = BasicLoader.tokenizeLine(line);
        assertNotNull(result);
        assertEquals(100, result.getLine());
        assertEquals(3, result.getStmts().size());
        
        // Check each statement
        for (int i = 0; i < 3; i++) {
            Statement stmt = result.getStmts().get(i);
            assertEquals("LET", stmt.getKeyword());
            assertTrue(stmt instanceof AssignmentStatement);
        }
        
        AssignmentStatement stmt1 = (AssignmentStatement) result.getStmts().get(0);
        assertEquals("A", stmt1.getVariable());
        assertEquals("5", stmt1.getExpression());
        
        AssignmentStatement stmt2 = (AssignmentStatement) result.getStmts().get(1);
        assertEquals("B", stmt2.getVariable());
        assertEquals("6", stmt2.getExpression());
    }

    @Test
    public void testTokenizeProgram() throws Exception {
        List<String> lines = Arrays.asList(
            "10 PRINT \"Hello\"",
            "20 A=5",
            "30 END"
        );
        
        Program program = BasicLoader.tokenize(lines);
        assertEquals(3, program.size());
        
        // Check first line
        ProgramLine line1 = program.getLine(0);
        assertEquals(10, line1.getLine());
        assertEquals("PRINT", line1.getStmts().get(0).getKeyword());
        
        // Check second line
        ProgramLine line2 = program.getLine(1);
        assertEquals(20, line2.getLine());
        assertEquals("LET", line2.getStmts().get(0).getKeyword());
        
        // Check third line
        ProgramLine line3 = program.getLine(2);
        assertEquals(30, line3.getLine());
        assertEquals("END", line3.getStmts().get(0).getKeyword());
    }

    @Test
    public void testCaseInsensitiveKeywords() throws Exception {
        String line = "370 if a=1 then print \"test\"";
        ProgramLine result = BasicLoader.tokenizeLine(line);
        assertNotNull(result);
        assertEquals(370, result.getLine());
        assertTrue(result.getStmts().size() >= 1);
        
        Statement stmt = result.getStmts().get(0);
        assertEquals("IF", stmt.getKeyword()); // Should be converted to uppercase
    }
} 