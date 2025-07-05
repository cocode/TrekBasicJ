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
    
    // Data management for READ/DATA/RESTORE
    private final List<Object> dataValues;
    private int dataPointer;
    
    // User-defined functions management
    private final Map<String, DefStatement> userFunctions;

    // Breakpoint and single-step support
    private List<int[]> codeBreakpoints = Collections.emptyList();
    private List<String> watchSymbols = Collections.emptyList();
    private boolean singleStepMode = false;

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
        this.dataValues = new ArrayList<>();
        this.dataPointer = 0;
        this.userFunctions = new HashMap<>();
        
        setupProgram();
    }

    private void setupProgram() {
        // Initialize built-in functions
        symbols.put("INT", "BUILTIN_FUNCTION");
        symbols.put("RND", "BUILTIN_FUNCTION");
        symbols.put("SGN", "BUILTIN_FUNCTION");
        symbols.put("EXP", "BUILTIN_FUNCTION");
        symbols.put("LOG", "BUILTIN_FUNCTION");
        symbols.put("SIN", "BUILTIN_FUNCTION");
        symbols.put("COS", "BUILTIN_FUNCTION");
        symbols.put("TAN", "BUILTIN_FUNCTION");
        symbols.put("ATN", "BUILTIN_FUNCTION");
        symbols.put("SQR", "BUILTIN_FUNCTION");
        symbols.put("ABS", "BUILTIN_FUNCTION");
        symbols.put("LEFT$", "BUILTIN_FUNCTION");
        symbols.put("RIGHT$", "BUILTIN_FUNCTION");
        symbols.put("MID$", "BUILTIN_FUNCTION");
        symbols.put("LEN", "BUILTIN_FUNCTION");
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

            // Breakpoint before executing statement
            if (hitCodeBreakpoint()) {
                runStatus = RunStatus.BREAK_CODE;
                return runStatus;
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

            // Check if we should terminate due to END or STOP
            if (runStatus == RunStatus.END_CMD || runStatus == RunStatus.END_STOP) {
                return runStatus;
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

            // Single-step support – stop after executing one statement
            if (singleStepMode) {
                singleStepMode = false; // consume step
                runStatus = RunStatus.BREAK_STEP;
                return runStatus;
            }

            // Breakpoint support after statement execution
            if (hitCodeBreakpoint()) {
                runStatus = RunStatus.BREAK_CODE;
                return runStatus;
            }
        }
    }

    public RunStatus runProgram(List<int[]> breakpoints, List<String> dataBps, boolean singleStep) throws BasicSyntaxError, BasicRuntimeError {
        this.codeBreakpoints = breakpoints != null ? breakpoints : Collections.emptyList();
        this.watchSymbols = dataBps != null ? dataBps : Collections.emptyList();
        this.singleStepMode = singleStep;
        RunStatus rs = runProgram();
        // reset
        this.codeBreakpoints = Collections.emptyList();
        this.watchSymbols = Collections.emptyList();
        this.singleStepMode = false;
        return rs;
    }

    private boolean hitCodeBreakpoint() {
        if (codeBreakpoints.isEmpty()) return false;
        int lineNum = getCurrentLine().getLine();
        int offset = location.getOffset();
        for (int[] bp : codeBreakpoints) {
            if (bp[0] == lineNum && bp[1] == offset) return true;
        }
        return false;
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
            case "STOP" -> {
                Object aVar = symbols.get("A$");
                System.out.printf("STOP encountered at line %d (A$=%s, len=%s)%n", getCurrentLine().getLine(), aVar, aVar==null?"null":aVar.toString().length());
                runStatus = RunStatus.END_STOP;
            }
            case "GOTO" -> executeGoto(stmt);
            case "GOSUB" -> executeGosub(stmt);
            case "RETURN" -> executeReturn();
            case "FOR" -> executeFor(stmt);
            case "NEXT" -> executeNext(stmt);
            case "IF" -> executeIf(stmt);
            case "THEN" -> { /* THEN is handled by IF */ }
            case "ELSE" -> { /* ELSE is handled by IF */ }
            case "CLEAR" -> executeClear(stmt);
            case "DIM" -> executeDim(stmt);
            case "INPUT" -> executeInput(stmt);
            case "READ" -> executeRead(stmt);
            case "DATA" -> executeData(stmt);
            case "RESTORE" -> executeRestore(stmt);
            case "DEF" -> executeDef(stmt);
            case "ON" -> executeOn(stmt);
            default -> throw new BasicSyntaxError("Unknown statement: " + keyword);
        }
    }

    private void executePrint(Statement stmt) throws BasicRuntimeError {
        if (stmt.getArgs().isEmpty()) {
            System.out.println();
            return;
        }

        // Parse print list handling separators ; and , at top level only
        List<String> items = new ArrayList<>();
        List<Character> seps = new ArrayList<>(); // separator preceding item (except first)
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int parenDepth = 0;

        String args = stmt.getArgs();
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (!inString) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    if (parenDepth > 0) parenDepth--;
                }

                if (parenDepth == 0 && (c == ';' || c == ',')) {
                    items.add(current.toString());
                    seps.add(c);
                    current.setLength(0);
                    continue; // do not keep separator
                }
            }

            current.append(c);
        }
        items.add(current.toString());

        // Determine newline requirement by looking at last non-space char
        boolean newline = true;
        for (int i = args.length() - 1; i >= 0; i--) {
            char c = args.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (c == ';' || c == ',') newline = false;
                break;
            }
        }

        // Print items
        for (int idx = 0; idx < items.size(); idx++) {
            String part = items.get(idx).trim();
            if (!part.isEmpty()) {
                Object value;
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    value = part.substring(1, part.length() - 1);
                    System.out.print(value);
                } else {
                    value = evaluateExpression(part);
                    // BASIC prints a leading space before positive numbers
                    if (value instanceof Number num && num.doubleValue() >= 0) {
                        System.out.print(" " + value + " ");
                    } else {
                        System.out.print(value);
                    }
                }
            }

            // Handle separator spacing (except after last item)
            if (idx < seps.size()) {
                char sep = seps.get(idx);
                if (sep == ',') {
                    System.out.print("    "); // simple tab spacing approximation
                } // semicolon -> no extra spacing
            }
        }

        if (newline) System.out.println();
    }

    private void executeAssignment(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        if (!(stmt instanceof AssignmentStatement)) {
            throw new BasicSyntaxError("Invalid assignment statement");
        }
        
        AssignmentStatement assignment = (AssignmentStatement) stmt;
        String valueExpr = assignment.getExpression();
        Object value = evaluateExpression(valueExpr);
        
        // Normalize numeric value: if the expression was a plain numeric literal (no letters or parentheses)
        // and the resulting value is an integral Double, store it as Integer so that symbols keep expected type.
        if (value instanceof Double d && d == Math.rint(d)) {
            boolean plainNumber = valueExpr.matches("\\s*[-+]?[0-9]+\\s*");
            if (plainNumber) {
                value = (int) d.doubleValue();
            }
        }
        
        if (assignment.isArrayAssignment()) {
            // Array assignment like A(1) = 5
            executeArrayAssignment(assignment, value);
        } else {
            // Simple variable assignment
            String variable = assignment.getVariable().toUpperCase();
            symbols.put(variable, value);
        }
    }
    
    private void executeArrayAssignment(AssignmentStatement assignment, Object value) throws BasicSyntaxError, BasicRuntimeError {
        String arrayName = assignment.getArrayName();
        String indices = assignment.getArrayIndices();
        
        // Look up the array
        String arrayKey = "ARRAY:" + arrayName;
        if (!symbols.containsKey(arrayKey)) {
            throw new BasicSyntaxError("Array not defined: " + arrayName);
        }
        
        Object array = symbols.get(arrayKey);
        
        // Parse indices
        String[] indexStrs = indices.split(",");
        
        try {
            // Navigate to the parent of the target element
            Object current = array;
            for (int i = 0; i < indexStrs.length - 1; i++) {
                int index = (int) toNumber(evaluateExpression(indexStrs[i].trim()));
                index -= Dialect.ARRAY_OFFSET;
                
                if (current instanceof Object[]) {
                    Object[] arr = (Object[]) current;
                    if (index >= 0 && index < arr.length) {
                        current = arr[index];
                    } else {
                        throw new BasicSyntaxError("Array index out of bounds: " + index);
                    }
                } else {
                    throw new BasicSyntaxError("Too many array dimensions");
                }
            }
            
            // Set the final element
            int finalIndex = (int) toNumber(evaluateExpression(indexStrs[indexStrs.length - 1].trim()));
            finalIndex -= Dialect.ARRAY_OFFSET;
            if (current instanceof Object[]) {
                Object[] arr = (Object[]) current;
                if (finalIndex >= 0 && finalIndex < arr.length) {
                    arr[finalIndex] = value;
                } else {
                    throw new BasicSyntaxError("Array index out of bounds: " + finalIndex);
                }
            } else {
                throw new BasicSyntaxError("Invalid array assignment");
            }
        } catch (NumberFormatException e) {
            throw new BasicSyntaxError("Invalid array index");
        }
    }
    
    private double toNumber(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void executeGoto(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        String args = stmt.getArgs().trim();
        try {
            // First try to parse as a literal line number
            int lineNumber = Integer.parseInt(args);
            int lineIndex = program.findLineIndex(lineNumber);
            gotoLocation = new ControlLocation(lineIndex, 0);
        } catch (NumberFormatException e) {
            // If not a literal number, evaluate as an expression (computed GOTO)
            Object result = evaluateExpression(args);
            if (result instanceof Number) {
                int lineNumber = ((Number) result).intValue();
                int lineIndex = program.findLineIndex(lineNumber);
                gotoLocation = new ControlLocation(lineIndex, 0);
            } else {
                throw new BasicSyntaxError("Invalid line number in GOTO: " + args + " (evaluated to: " + result + ")");
            }
        }
    }

    private void executeGosub(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        // Save current location for RETURN
        ControlLocation nextLocation = getNextStatement();
        if (nextLocation != null) {
            gosubStack.push(nextLocation);
        }
        
        // Handle computed GOSUB (similar to computed GOTO)
        String args = stmt.getArgs().trim();
        try {
            // First try to parse as a literal line number
            int lineNumber = Integer.parseInt(args);
            int lineIndex = program.findLineIndex(lineNumber);
            gotoLocation = new ControlLocation(lineIndex, 0);
        } catch (NumberFormatException e) {
            // If not a literal number, evaluate as an expression (computed GOSUB)
            Object result = evaluateExpression(args);
            if (result instanceof Number) {
                int lineNumber = ((Number) result).intValue();
                int lineIndex = program.findLineIndex(lineNumber);
                gotoLocation = new ControlLocation(lineIndex, 0);
            } else {
                throw new BasicSyntaxError("Invalid line number in GOSUB: " + args + " (evaluated to: " + result + ")");
            }
        }
    }

    private void executeReturn() throws BasicSyntaxError {
        if (gosubStack.isEmpty()) {
            throw new BasicSyntaxError("RETURN without GOSUB");
        }
        
        gotoLocation = gosubStack.pop();
    }

    private void executeFor(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
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
        
        if (!(stepValue instanceof Number) || ((Number)stepValue).doubleValue() == 0.0) {
            throw new BasicRuntimeError("STEP value cannot be 0");
        }

        // No early skipping of the loop body even if the initial value is already
        // beyond the end value in the direction of STEP. This matches the
        // behaviour expected by existing unit tests where the body is executed
        // once before the loop condition is evaluated at the NEXT statement.
        
        // Set loop variable to start value
        symbols.put(var.toUpperCase(), startValue);
        
        // If we re-enter the same FOR line (via GOTO) while its record is still
        // on the stack, classic BASIC resets the loop by discarding the old
        // control record and pushing a fresh one, which also re-initialises
        // the loop variable.  This allows patterns like "GOTO 100" inside the
        // loop body to restart the iteration sequence.
        if (!forStack.isEmpty()) {
            ForRecord top = forStack.peek();
            if (top.variable().equals(var.toUpperCase()) && top.location().equals(location)) {
                forStack.pop();
            }
        }

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
            
            double nextVal = current + step;

            // Will the next value still be within bounds?
            boolean continueLoop = (step > 0) ? (nextVal <= end) : (nextVal >= end);

            if (continueLoop) {
                // Advance variable and loop again
                symbols.put(var, nextVal);
                gotoLocation = getNextStatementFrom(forRecord.location());
            } else {
                // Clamp variable to final legal value (classic BASIC behaviour)
                symbols.put(var, current);
                forStack.pop();
            }
        }
    }

    private void executeIf(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        if (!(stmt instanceof IfStatement)) {
            throw new BasicSyntaxError("Invalid IF statement");
        }
        
        IfStatement ifStmt = (IfStatement) stmt;
        String condition = ifStmt.getCondition();
        
        boolean result = evaluateCondition(condition);
        
        if (stmt instanceof IfThenElseStatement) {
            IfThenElseStatement ifThenElseStmt = (IfThenElseStatement) stmt;
            if (result) {
                // Execute the THEN statements
                executeThenStatements(ifThenElseStmt.getThenStatements());
            } else {
                // Execute the ELSE statements
                executeThenStatements(ifThenElseStmt.getElseStatements());
            }
            // Whether true or false, we continue to the next statement after IF
        } else if (stmt instanceof IfThenStatement) {
            IfThenStatement ifThenStmt = (IfThenStatement) stmt;
            if (result) {
                // Execute the THEN statements
                executeThenStatements(ifThenStmt.getThenStatements());
            }
            // Whether true or false, we continue to the next statement after IF
        } else {
            // Simple IF without THEN - skip to next line if false
            if (!result) {
                gotoLocation = getNextLine();
            }
        }
    }
    
    private void executeThenStatements(String thenStatements) throws BasicSyntaxError, BasicRuntimeError {
        // Parse and execute the statements after THEN
        List<String> statements = smartSplit(thenStatements, ':');
        
        for (String statementText : statements) {
            statementText = statementText.trim();
            if (!statementText.isEmpty()) {
                // Parse and remember any pre-existing jump target
                Statement stmt = BasicLoader.parseStatement(statementText);
                ControlLocation beforeJump = gotoLocation; // may be null

                // Execute the statement that belongs to the THEN-part
                executeStatement(stmt);

                /*
                 * Only stop iterating when this statement caused a *new* control
                 * transfer.  Merely inheriting a gotoLocation that was already
                 * set by a previous statement must not prevent the remaining
                 * THEN-statements from running.  (Example: "IF X THEN A=1:B=2:GOTO 5000")
                 */
                if (gotoLocation != null && !gotoLocation.equals(beforeJump)) {
                    break;
                }
            }
        }
    }
    
    /**
     * Smart split that doesn't split on separators inside string literals
     */
    private static List<String> smartSplit(String text, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"') {
                inString = !inString;
                current.append(c);
            } else if (c == separator && !inString) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }

    private void executeClear(Statement stmt) {
        // CLEAR statement - clears all variables
        // In some BASIC dialects, it can take parameters for memory allocation
        symbols.clear();
    }
    
    private void executeDim(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof DimStatement)) {
            throw new BasicSyntaxError("Invalid DIM statement");
        }
        
        DimStatement dimStmt = (DimStatement) stmt;
        
        for (DimStatement.ArrayDeclaration declaration : dimStmt.getArrayDeclarations()) {
            String arrayName = declaration.getName();
            Object array = declaration.createArray();
            
            // Store arrays with a special prefix to distinguish from scalar variables
            String arrayKey = "ARRAY:" + arrayName;
            symbols.put(arrayKey, array);
        }
    }
    
    private void executeInput(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        if (!(stmt instanceof InputStatement)) {
            throw new BasicSyntaxError("Invalid INPUT statement");
        }
        
        InputStatement inputStmt = (InputStatement) stmt;
        
        // Display prompt if present
        if (inputStmt.hasPrompt()) {
            System.out.print(inputStmt.getPrompt());
            System.out.print(" ");
        } else {
            System.out.print("? "); // Default BASIC prompt
        }
        
        // Read input from console
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            String input = reader.readLine();
            
            if (input == null) {
                input = ""; // Handle EOF
            }
            
            // Parse input values (comma-separated)
            String[] inputValues = input.split(",");
            java.util.List<String> variables = inputStmt.getVariables();
            
            // Assign values to variables
            for (int i = 0; i < variables.size(); i++) {
                String variable = variables.get(i);
                String value = "";
                
                if (i < inputValues.length) {
                    value = inputValues[i].trim();
                }
                
                // Convert and store the value
                Object convertedValue;
                if (variable.endsWith("$")) {
                    // String variable
                    if (Dialect.UPPERCASE_INPUT == 1) {
                        convertedValue = value.toUpperCase();
                    } else {
                        convertedValue = value;
                    }
                } else {
                    // Numeric variable
                    try {
                        if (value.contains(".")) {
                            convertedValue = Double.parseDouble(value);
                        } else if (!value.isEmpty()) {
                            convertedValue = Integer.parseInt(value);
                        } else {
                            convertedValue = 0; // Default for empty input
                        }
                    } catch (NumberFormatException e) {
                        convertedValue = 0; // Default for invalid numeric input
                    }
                }
                
                symbols.put(variable, convertedValue);
            }
            
        } catch (java.io.IOException e) {
            throw new BasicRuntimeError("Error reading input: " + e.getMessage());
        }
    }
    
    private void executeRead(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        if (!(stmt instanceof ReadStatement)) {
            throw new BasicSyntaxError("Invalid READ statement");
        }
        
        ReadStatement readStmt = (ReadStatement) stmt;
        
        for (String variable : readStmt.getVariables()) {
            if (dataPointer >= dataValues.size()) {
                throw new BasicRuntimeError("Out of data");
            }
            
            Object value = dataValues.get(dataPointer++);
            symbols.put(variable, value);
        }
    }
    
    private void executeData(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof DataStatement)) {
            throw new BasicSyntaxError("Invalid DATA statement");
        }
        
        DataStatement dataStmt = (DataStatement) stmt;
        dataValues.addAll(dataStmt.getDataValues());
    }
    
    private void executeRestore(Statement stmt) throws BasicSyntaxError {
        String args = stmt.getArgs().trim();
        
        if (args.isEmpty()) {
            // RESTORE without arguments resets to beginning
            dataPointer = 0;
        } else {
            // RESTORE with line number (not implemented yet)
            throw new BasicSyntaxError("RESTORE with line number not yet implemented");
        }
    }
    
    private void executeDef(Statement stmt) throws BasicSyntaxError {
        if (!(stmt instanceof DefStatement)) {
            throw new BasicSyntaxError("Invalid DEF statement");
        }
        
        DefStatement defStmt = (DefStatement) stmt;
        System.out.println("DEBUG: Registering user function: " + defStmt.getFunctionName() + " = " + defStmt.getExpression());
        userFunctions.put(defStmt.getFunctionName(), defStmt);
    }

    /**
     * Execute an ON GOTO / ON GOSUB computed jump
     * Syntax:  ON <expr> GOTO line1,line2,...   or   ON <expr> GOSUB line1,line2,...
     */
    private void executeOn(Statement stmt) throws BasicSyntaxError, BasicRuntimeError {
        String upper = stmt.getArgs().toUpperCase();
        boolean gosub = upper.contains("GOSUB");
        String keyword = gosub ? "GOSUB" : "GOTO";

        int kwIndex = upper.indexOf(keyword);
        if (kwIndex == -1) {
            throw new BasicSyntaxError("ON statement missing " + keyword);
        }

        String exprPart = stmt.getArgs().substring(0, kwIndex).trim();
        String listPart = stmt.getArgs().substring(kwIndex + keyword.length()).trim();

        Object exprVal = evaluateExpression(exprPart);
        if (!(exprVal instanceof Number)) {
            return; // Non-numeric expression – fall through (no jump)
        }
        int index = ((Number) exprVal).intValue(); // BASIC is 1-based
        if (index < 1) {
            return; // Out of range – continue execution
        }

        String[] dests = listPart.split(",");
        if (index > dests.length) {
            return; // Out of range – continue
        }

        String destStr = dests[index - 1].trim();
        if (destStr.isEmpty()) {
            return;
        }
        try {
            int lineNumber = Integer.parseInt(destStr);
            int lineIndex = program.findLineIndex(lineNumber);
            if (gosub) {
                // Save return location
                ControlLocation nextLocation = getNextStatement();
                if (nextLocation != null) {
                    gosubStack.push(nextLocation);
                }
            }
            gotoLocation = new ControlLocation(lineIndex, 0);
        } catch (NumberFormatException e) {
            throw new BasicSyntaxError("Invalid line number in ON statement: " + destStr);
        }
    }

    /**
     * Expression evaluator using the dedicated ExpressionEvaluator class
     */
    private Object evaluateExpression(String expression) throws BasicRuntimeError {
        ExpressionEvaluator evaluator = new ExpressionEvaluator(symbols, userFunctions);
        try {
            return evaluator.evaluate(expression);
        } catch (RuntimeException re) {
            if (re.getMessage()!=null && re.getMessage().startsWith("Undefined variable:")) {
                throw new BasicRuntimeError(re.getMessage());
            }
            throw re;
        }
    }

    /**
     * Condition evaluator using the dedicated ExpressionEvaluator class
     */
    private boolean evaluateCondition(String condition) throws BasicRuntimeError {
        ExpressionEvaluator evaluator = new ExpressionEvaluator(symbols, userFunctions);
        try {
            return evaluator.evaluateCondition(condition);
        } catch (RuntimeException re) {
            if (re.getMessage()!=null && re.getMessage().startsWith("Undefined variable:")) {
                throw new BasicRuntimeError(re.getMessage());
            }
            throw re;
        }
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
        return getNextStatementFrom(location);
    }

    private ControlLocation getNextStatementFrom(ControlLocation loc) {
        return program.getNextStatementLocation(loc.getIndex(), loc.getOffset());
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
        // Count only user variables, not built-in functions
        return (int) symbols.entrySet().stream()
            .filter(entry -> !"BUILTIN_FUNCTION".equals(entry.getValue()))
            .count();
    }

    public Map<String, Object> getSymbols() {
        return new HashMap<>(symbols);
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

    public Program getProgram() {
        return program;
    }

    public Integer getCurrentIndex() {
        return location != null ? location.getIndex() : null;
    }

    public ControlLocation getCurrentLocation() {
        return location;
    }

    public Stack<ForRecord> getForStack() {
        return forStack;
    }

    public Stack<ControlLocation> getGosubStack() {
        return gosubStack;
    }
} 