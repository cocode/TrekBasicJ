package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Reproduces array-index overflow seen in SuperStarTrek long-range sensor loop
 * where FOR I=Q1-1 TO Q1+1 iterates beyond sector bounds.
 */
public class ForLoopBoundsTest extends TestCaseBase {

    @Test
    public void testForLoopClamped() throws Exception {
        /* Program emulates offending loop:
           FOR I=8 TO 9
             IF I>0 AND I<9 THEN PRINT I
           NEXT I
        */
        List<String> prog = Arrays.asList(
            "100 Q1=8:X=0",
            "110 FOR I=Q1 TO Q1+1",
            "120 IF I>0 AND I<9 THEN X=X+1",
            "130 NEXT I",
            "140 END"
        );
        Executor ex = runIt(prog);
        // Program should complete; final I should be 9 (clamped)
        assertEquals(9.0, ex.getSymbol("I"));
    }
} 