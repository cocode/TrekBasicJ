package com.worldware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Ensures FOR records are reset if execution re-enters a FOR line after a GOTO
 */
public class ForLoopGotoExitTest extends TestCaseBase {

    @Test
    public void testGotoOutAndBackIntoLoop() throws Exception {
        List<String> prog = Arrays.asList(
            "100 FOR I=1 TO 2",
            "110 GOTO 150",
            "120 NEXT I",
            "130 END",   // should never reach
            "150 PRINT \"BACK\";",
            "160 IF I<2 THEN GOTO 120",  // resume loop to execute NEXT
            "170 NEXT I",
            "180 END"
        );
        Executor ex = runIt(prog);
        // Program completes with I equal to 2
        assertEquals(2.0, ex.getSymbol("I"));
    }
} 