package com.worldware;

import org.junit.jupiter.api.Test;
import java.util.*;

/** Tests specific FOR/NEXT loop behaviours */
public class ForLoopTest extends TestCaseBase {

    /**
     * BASIC loops include the TO value. This test ensures a loop whose start
     * value already equals the limit executes exactly once.
     */
    @Test
    public void testInclusiveUpperBound() throws Exception {
        List<String> src = Arrays.asList(
            "100 A=0",
            "110 FOR I=1 TO 1",
            "120 A=A+1",
            "130 NEXT I"
        );
        Executor ex = runIt(src);
        assertValue(ex, "A", 1);
    }
} 