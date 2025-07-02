package com.worldware;

/**
 * Records the ending status of the program.
 */
public enum RunStatus {
    RUN,                    // Running normally
    END_CMD,                // Hit an END statement. Returns status 0 (success)
    END_STOP,               // Hit a STOP statement. Returns status 1 (failed)
    END_OF_PROGRAM,         // Fell off the end of the program. Returns status 0 (success)
    END_ERROR_SYNTAX,
    END_ERROR_INTERNAL,
    END_ERROR_RUNTIME,
    BREAK_CODE,
    BREAK_DATA,
    BREAK_STEP
} 