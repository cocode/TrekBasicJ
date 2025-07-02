package com.worldware;

import java.io.*;
import java.util.*;

/**
 * This class executes BASIC programs
 */
public class Executor {
    private final Program program;
    private ControlLocation location;
    private RunStatus runStatus;
    private PrintWriter traceFile;
    private boolean stackTrace;
    private ControlLocation gotoLocation;
    private final Stack<ControlLocation> gosubStack;
    private final Stack<ForRecord> forStack;
    private final Map<String, Object> symbols;
    private final Set<String> dataBreakpoints;
    private boolean modified;

    public Executor(Program program) throws IOException {
        this(program, false);
    }

    public Executor(Program program, boolean trace) throws IOException {
        this.program = program;
        this.location = new ControlLocation(0, 0);
        this.runStatus = RunStatus.RUN;
        this.traceFile = trace ? new PrintWriter(new FileWriter("tracefile.txt")) : null;
        this.stackTrace = false;
        this.gotoLocation = null;
        this.gosubStack = new Stack<>();
        this.forStack = new Stack<>();
        this.symbols = new HashMap<>();
        this.dataBreakpoints = new HashSet<>();
        this.modified = false;
        
        setupProgram();
    }

    private void setupProgram() {
        // Initialize any built-in symbols/functions here
        // TODO: Add built-in functions like INT, RND, SGN, etc.
    }

    /**
     * Run the program
     */
    public RunStatus runProgram() throws BasicSyntaxError, BasicRuntimeError {
        runStatus = RunStatus.RUN;
        
        while (true) {
            if (atEnd()) {
                runStatus = RunStatus.END_OF_PROGRAM;
                return runStatus;
            }

            ProgramLine currentLine = getCurrentLine();
            
            if (traceFile != null && location.getOffset() == 0) {
                traceFile.println(">" + currentLine.getSource());
            }

            Statement stmt = getCurrentStatement();
            
            if (traceFile != null) {
                traceFile.println("\t" + stmt);
            }

            try {
                executeStatement(stmt);
            } catch (BasicSyntaxError bse) {
                runStatus = RunStatus.END_ERROR_SYNTAX;
                throw new BasicSyntaxError(bse.getMessage(), currentLine.getLine());
            } catch (BasicRuntimeError bre) {
                runStatus = RunStatus.END_ERROR_RUNTIME;
                throw bre;
            } catch (Exception e) {
                runStatus = RunStatus.END_ERROR_INTERNAL;
                throw new BasicInternalError("Internal error in line " + currentLine.getLine() + ": " + e.getMessage());
            }

            if (gotoLocation != null) {
                if (traceFile != null) {
                    ProgramLine destinationLine = program.getLine(gotoLocation.getIndex());
                    traceFile.println("\tControl Transfer from line " + location + " TO line " + destinationLine.getLine() + ": " + gotoLocation + ".");
                }
                location = gotoLocation;
                gotoLocation = null;
            } else {
                // Advance to next statement
                ControlLocation nextLocation = getNextStatement();
                if (nextLocation == null) {
                    runStatus = RunStatus.END_OF_PROGRAM;
                    location = new ControlLocation(null, 0);
                } else {
                    location = nextLocation;
                }
            }
        }
    }

    /**
     * Execute a single statement
     */
    private void executeStatement(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        String keyword = stmt.getKeyword();
        
        switch (keyword) {
            case "REM" -> { /* Do nothing for comments */ }
            case "PRINT" -> executePrint(stmt);
            case "LET" -> executeAssignment(stmt);
            case "END" -> { runStatus = RunStatus.END_CMD; }
            case "STOP" -> { runStatus = RunStatus.END_STOP; }
            case "GOTO" -> executeGoto(stmt);
            case "GOSUB" -> executeGosub(stmt);
            case "RETURN" -> executeReturn();
            case "FOR" -> executeFor(stmt);
            case "NEXT" -> executeNext(stmt);
            case "IF" -> executeIf(stmt);
            case "THEN" -> { /* THEN is handled by IF */ }
            case "ELSE" -> { /* ELSE is handled by IF */ }
            case "CLEAR" -> executeClear(stmt);
            default -> throw new BasicSyntaxError("Unknown statement: " + keyword);
        }
    }

