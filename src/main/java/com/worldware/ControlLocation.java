package com.worldware;

/**
 * Target of a control transfer. Used by GOTO, GOSUB, NEXT, etc.
 * index: The index into the Program._lines list
 * offset: The index into the ProgramLine.stmts list.
 */
public class ControlLocation {
    private final Integer index;
    private final int offset;

    public ControlLocation(Integer index, int offset) {
        this.index = index;
        this.offset = offset;
    }

    public Integer getIndex() {
        return index;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("ControlLocation(index=%s, offset=%d)", index, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlLocation that = (ControlLocation) o;
        return offset == that.offset && 
               java.util.Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(index, offset);
    }
} 