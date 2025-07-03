package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InputStatementTest {

    @Test
    public void testSimpleInput() throws Exception {
        InputStatement stmt = new InputStatement("INPUT", "A");
        
        assertFalse(stmt.hasPrompt());
        assertEquals(1, stmt.getVariables().size());
        assertEquals("A", stmt.getVariables().get(0));
    }

    @Test
    public void testMultipleVariables() throws Exception {
        InputStatement stmt = new InputStatement("INPUT", "A,B,C$");
        
        assertFalse(stmt.hasPrompt());
        assertEquals(3, stmt.getVariables().size());
        assertEquals("A", stmt.getVariables().get(0));
        assertEquals("B", stmt.getVariables().get(1));
        assertEquals("C$", stmt.getVariables().get(2));
    }

    @Test
    public void testInputWithPrompt() throws Exception {
        InputStatement stmt = new InputStatement("INPUT", "\"Enter value\"; A");
        
        assertTrue(stmt.hasPrompt());
        assertEquals("Enter value", stmt.getPrompt());
        assertEquals(1, stmt.getVariables().size());
        assertEquals("A", stmt.getVariables().get(0));
    }

    @Test
    public void testInputWithPromptAndComma() throws Exception {
        InputStatement stmt = new InputStatement("INPUT", "\"Name: \", N$");
        
        assertTrue(stmt.hasPrompt());
        assertEquals("Name: ", stmt.getPrompt());
        assertEquals(1, stmt.getVariables().size());
        assertEquals("N$", stmt.getVariables().get(0));
    }

    @Test
    public void testInputWithPromptMultipleVars() throws Exception {
        InputStatement stmt = new InputStatement("INPUT", "\"Enter X,Y: \"; X,Y");
        
        assertTrue(stmt.hasPrompt());
        assertEquals("Enter X,Y: ", stmt.getPrompt());
        assertEquals(2, stmt.getVariables().size());
        assertEquals("X", stmt.getVariables().get(0));
        assertEquals("Y", stmt.getVariables().get(1));
    }

    @Test
    public void testInvalidInputNoVariables() {
        assertThrows(BasicSyntaxError.class, () -> {
            new InputStatement("INPUT", "");
        });
    }

    @Test
    public void testInvalidInputPromptOnly() {
        assertThrows(BasicSyntaxError.class, () -> {
            new InputStatement("INPUT", "\"Enter value\"");
        });
    }

    @Test
    public void testInvalidVariableName() {
        assertThrows(BasicSyntaxError.class, () -> {
            new InputStatement("INPUT", "123ABC");
        });
    }
} 