    private void executePrint(Statement stmt) {
        // Simple PRINT implementation
        String args = stmt.getArgs();
        if (args.isEmpty()) {
            System.out.println(); // Empty line
        } else {
            // TODO: Implement proper expression evaluation
            // For now, just handle simple string literals
            if (args.startsWith("\"") && args.endsWith("\"")) {
                String text = args.substring(1, args.length() - 1);
                System.out.println(text);
            } else {
                // Try to evaluate as variable or expression
                Object value = evaluateExpression(args);
                System.out.println(value);
            }
        }
    }

    private void executeAssignment(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof AssignmentStatement)) {
            throw new BasicSyntaxError("Invalid assignment statement");
        }
        
        AssignmentStatement assignment = (AssignmentStatement) stmt;
        String variable = assignment.getVariable();
        String expression = assignment.getExpression();
        
        Object value = evaluateExpression(expression);
        symbols.put(variable.toUpperCase(), value);
    }

    private void executeGoto(Statement stmt) throws BasicSyntaxError {
        String args = stmt.getArgs().trim();
        try {
            int lineNumber = Integer.parseInt(args);
            int lineIndex = program.findLineIndex(lineNumber);
            gotoLocation = new ControlLocation(lineIndex, 0);
        } catch (NumberFormatException e) {
            throw new BasicSyntaxError("Invalid line number in GOTO: " + args);
        }
    }

    private void executeGosub(Statement stmt) throws BasicSyntaxError {
        // Save current location for RETURN
        ControlLocation nextLocation = getNextStatement();
        if (nextLocation != null) {
            gosubStack.push(nextLocation);
        }
        
        // Execute the GOTO part
        executeGoto(stmt);
    }

    private void executeReturn() throws BasicSyntaxError {
        if (gosubStack.isEmpty()) {
            throw new BasicSyntaxError("RETURN without GOSUB");
        }
        
        gotoLocation = gosubStack.pop();
    }

    private void executeFor(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof ForStatement)) {
            throw new BasicSyntaxError("Invalid FOR statement");
        }
        
        ForStatement forStmt = (ForStatement) stmt;
        String var = forStmt.getIndexVariable();
        String startExpr = forStmt.getStartExpression();
        String endExpr = forStmt.getEndExpression();
        String stepExpr = forStmt.getStepExpression();
        
        // Evaluate expressions and set up loop
        Object startValue = evaluateExpression(startExpr);
        Object endValue = evaluateExpression(endExpr);
        Object stepValue = evaluateExpression(stepExpr);
        
        // Set loop variable to start value
        symbols.put(var.toUpperCase(), startValue);
        
        // Push FOR record onto stack
        forStack.push(new ForRecord(var.toUpperCase(), endValue, stepValue, location));
    }

    private void executeNext(Statement stmt) throws BasicSyntaxError {
        String var = stmt.getArgs().trim().toUpperCase();
        
        if (forStack.isEmpty()) {
            throw new BasicSyntaxError("NEXT without FOR");
        }
        
        ForRecord forRecord = forStack.peek();
        if (!forRecord.variable().equals(var)) {
            throw new BasicSyntaxError("NEXT variable mismatch");
        }
        
        // Get current value and step
        Object currentValue = symbols.get(var);
        Object stepValue = forRecord.step();
        Object endValue = forRecord.stop();
        
        // TODO: Implement proper numeric operations
        // For now, assume all are numbers
        if (currentValue instanceof Number && stepValue instanceof Number && endValue instanceof Number) {
            double current = ((Number) currentValue).doubleValue();
            double step = ((Number) stepValue).doubleValue();
            double end = ((Number) endValue).doubleValue();
            
            current += step;
            symbols.put(var, current);
            
            // Check if loop should continue
            boolean continueLoop = (step > 0) ? (current <= end) : (current >= end);
            
            if (continueLoop) {
                // Go back to FOR statement
                gotoLocation = new ControlLocation(forRecord.location().getIndex(), forRecord.location().getOffset() + 1);
            } else {
                // Exit loop
                forStack.pop();
            }
        }
    }

    private void executeIf(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof IfStatement)) {
            throw new BasicSyntaxError("Invalid IF statement");
        }
        
        IfStatement ifStmt = (IfStatement) stmt;
        String condition = ifStmt.getCondition();
        
        // TODO: Implement proper condition evaluation
        // For now, just handle simple cases
        boolean result = evaluateCondition(condition);
        
        if (!result) {
            // Skip to next line if condition is false
            gotoLocation = getNextLine();
        }
    }

    private void executeClear(Statement stmt) {
        // CLEAR statement - clears all variables
        // In some BASIC dialects, it can take parameters for memory allocation
        symbols.clear();
    }

    /**
     * Simple expression evaluator - needs major expansion
     */
    private Object evaluateExpression(String expression) {
        expression = expression.trim();
        
        // Handle string literals
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }
        
        // Handle simple addition (e.g., F1+F2)
        if (expression.contains("+") && !expression.startsWith("\"")) {
            String[] parts = expression.split("\\+");
            if (parts.length == 2) {
                Object left = evaluateExpression(parts[0].trim());
                Object right = evaluateExpression(parts[1].trim());
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
            }
        }
        
        // Handle numeric literals
        try {
            if (expression.contains(".")) {
                return Double.parseDouble(expression);
            } else {
                return Integer.parseInt(expression);
            }
        } catch (NumberFormatException e) {
            // Not a number, try as variable
        }
        
        // Handle variables
        String varName = expression.toUpperCase();
        if (symbols.containsKey(varName)) {
            return symbols.get(varName);
        }
        
        // TODO: Handle expressions with operators, function calls, etc.
        
        return 0; // Default value
    }

    /**
     * Simple condition evaluator - needs major expansion
     */
    private boolean evaluateCondition(String condition) {
        // TODO: Implement proper condition evaluation
        // For now, just return true to allow simple testing
        return true;
    }

    // Utility methods
    
    public boolean atEnd() {
        return location.getIndex() == null || location.getIndex() >= program.size();
    }

    public ProgramLine getCurrentLine() {
        if (atEnd()) {
            return null;
        }
        return program.getLine(location.getIndex());
    }

    public Statement getCurrentStatement() {
        ProgramLine line = getCurrentLine();
        if (line == null || location.getOffset() >= line.getStmts().size()) {
            return null;
        }
        return line.getStmts().get(location.getOffset());
    }

    public ControlLocation getNextStatement() {
        if (atEnd()) {
            return null;
        }
        return program.getNextStatementLocation(location.getIndex(), location.getOffset());
    }

    public ControlLocation getNextLine() {
        if (atEnd()) {
            return null;
        }
        Integer nextIndex = program.getNextIndex(location.getIndex());
        return nextIndex != null ? new ControlLocation(nextIndex, 0) : null;
    }

    // Symbol table methods
    
    public Object getSymbol(String name) {
        return symbols.get(name.toUpperCase());
    }

    public void putSymbol(String name, Object value) {
        symbols.put(name.toUpperCase(), value);
    }

    public int getSymbolCount() {
        return symbols.size();
    }

    public void close() throws IOException {
        if (traceFile != null) {
            traceFile.close();
        }
    }

    /**
     * FOR loop record
     */
    public record ForRecord(String variable, Object stop, Object step, ControlLocation location) {}
} 