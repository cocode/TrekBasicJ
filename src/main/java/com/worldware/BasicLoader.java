package com.worldware;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and parses BASIC programs from source code
 */
public class BasicLoader {
    
    private static final Pattern LINE_PATTERN = Pattern.compile("^(\\d+)\\s*(.*)$");
    
    /**
     * Smart split that doesn't split on separators inside string literals
     */
    static List<String> smartSplit(String text, char separator) {
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
    
    /**
     * Split a BASIC source line into statements separated by ':' taking into
     * account that any ':' appearing after a THEN in an IF-statement belongs to
     * that IF and must not start a new statement.
     */
    private static List<String> splitStatements(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean afterThen = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (!inString) {
                // Detect the keyword THEN (word boundary, case-insensitive)
                if (!afterThen && (c == 'T' || c == 't')) {
                    String upperRest = text.substring(i).toUpperCase();
                    if (upperRest.startsWith("THEN") && (i == 0 || !Character.isLetter(text.charAt(i - 1)))) {
                        afterThen = true;
                    }
                }

                if (c == ':' && !afterThen) {
                    parts.add(current.toString());
                    current.setLength(0);
                    continue; // Do not include ':'
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }
    
    /**
     * Tokenize a list of BASIC source lines into a Program
     */
    public static Program tokenize(List<String> lines) throws BasicSyntaxError {
        List<ProgramLine> programLines = new ArrayList<>();
        Set<Integer> seenLineNumbers = new HashSet<>();
        
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                ProgramLine programLine = tokenizeLine(line);
                if (seenLineNumbers.contains(programLine.getLine())) {
                    throw new BasicSyntaxError("Duplicate line number: " + programLine.getLine());
                }
                seenLineNumbers.add(programLine.getLine());
                programLines.add(programLine);
            }
        }
        
        return new Program(programLines);
    }
    
    /**
     * Parse an IF statement that may contain THEN and ELSE
     */
    private static Statement parseIfStatement(String keyword, String args) throws BasicSyntaxError {
        String upperArgs = args.toUpperCase();
        int thenIndex = upperArgs.indexOf("THEN");
        
        if (thenIndex == -1) {
            // Simple IF without THEN
            return new IfStatement(keyword, args);
        }
        
        String condition = args.substring(0, thenIndex).trim();
        String afterThen = args.substring(thenIndex + 4).trim();
        
        // Check for ELSE clause
        int elseIndex = upperArgs.indexOf("ELSE", thenIndex + 4);
        if (elseIndex == -1) {
            // IF...THEN without ELSE
            return new IfThenStatement(keyword, condition, afterThen);
        }
        
        // IF...THEN...ELSE
        String thenPart = args.substring(thenIndex + 4, elseIndex).trim();
        String elsePart = args.substring(elseIndex + 4).trim();
        
        return new IfThenElseStatement(keyword, condition, thenPart, elsePart);
    }
    
    /**
     * Tokenize a single BASIC source line
     */
    public static ProgramLine tokenizeLine(String line) throws BasicSyntaxError {
        line = line.trim();
        if (line.isEmpty()) {
            throw new BasicSyntaxError("Empty line");
        }
        
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new BasicSyntaxError("Invalid line format: " + line);
        }
        
        int lineNumber = Integer.parseInt(matcher.group(1));
        String rest = matcher.group(2).trim();
        
        // Split the line into separate statements using ':' as a separator, but
        // honour the rule that everything after THEN on an IF-statement belongs
        // to that IF (colons inside that region are *not* top-level separators).
        List<String> statementParts = splitStatements(rest);
        List<Statement> statements = new ArrayList<>();
        
        for (int idx = 0; idx < statementParts.size(); idx++) {
            String part = statementParts.get(idx).trim();
            if (part.isEmpty()) continue;

            // If a REM appears, swallow the rest of the parts so that any ':'
            // inside the comment is preserved.
            if (part.toUpperCase().startsWith("REM")) {
                // Re-join remaining segments with ':'
                StringBuilder remBuilder = new StringBuilder(part);
                for (int j = idx + 1; j < statementParts.size(); j++) {
                    remBuilder.append(":").append(statementParts.get(j));
                }
                part = remBuilder.toString();
                idx = statementParts.size(); // exit loop after processing
            }

            Statement stmt = parseStatement(part);
            statements.add(stmt);
        }
        
        return new ProgramLine(lineNumber, statements, line);
    }
    
