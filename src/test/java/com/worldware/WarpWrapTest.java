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

    @Test
    public void testSectorWrapLeftEdge() throws Exception {
        List<String> prog = Arrays.asList(
                "100 S2=2:Q2=3",
                "110 FOR I=1 TO 3",
                "120 S2=S2-1",
                "130 IF S2<1 OR S2>8 THEN S2=S2+8:Q2=Q2-1",
                "140 NEXT I",
                "150 END"
        );
        Executor ex = runIt(prog);
        assertNumberEquals(7, ex.getSymbol("S2"));
        assertNumberEquals(2, ex.getSymbol("Q2"));
    }

    @Test
    public void testSectorWrapLeftEightSteps() throws Exception {
        // 8 steps left should wrap once and land back on starting column, quadrant -1
        List<String> prog = Arrays.asList(
                "100 S2=5:Q2=3",
                "110 FOR I=1 TO 8",
                "120 S2=S2-1",
                "130 IF S2<1 OR S2>8 THEN S2=S2+8:Q2=Q2-1",
                "140 NEXT I",
                "150 END"
        );
        Executor ex = runIt(prog);
        assertNumberEquals(5, ex.getSymbol("S2"));
        assertNumberEquals(2, ex.getSymbol("Q2"));
    }

    @Test
    public void testSectorWrapDownEightSteps() throws Exception {
        // vertical downward (increase row) course 5?? Actually down is S1+1
        List<String> prog = Arrays.asList(
                "100 S1=6:Q1=3",
                "110 FOR I=1 TO 8",
                "120 S1=S1+1",
                "130 IF S1<1 OR S1>8 THEN S1=S1-8:Q1=Q1+1",
                "140 NEXT I",
                "150 END"
        );
        Executor ex = runIt(prog);
        assertNumberEquals(6, ex.getSymbol("S1"));
        assertNumberEquals(4, ex.getSymbol("Q1"));
    }

    @org.junit.jupiter.api.Disabled("Pattern causes infinite loop with restart; not needed for wrap correctness")
    @Test
    public void testSectorWrapRightWithGoto() throws Exception {
        List<String> prog = Arrays.asList(
                "100 S2=5:Q2=3",
                "110 FOR D=1 TO 8",
                "120 S2=S2+1",
                "130 IF S2<1 OR S2>8 THEN S2=S2-8:Q2=Q2+1:GOTO 110", // wrap then restart same iteration? emulate trek pattern
                "140 NEXT D",
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