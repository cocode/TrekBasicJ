package com.worldware;

/**
 * Basic implementation of a BASIC statement
 */
public class BasicStatement implements Statement {
    private final String keyword;
    private final String args;

    public BasicStatement(String keyword, String args) {
        this.keyword = keyword;
        this.args = args != null ? args.trim() : "";
    }

    @Override
    public String getKeyword() {
        return keyword;
    }

    @Override
    public String getArgs() {
        return args;
    }

    @Override
    public String toString() {
        if (args.isEmpty()) {
            return keyword;
        } else {
            return keyword + " " + args;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicStatement that = (BasicStatement) o;
        return java.util.Objects.equals(keyword, that.keyword) &&
               java.util.Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(keyword, args);
    }
} 