    /**
     * Parse a single statement from its text (public for use by Executor)
     */
    public static Statement parseStatement(String statementText) throws BasicSyntaxError {
        statementText = statementText.trim();
        if (statementText.isEmpty()) {
            throw new BasicSyntaxError("Empty statement");
        }
        
        // Find the first space to separate keyword from arguments
        // Handle special case where PRINT might be followed immediately by variable (e.g., PRINTF1)
        String keyword;
        String args;
        
        // Handle cases where keywords are followed immediately by arguments without spaces
        if (statementText.toUpperCase().startsWith("PRINT") && statementText.length() > 5) {
            char nextChar = statementText.charAt(5);
            if (nextChar != ' ' && nextChar != '\t') {
                // PRINT followed by arguments without space (e.g., PRINT"text" or PRINTF1)
                keyword = "PRINT";
                args = statementText.substring(5).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("NEXT") && statementText.length() > 4) {
            char nextChar = statementText.charAt(4);
            if (nextChar != ' ' && nextChar != '\t') {
                // NEXT followed by variable without space (e.g., NEXTI)
                keyword = "NEXT";
                args = statementText.substring(4).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("IF") && statementText.length() > 2) {
            char nextChar = statementText.charAt(2);
            if (nextChar != ' ' && nextChar != '\t') {
                // IF followed by condition without space (e.g., IFX>Y)
                keyword = "IF";
                args = statementText.substring(2).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("DIM") && statementText.length() > 3) {
            char nextChar = statementText.charAt(3);
            if (nextChar != ' ' && nextChar != '\t') {
                // DIM followed by array declaration without space (e.g., DIMA(2))
                keyword = "DIM";
                args = statementText.substring(3).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("FOR") && statementText.length() > 3) {
            char nextChar = statementText.charAt(3);
            if (nextChar != ' ' && nextChar != '\t') {
                // FOR followed by variable without space (e.g., FORI=1TO8)
                keyword = "FOR";
                args = statementText.substring(3).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("GOTO") && statementText.length() > 4) {
            char nextChar = statementText.charAt(4);
            if (nextChar != ' ' && nextChar != '\t') {
                // GOTO followed by line number without space (e.g., GOTO100)
                keyword = "GOTO";
                args = statementText.substring(4).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("GOSUB") && statementText.length() > 5) {
            char nextChar = statementText.charAt(5);
            if (nextChar != ' ' && nextChar != '\t') {
                // GOSUB followed by line number without space
                keyword = "GOSUB";
                args = statementText.substring(5).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("ON") && statementText.length() > 2) {
            char nextChar = statementText.charAt(2);
            if (nextChar != ' ' && nextChar != '\t') {
                // ON followed by expression without space (e.g., ONXGOTO100,200)
                keyword = "ON";
                args = statementText.substring(2).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("RETURN") && statementText.length() > 6) {
            char nextChar = statementText.charAt(6);
            if (nextChar != ' ' && nextChar != '\t') {
                // RETURN followed by variable without space (e.g., RETURNX)
                keyword = "RETURN";
                args = statementText.substring(6).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("THEN") && statementText.length() > 4) {
            char nextChar = statementText.charAt(4);
            if (nextChar != ' ' && nextChar != '\t') {
                // THEN followed by condition without space (e.g., THENX>Y)
                keyword = "THEN";
                args = statementText.substring(4).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("ELSE") && statementText.length() > 4) {
            char nextChar = statementText.charAt(4);
            if (nextChar != ' ' && nextChar != '\t') {
                // ELSE followed by condition without space (e.g., ELSEX>Y)
                keyword = "ELSE";
                args = statementText.substring(4).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("RESTORE") && statementText.length() > 7) {
            char nextChar = statementText.charAt(7);
            if (nextChar != ' ' && nextChar != '\t') {
                // RESTORE followed by variable without space (e.g., RESTOREX)
                keyword = "RESTORE";
                args = statementText.substring(7).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("CLEAR") && statementText.length() > 5) {
            char nextChar = statementText.charAt(5);
            if (nextChar != ' ' && nextChar != '\t') {
                // CLEAR followed by variable without space (e.g., CLEARX)
                keyword = "CLEAR";
                args = statementText.substring(5).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else if (statementText.toUpperCase().startsWith("INPUT") && statementText.length() > 5) {
            char nextChar = statementText.charAt(5);
            if (nextChar != ' ' && nextChar != '\t') {
                keyword = "INPUT";
                args = statementText.substring(5).trim();
            } else {
                int spaceIndex = statementText.indexOf(' ');
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        } else {
            int spaceIndex = statementText.indexOf(' ');
            if (spaceIndex == -1) {
                keyword = statementText.toUpperCase();
                args = "";
            } else {
                keyword = statementText.substring(0, spaceIndex).toUpperCase();
                args = statementText.substring(spaceIndex + 1).trim();
            }
        }
        
        // Handle special cases and create appropriate statement types
        return switch (keyword) {
            case "REM" -> new BasicStatement(keyword, args);
            case "PRINT" -> new PrintStatement(keyword, args);
            case "END" -> new BasicStatement(keyword, "");
            case "STOP" -> new BasicStatement(keyword, "");
            case "GOTO" -> new BasicStatement(keyword, args);
            case "GOSUB" -> new BasicStatement(keyword, args);
            case "RETURN" -> new BasicStatement(keyword, args);
            case "FOR" -> new ForStatement(keyword, args);
            case "NEXT" -> new BasicStatement(keyword, args);
            case "IF" -> parseIfStatement(keyword, args);
            case "THEN" -> new BasicStatement(keyword, args);
            case "ELSE" -> new BasicStatement(keyword, args);
            case "DIM" -> new DimStatement(keyword, args);
            case "DEF" -> new DefStatement(keyword, args);
            case "INPUT" -> new InputStatement(keyword, args);
            case "READ" -> new ReadStatement(keyword, args);
            case "DATA" -> new DataStatement(keyword, args);
            case "RESTORE" -> new BasicStatement(keyword, args);
            case "CLEAR" -> new BasicStatement(keyword, args);
            case "ON" -> new BasicStatement(keyword, args);
            default -> {
                // Check if it's an assignment (no keyword, just variable = expression)
                if (statementText.contains("=") && !statementText.contains("==") && !statementText.contains("<=") && !statementText.contains(">=")) {
                    String assignmentArgs = keyword.equals("LET") ? args : statementText;
                    yield new AssignmentStatement("LET", assignmentArgs);
                } else {
                    // Numeric-only statement treated as GOTO <number>
                    if (statementText.matches("\\d+")) {
                        yield new BasicStatement("GOTO", statementText);
                    } else {
                        yield new BasicStatement(keyword, args);
                    }
                }
            }
        };
    }
} 