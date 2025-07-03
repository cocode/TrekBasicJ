package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Translation of python_tests/test_basic_symbols.py for the new SymbolTable class.
 */
public class BasicSymbolsTest {

    @Test
    public void testBasic() {
        SymbolTable sym = new SymbolTable();
        sym.putSymbol("A", 3, SymbolType.VARIABLE, null);

        assertEquals(1, sym.size());
        assertEquals(3, sym.getSymbol("A"));
        assertEquals(SymbolType.VARIABLE, sym.getSymbolType("A"));
        assertTrue(sym.isSymbolDefined("A"));
        assertFalse(sym.isSymbolDefined("B"));

        sym.putSymbol("B", "ABC", SymbolType.VARIABLE, null);
        assertEquals(2, sym.size());
        assertEquals("ABC", sym.getSymbol("B"));
        assertEquals(SymbolType.VARIABLE, sym.getSymbolType("B"));
        assertNull(sym.getSymbolArg("B"));
        assertTrue(sym.isSymbolDefined("B"));

        // Ensure A still valid
        assertEquals(SymbolType.VARIABLE, sym.getSymbolType("A"));
        assertTrue(sym.isSymbolDefined("A"));
    }

    @Test
    public void testScope() {
        SymbolTable outer = new SymbolTable();
        SymbolTable inner = outer.getNestedScope();
        outer.putSymbol("A", 3, SymbolType.VARIABLE, null);
        outer.putSymbol("B", 27, SymbolType.VARIABLE, null);
        assertEquals(2, inner.size());
        assertEquals(2, outer.size());

        inner.putSymbol("A", "5", SymbolType.VARIABLE, "X");
        assertEquals(3, inner.size());
        assertEquals(2, outer.size());

        assertEquals("5", inner.getSymbol("A"));
        assertEquals(SymbolType.VARIABLE, inner.getSymbolType("A"));
        assertEquals("X", inner.getSymbolArg("A"));
        assertTrue(inner.isSymbolDefined("A"));

        // Access to outer via inner
        assertEquals(27, inner.getSymbol("B"));
        assertEquals(SymbolType.VARIABLE, inner.getSymbolType("B"));
        assertNull(inner.getSymbolArg("B"));
        assertTrue(inner.isSymbolDefined("B"));

        // Outer values unaffected
        assertEquals(3, outer.getSymbol("A"));
        assertEquals(SymbolType.VARIABLE, outer.getSymbolType("A"));
        assertNull(outer.getSymbolArg("A"));
        assertTrue(outer.isSymbolDefined("A"));
    }

    @Test
    public void testGetTable() {
        SymbolTable s = new SymbolTable();
        assertEquals(0, s._symbol_tables().size());
        s.putSymbol("a", "99", SymbolType.VARIABLE, "test");
        assertEquals(1, s._symbol_tables().size());
        s.putSymbol("b", "999", SymbolType.VARIABLE, "test");
        assertEquals(1, s._symbol_tables().size());
        s.putSymbol("b", "9999", SymbolType.ARRAY, "test");
        assertEquals(2, s._symbol_tables().size());
        s.putSymbol("b", "99999", SymbolType.FUNCTION, "test");
        assertEquals(3, s._symbol_tables().size());
    }

    @Test
    public void testTableTypes() {
        SymbolTable s = new SymbolTable();
        assertEquals(0, s.size());
        s.putSymbol("a", "99", SymbolType.VARIABLE, "test1");
        java.util.List<Integer> array = java.util.Collections.nCopies(10, 10);
        s.putSymbol("a", array, SymbolType.ARRAY, "test2");
        s.putSymbol("a", "x*x", SymbolType.FUNCTION, "test3");
        assertEquals(3, s.size());
        assertEquals("99", s.getSymbol("a", SymbolType.VARIABLE));
        assertEquals(array, s.getSymbol("a", SymbolType.ARRAY));
        assertEquals("x*x", s.getSymbol("a", SymbolType.FUNCTION));

        SymbolTable inner = s.getNestedScope();
        assertEquals(3, inner.size());
        assertEquals("99", inner.getSymbol("a", SymbolType.VARIABLE));
        assertEquals(array, inner.getSymbol("a", SymbolType.ARRAY));
        assertEquals("x*x", inner.getSymbol("a", SymbolType.FUNCTION));
    }
} 