package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class StrFunctionTest extends TestCaseBase {

    @Test
    public void testStrDollarPositive() throws Exception {
        List<String> prog = Arrays.asList(
            "100 A=42",
            "110 B$=STR$(A)",
            "120 END"
        );
        Executor ex = runIt(prog);
        assertEquals(" 42", ex.getSymbol("B$"));
    }

    @Test
    public void testStrDollarNegative() throws Exception {
        List<String> prog = Arrays.asList(
            "100 A=-7",
            "110 B$=STR$(A)",
            "120 END"
        );
        Executor ex = runIt(prog);
        assertEquals("-7", ex.getSymbol("B$"));
    }

    @Test
    public void testRightWithStr() throws Exception {
        List<String> prog = Arrays.asList(
            "100 A=1234",
            "110 B$=RIGHT$(STR$(A+1000),3)",
            "120 END"
        );
        Executor ex = runIt(prog);
        assertEquals("234", ex.getSymbol("B$"));
    }
} 