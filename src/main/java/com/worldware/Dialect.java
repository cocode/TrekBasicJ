package com.worldware;

public class Dialect {
    // 1 = uppercase user input, 0 = preserve case
    public static final int UPPERCASE_INPUT = 1;

    /**
     * Offset applied to BASIC array subscripts.  In traditional Microsoft-style
     * BASIC the first element is index 1, so ARRAY_OFFSET is 1.
     * If you ever need zero-based arrays set this to 0.
     */
    public static final int ARRAY_OFFSET = 1;
} 