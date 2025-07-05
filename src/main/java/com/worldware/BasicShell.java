package com.worldware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Minimal Java port of Python {@code basic_shell.py} providing core utilities
 * (loading programs, renumbering, formatting) that are needed by unit tests.
 * <p>
 * The full interactive command-line shell present in the Python version is
 * extensive.  For the purposes of the TrekBasicJ translation we currently port
 * the subset that external code and tests require:
 * <ul>
 *   <li>Constructors that optionally load a program file</li>
 *   <li>{@link #loadFromString(List)}</li>
 *   <li>{@link #loadProgram(Program)}</li>
 *   <li>{@link #loadFromFile(boolean)}</li>
 *   <li>{@link #buildLineMap(Program, int, int)}</li>
 *   <li>{@link #renumber(Program, Map, int, int)}</li>
 *   <li>{@link #format(Program)}</li>
 *   <li>a stub {@link #cmdSymbols(String)} so existing tests can call it</li>
 * </ul>
 * <p>
 * The implementation purposefully avoids any user-interaction code (readline,
 * history files, etc.) until it becomes necessary.  The focus is on program
 * transformation helpers which are needed by the automated tests.
 */
public class BasicShell {

    /** The BASIC source file supplied on the command line (may be {@code null}). */
    private String programFile;

    /** Current executor – created once a program has been loaded. */
    private Executor executor;

    /** Indicates whether the last load operation succeeded.  Named after the
     *  Python field so tests can access it directly. */
    public boolean load_status;

    // Minimal stubs for break- and watch-points (not yet implemented)
    private final List<int[]> breakpoints = new ArrayList<>();
    private final List<String> dataBreakpoints = new ArrayList<>();

    // ---------------------------------------------------------------------
    // Construction / loading helpers
    // ---------------------------------------------------------------------
    public BasicShell() {
        this(null);
    }

    public BasicShell(String programFile) {
        this.programFile = programFile;
        this.executor = null;
        this.load_status = false;
        if (programFile != null) {
            loadFromFile(false);
        }
    }

    /**
     * Build an executor from an in-memory listing (used by unit tests).
     */
    public void loadFromString(List<String> listing) {
        try {
            Program program = BasicLoader.tokenize(listing);
            this.executor = new Executor(program);
            this.load_status = true;
        } catch (BasicSyntaxError | IOException e) {
            System.err.println("Failed to load program from string: " + e.getMessage());
            this.load_status = false;
        }
    }

    /**
     * Replace the currently loaded program with the supplied one.
     */
    public void loadProgram(Program program) {
        try {
            this.executor = new Executor(program);
            this.load_status = true;
        } catch (IOException e) {
            System.err.println("Failed to create executor: " + e.getMessage());
            this.load_status = false;
        }
    }

    /**
     * Load a BASIC file from disk.
     *
     * @param coverage currently ignored – present for API compatibility with
     *                 the Python version.
     */
    public void loadFromFile(boolean coverage) {
        if (programFile == null) {
            System.err.println("No program file specified");
            this.load_status = false;
            return;
        }

        // Accept file names without .bas suffix (mimic Python behaviour)
        Path path = Paths.get(programFile);
        if (!Files.exists(path)) {
            path = Paths.get(programFile + ".bas");
        }
        if (!Files.exists(path)) {
            System.err.println("File not found: " + programFile);
            this.load_status = false;
            return;
        }

        System.out.println("Loading " + path);
        try {
            List<String> lines = Files.readAllLines(path);
            Program program = BasicLoader.tokenize(lines);
            this.executor = new Executor(program);
            this.load_status = true;
        } catch (BasicSyntaxError e) {
            System.err.printf("Syntax error: %s in line %d%n", e.getMessage(), e.getLineNumber());
            this.load_status = false;
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            this.load_status = false;
        }
    }

    // ---------------------------------------------------------------------
    // Programme transformation helpers (renumber / format)
    // ---------------------------------------------------------------------

    /**
     * A tiny record that mirrors the tuple returned by the Python version.
     */
    public static class LineMapResult {
        public final Map<Integer, Integer> lineMap;
        public final int statementCount;
        public LineMapResult(Map<Integer, Integer> lineMap, int statementCount) {
            this.lineMap = lineMap;
            this.statementCount = statementCount;
        }
    }

    /**
     * Build a mapping from old to new line numbers and count total statements.
     */
    public LineMapResult buildLineMap(Program oldProgram, int startLine, int increment) {
        Map<Integer, Integer> lineMap = new HashMap<>();
        int statementCount = 0;
        int curLine = startLine;

        for (ProgramLine line : oldProgram) {
            lineMap.put(line.getLine(), curLine);
            curLine += increment;
            statementCount += line.getStmts().size();
        }
        return new LineMapResult(lineMap, statementCount);
    }

    /**
     * Renumber a program (lines and internal references).
     */
    public Program renumber(Program oldProgram,
                            Map<Integer, Integer> lineMap,
                            int startLine,
                            int increment) {
        List<ProgramLine> newProgramLines = new ArrayList<>();
        int curLine = startLine;

        for (ProgramLine oldLine : oldProgram) {
            List<Statement> newStatements = new ArrayList<>();
            for (Statement stmt : oldLine.getStmts()) {
                Statement rewritten = renumberStatement(stmt, lineMap);
                newStatements.add(rewritten);
            }
            String joined = joinStatements(newStatements);
            ProgramLine pl = new ProgramLine(curLine, newStatements, curLine + " " + joined);
            newProgramLines.add(pl);
            curLine += increment;
        }
        return new Program(newProgramLines);
    }

    /**
     * Reformat a program without changing line numbers (normalises spacing,
     * keywords, etc.).
     */
    public Program format(Program oldProgram) {
        List<ProgramLine> newLines = new ArrayList<>();
        for (ProgramLine line : oldProgram) {
            List<Statement> newStatements = new ArrayList<>();
            for (Statement stmt : line.getStmts()) {
                // Reparse statement text so that spacing/keywords are standardised
                try {
                    Statement parsed = BasicLoader.parseStatement(stmt.toString());
                    newStatements.add(parsed);
                } catch (BasicSyntaxError e) {
                    // Fallback – keep original statement if parsing fails
                    newStatements.add(stmt);
                }
            }
            String joined = joinStatements(newStatements);
            ProgramLine pl = new ProgramLine(line.getLine(), newStatements, line.getLine() + " " + joined);
            newLines.add(pl);
        }
        return new Program(newLines);
    }

    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

    /**
     * Renumber line number references inside a single statement if the keyword
     * supports it (GOTO, GOSUB, etc.).  Only the functionality required by the
     * current automated tests is implemented – this can be expanded later as
     * needed.
     */
    private Statement renumberStatement(Statement stmt, Map<Integer, Integer> lineMap) {
        String keyword = stmt.getKeyword().toUpperCase(Locale.ROOT);
        String args = stmt.getArgs();

        // Only handle very common cases for now.
        if (keyword.equals("GOTO") || keyword.equals("GOSUB")) {
            String newArgs = renumberLineList(args, lineMap, false);
            return createStatement(keyword, newArgs);
        }
        // Other keywords left unchanged
        return stmt;
    }

    /**
     * Renumber a list of comma-separated line numbers (used by ON..GOTO, etc.).
     */
    private String renumberLineList(String argText,
                                    Map<Integer, Integer> lineMap,
                                    boolean allowExpressions) {
        StringBuilder sb = new StringBuilder();
        String[] parts = argText.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.matches("\\d+")) {
                int oldLine = Integer.parseInt(part);
                int newLine = lineMap.getOrDefault(oldLine, oldLine);
                sb.append(newLine);
            } else {
                // If it's not a pure number we leave it alone unless expressions
                // are allowed (future work)
                sb.append(part);
            }
            if (i < parts.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /** Utility to join statements back together using ':' separators. */
    private String joinStatements(List<Statement> stmts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Statement st : stmts) {
            if (!first) sb.append(":");
            sb.append(st.toString());
            first = false;
        }
        return sb.toString();
    }

    /** Create a new Statement instance from its textual representation. */
    private Statement createStatement(String keyword, String args) {
        try {
            return BasicLoader.parseStatement(keyword + (args.isEmpty() ? "" : " " + args));
        } catch (BasicSyntaxError e) {
            // Should never happen because we started from a valid statement.
            return new BasicStatement(keyword, args);
        }
    }

    // ---------------------------------------------------------------------
    // Simple stub for the symbols command so existing tests compile.
    // ---------------------------------------------------------------------
    public void cmdSymbols(String args) {
        if (executor == null) {
            System.out.println("No program has been loaded yet.");
            return;
        }
        if (args == null || args.isBlank()) {
            System.out.println("Symbol count: " + executor.getSymbolCount());
        } else {
            String name = args.trim();
            Object value = executor.getSymbol(name);
            if (value == null) {
                System.out.printf("Symbol %s is not defined%n", name);
            } else {
                System.out.printf("%s = %s%n", name, value);
            }
        }
    }

    /** Expose the underlying Executor (read-only) for simple CLI use. */
    public Executor getExecutor() {
        return executor;
    }

    // ---------------------------------------------------------------------
    // Interactive command loop (very small subset of Python shell)
    // ---------------------------------------------------------------------
    private void commandLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break; // EOF
                line = line.trim();
                if (line.isEmpty()) continue;

                // Quick BASIC line detection (starts with digits)
                if (Character.isDigit(line.charAt(0))) {
                    System.out.println("Editing of BASIC lines not yet supported in Java shell.");
                    continue;
                }

                String cmd;
                String args = null;
                int space = line.indexOf(' ');
                if (space == -1) {
                    cmd = line;
                } else {
                    cmd = line.substring(0, space);
                    args = line.substring(space + 1).trim();
                }

                switch (cmd.toLowerCase(Locale.ROOT)) {
                    case "quit", "exit" -> {
                        return; // leave loop
                    }
                    case "help" -> printHelp();
                    case "run", "r" -> runProgramCommand();
                    case "load" -> {
                        if (args == null || args.isBlank()) {
                            System.out.println("Usage: load <file>");
                        } else {
                            this.programFile = args;
                            loadFromFile(false);
                        }
                    }
                    case "symbols", "sym" -> cmdSymbols(args);
                    default -> System.out.println("Unknown command. Type 'help' for list.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in command loop: " + e.getMessage());
        }
    }

    private void runProgramCommand() {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        try {
            RunStatus status = executor.runProgram();
            System.out.println("Program completed with status " + status);
        } catch (BasicSyntaxError | BasicRuntimeError e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("Commands: run | load <file> | symbols [name] | quit | help");
    }

    // ---------------------------------------------------------------------
    // Simple command-line entry point (optional)
    // ---------------------------------------------------------------------
    public static void main(String[] args) {
        BasicShell shell;
        if (args.length > 0) {
            shell = new BasicShell(args[0]);
        } else {
            shell = new BasicShell();
        }

        shell.commandLoop();
        System.out.println("TrekBasic shell terminated.");
    }
} 
