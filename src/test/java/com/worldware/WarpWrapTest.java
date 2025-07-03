package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WarpWrapTest extends TestCaseBase {

    @Test
    public void testSectorWrapRightEdge() throws Exception {
        List<String> prog = Arrays.asList(
                "100 S2=6:Q2=3",
                "110 FOR I=1 TO 7",
                "120 S2=S2+1",
                "130 IF S2<1 OR S2>8 THEN S2=S2-8:Q2=Q2+1",
                "140 NEXT I",
                "150 END"
        );
        Executor ex = runIt(prog);
        assertNumberEquals(5, ex.getSymbol("S2"));
        assertNumberEquals(4, ex.getSymbol("Q2"));
    }

    private static void assertNumberEquals(double expected, Object actual) {
        assertTrue(actual instanceof Number,
                "Expected numeric but got " + actual);
        assertEquals(expected, ((Number) actual).doubleValue(), 1e-9);
    }
} 