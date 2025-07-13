package com.worldware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

                // Allow ?expr without space by inserting space
                if (line.startsWith("?") && line.length()>1 && line.charAt(1)!=' ') {
                    line = "? " + line.substring(1);
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

                String cmdRaw = cmd.toLowerCase(Locale.ROOT);
                // Handle shortest unique prefix matching
                String resolvedCmd = resolveCommand(cmdRaw);
                if (resolvedCmd == null) {
                    System.out.println("Unknown or ambiguous command. Type 'help' for list.");
                    continue;
                }
                cmd = resolvedCmd;

                switch (cmd) {
                    case "quit", "exit" -> {
                        return; // leave loop
                    }
                    case "help" -> {
                        if (args == null || args.isBlank()) printHelp(); else printHelp(args);
                    }
                    case "run", "r" -> cmdRun(args);
                    case "load" -> {
                        if (args == null || args.isBlank()) {
                            System.out.println("Usage: load <file>");
                        } else {
                            this.programFile = args;
                            loadFromFile(false);
                        }
                    }
                    case "symbols", "sym" -> cmdSymbols(args);
                    case "save" -> cmdSave(args);
                    case "clear" -> cmdClear();
                    case "format" -> cmdFormat();
                    case "renum" -> cmdRenum(args);
                    case "llvm" -> cmdLLVM(args);
                    case "benchmark" -> cmdBenchmark();
                    case "stop" -> cmdStop();
                    case "list" -> cmdList(args);
                    case "stmts" -> cmdStmts(args);
                    case "fors", "forstack" -> cmdForStack();
                    case "gosubs" -> cmdGosubStack();
                    case "break" -> cmdBreak(args);
                    case "continue", "c" -> cmdContinue(false);
                    case "next", "n" -> cmdContinue(true);
                    case "?" -> cmdPrint(args);
                    case "coverage" -> cmdCoverage(args);
                    default -> System.out.println("Unknown command. Type 'help' for list.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in command loop: " + e.getMessage());
        }
    }

    private void cmdRun(String args) {
        if (executor == null) { System.out.println("No program loaded."); return; }
        boolean coverage = args != null && args.trim().equalsIgnoreCase("coverage");
        Program prog = executor.getProgram();
        try {
            executor = new Executor(prog, coverage);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        cmdContinue(false);
    }

    private void cmdCoverage(String args) {
        if (executor == null) { System.out.println("No program loaded."); return; }
        Map<Integer, Set<Integer>> cov = executor.getCoverage();
        if (cov == null || cov.isEmpty()) {
            System.out.println("No coverage data recorded.");
            return;
        }
        if (args != null && args.trim().equals("lines")) {
            for (ProgramLine line : executor.getProgram()) {
                if (!cov.containsKey(line.getLine())) {
                    System.out.println(line.getLine() + " not executed");
                }
            }
            return;
        }
        // summary
        int totalLines = executor.getProgram().size();
        int executedLines = cov.size();
        System.out.printf("Lines executed: %d/%d (%.1f%%)\n", executedLines, totalLines, 100.0*executedLines/totalLines);
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
        System.out.println("Commands: run | load | save | clear | format | renum | list | stmts | fors | gosubs | llvm | benchmark | stop | symbols | quit | help");
        System.out.println("Type 'help <cmd>' for details.");
    }

    private static void printUsageSave() { System.out.println("Usage: save <file>"); }
    private static void printUsageRenum() { System.out.println("Usage: renum [start [increment]] (defaults 100 10)"); }

    // ---------------------------------------------------------------------
    // Command implementations (phase-1 features)
    // ---------------------------------------------------------------------
    private void cmdSave(String args) {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        if (args == null || args.isBlank()) {
            printUsageSave();
            return;
        }
        String filename = args.trim();
        if (!filename.endsWith(".bas")) filename += ".bas";
        Path p = Paths.get(filename);
        if (Files.exists(p)) {
            System.out.println("File exists – will not overwrite: " + filename);
            return;
        }
        try (FileWriter fw = new FileWriter(filename)) {
            for (ProgramLine pl : executor.getProgram()) {
                fw.write(pl.getSource());
                fw.write(System.lineSeparator());
            }
            System.out.println("Program saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving: " + e.getMessage());
        }
    }

    private void cmdClear() {
        executor = null;
        programFile = null;
        load_status = false;
        breakpoints.clear();
        dataBreakpoints.clear();
        System.out.println("Program and state cleared.");
    }

    private void cmdFormat() {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        Program oldProg = executor.getProgram();
        Program newProg = format(oldProg);
        loadProgram(newProg);
        System.out.println("Program formatted.");
    }

    private void cmdRenum(String args) {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        int start = 100;
        int inc = 10;
        if (args != null && !args.isBlank()) {
            String[] parts = args.trim().split(" ");
            try {
                if (parts.length >= 1) start = Integer.parseInt(parts[0]);
                if (parts.length >= 2) inc = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                printUsageRenum();
                return;
            }
        }
        Program oldProg = executor.getProgram();
        LineMapResult res = buildLineMap(oldProg, start, inc);
        Program newProg = renumber(oldProg, res.lineMap, start, inc);
        loadProgram(newProg);
        System.out.printf("Renumbered %d statements into %d lines starting at %d inc %d%n",
                res.statementCount, newProg.size(), start, inc);
    }

    private void cmdLLVM(String args) {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        String ir = new com.worldware.llvm.LLVMGenerator(executor.getProgram()).generate();
        if (args == null || args.isBlank()) {
            System.out.println(ir);
        } else {
            String file = args.trim();
            try {
                Files.writeString(Paths.get(file), ir, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("LLVM IR written to " + file);
            } catch (IOException e) {
                System.err.println("Error writing LLVM file: " + e.getMessage());
            }
        }
    }

    private void cmdBenchmark() {
        if (programFile == null) {
            System.out.println("No program file specified for benchmark.");
            return;
        }
        long loadStart = System.nanoTime();
        loadFromFile(false);
        long loadTime = System.nanoTime() - loadStart;
        long runStart = System.nanoTime();
        runProgramCommand();
        long runTime = System.nanoTime() - runStart;
        System.out.printf("Load: %.3f sec  Run: %.3f sec%n", loadTime / 1e9, runTime / 1e9);
    }

    private void cmdStop() {
        if (programFile == null) {
            System.out.println("Nothing to restart.");
            return;
        }
        loadFromFile(false);
        System.out.println("Executor reset – ready to run from start.");
    }

    private void cmdList(String args) {
        if (executor == null) {
            System.out.println("No program loaded.");
            return;
        }
        int count = 10;
        int startIndex;
        if (args == null || args.isBlank()) {
            Integer curIdx = executor.getCurrentIndex();
            startIndex = curIdx != null ? curIdx : 0;
        } else {
            String[] parts = args.trim().split(" ");
            try {
                startIndex = executor.getProgram().findLineIndex(Integer.parseInt(parts[0]));
            } catch (Exception e) {
                System.out.println("Invalid start line");
                return;
            }
            if (parts.length > 1) {
                try { count = Integer.parseInt(parts[1]); } catch (NumberFormatException ignore) {}
            }
        }
        List<String> lines = executor.getProgram().getLinesRange(startIndex, count);
        ControlLocation currentLoc = executor.getCurrentLocation();
        Integer curIdx = currentLoc != null ? currentLoc.getIndex() : null;
        for (int i = 0; i < lines.size(); i++) {
            String prefix = (curIdx != null && curIdx == startIndex + i) ? "*" : " ";
            System.out.println(prefix + lines.get(i));
        }
    }

    private void cmdStmts(String args) {
        if (executor == null) { System.out.println("No program loaded."); return; }
        Integer lineFilter = null;
        if (args != null && !args.isBlank()) {
            try { lineFilter = Integer.parseInt(args.trim()); } catch (NumberFormatException ignore) {}
        }
        for (ProgramLine pl : executor.getProgram()) {
            if (lineFilter != null && lineFilter != pl.getLine()) continue;
            System.out.print(pl.getLine() + " ");
            for (Statement st : pl.getStmts()) {
                System.out.print("\t" + st + "|");
            }
            System.out.println();
        }
    }

    private void cmdForStack() {
        if (executor == null) { System.out.println("No program loaded."); return; }
        Stack<Executor.ForRecord> fs = executor.getForStack();
        System.out.println("FOR stack:");
        if (fs.isEmpty()) {
            System.out.println("\t<empty>");
        }
        for (Executor.ForRecord fr : fs) {
            System.out.println("\t" + fr);
        }
    }

    private void cmdGosubStack() {
        if (executor == null) { System.out.println("No program loaded."); return; }
        Stack<ControlLocation> gs = executor.getGosubStack();
        System.out.println("GOSUB stack:");
        if (gs.isEmpty()) {
            System.out.println("\t<empty>");
        }
        for (ControlLocation cl : gs) {
            ProgramLine line = executor.getProgram().getLine(cl.getIndex());
            System.out.printf("\tLine %d Clause %d%n", line.getLine(), cl.getOffset());
        }
    }

    private void cmdBreak(String args) {
        if (args == null || args.isBlank() || args.equals("list")) {
            if (breakpoints.isEmpty() && dataBreakpoints.isEmpty()) {
                System.out.println("<no breakpoints>");
            } else {
                System.out.println("Breakpoints:");
                for (int[] bp : breakpoints) System.out.printf("\tline %d clause %d%n", bp[0], bp[1]);
                for (String s : dataBreakpoints) System.out.printf("\tdata %s%n", s);
            }
            return;
        }
        if (args.equals("clear")) {
            breakpoints.clear(); dataBreakpoints.clear();
            System.out.println("Breakpoints cleared");
            return;
        }
        String[] parts = args.trim().split(" ");
        if (Character.isDigit(parts[0].charAt(0))) {
            int line = Integer.parseInt(parts[0]);
            int offset = 0;
            if (parts.length > 1) {
                try { offset = Integer.parseInt(parts[1]); } catch (NumberFormatException ignore) {}
            }
            breakpoints.add(new int[]{line, offset});
            System.out.printf("Breakpoint set at line %d clause %d%n", line, offset);
        } else {
            dataBreakpoints.add(parts[0]);
            System.out.println("Data breakpoint set on symbol " + parts[0]);
        }
    }

    private void cmdContinue(boolean singleStep) {
        if (executor == null) {
            System.out.println("No program loaded."); return; }
        try {
            RunStatus rs = executor.runProgram(breakpoints, dataBreakpoints, singleStep);
            switch (rs) {
                case BREAK_CODE -> System.out.println("Breakpoint hit.");
                case BREAK_STEP -> System.out.println("Single step done.");
                case BREAK_DATA -> {
                    ControlLocation loc = executor.getCurrentLocation();
                    ProgramLine line = executor.getProgram().getLine(loc.getIndex());
                    System.out.printf("Data Breakpoint before line %d clause %d%n", line.getLine(), loc.getOffset());
                }
                default -> System.out.println("Program completed with status " + rs);
            }
        } catch (BasicRuntimeError | BasicSyntaxError e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void cmdPrint(String expr) {
        if (expr == null || expr.isBlank()) {
            System.out.println("Usage: ? <expression>");
            return;
        }
        try {
            Map<String,Object> symTab = (executor != null) ? executor.getSymbols() : new HashMap<>();
            ExpressionEvaluator eval = new ExpressionEvaluator(symTab);
            Object result = eval.evaluate(expr);
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("Error evaluating: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Help handling
    // ---------------------------------------------------------------------
    private void printHelp(String cmd) {
        switch (cmd.toLowerCase(Locale.ROOT)) {
            case "run", "r" -> System.out.println("run : execute current program from beginning");
            case "load" -> System.out.println("load <file> : load BASIC file");
            case "save" -> printUsageSave();
            case "clear" -> System.out.println("clear : remove program and state");
            case "format" -> System.out.println("format : canonical reformat");
            case "renum" -> printUsageRenum();
            case "llvm" -> System.out.println("llvm [file] : print or save LLVM IR");
            case "benchmark" -> System.out.println("benchmark : time load+run");
            case "stop" -> System.out.println("stop : reset executor to start");
            case "symbols", "sym" -> System.out.println("symbols [name] : show symbol(s)");
            case "list" -> System.out.println("list [start [count]] : list program lines");
            case "stmts" -> System.out.println("stmts [line] : list statements for a specific line");
            case "fors", "forstack" -> System.out.println("fors | forstack : show FOR stack");
            case "gosubs" -> System.out.println("gosubs : show GOSUB stack");
            case "break" -> System.out.println("break <line> [clause] | break <symbol> | break list | break clear");
            case "continue", "c" -> System.out.println("continue : resume after breakpoint");
            case "next", "n" -> System.out.println("next : single-step one statement");
            case "?" -> System.out.println("? <expr> : evaluate expression");
            case "coverage" -> System.out.println("coverage [lines] : report coverage after 'run coverage'");
            default -> System.out.println("Unknown command: " + cmd);
        }
    }

    // ---------------------------------------------------------------------
    // Command resolution helper
    // ---------------------------------------------------------------------
    private static final List<String> COMMAND_LIST = Arrays.asList(
            "?", "run", "load", "save", "clear", "format", "renum", "list", "stmts", "coverage",
            "fors", "forstack", "gosubs", "llvm", "benchmark", "stop", "symbols",
            "quit", "exit", "help");

    private String resolveCommand(String prefix) {
        // exact match first
        for (String cmd : COMMAND_LIST) {
            if (cmd.equals(prefix)) return cmd;
        }
        List<String> matches = new ArrayList<>();
        for (String cmd : COMMAND_LIST) {
            if (cmd.startsWith(prefix)) matches.add(cmd);
        }
        if (matches.size() == 1) return matches.get(0);
        return null; // unknown or ambiguous
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
