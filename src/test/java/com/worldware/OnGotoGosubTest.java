package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class OnGotoGosubTest extends TestCaseBase {

    @Test
    public void testOnGoto() throws Exception {
        List<String> src = Arrays.asList(
            "100 ON 2 GOTO 200,300,400",
            "110 A=999 : END",
            "200 A=1 : END",
            "300 A=2 : END",
            "400 A=3 : END"
        );
        Executor ex = runIt(src);
        assertValue(ex, "A", 2);
    }

    @Test
    public void testOnGosub() throws Exception {
        List<String> src = Arrays.asList(
            "100 ON 1 GOSUB 200,300",
            "110 A=5 : END",
            "200 B=2 : RETURN",
            "300 B=3 : RETURN"
        );
        Executor ex = runIt(src);
        assertValue(ex, "B", 2);
        assertValue(ex, "A", 5);
    }

    @Test
    public void testForStepZeroError() {
        List<String> src = Arrays.asList("100 FOR I=1 TO 10 STEP 0", "110 NEXT I");
        assertThrows(BasicRuntimeError.class, () -> runIt(src));
    }

    @Test
    public void testForNoExecutionWhenOutOfRange() throws Exception {
        List<String> src = Arrays.asList(
            "100 FOR I=10 TO 1 STEP 1", // loop should be skipped
            "110 A=42",
            "120 NEXT I"
        );
        Executor ex = runIt(src);
        assertValue(ex, "A", 42);
    }
} 