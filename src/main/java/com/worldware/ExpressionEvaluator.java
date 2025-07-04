package com.worldware;

import java.util.*;
import java.util.function.Function;

/**
 * Evaluates BASIC expressions using a small recursive-descent parser.
 * Supported features:
 *   • Numeric & string literals
 *   • Variables & array access
 *   • Built-in functions (INT, RND, ABS …) – maintained in a map not switch/case
 *   • User-defined DEF FN functions
 *   • Arithmetic (+,-,*,/,^) with correct precedence & unary +/-
 *   • Comparisons (=, <>, <, >, <=, >=)
 *   • Boolean AND / OR
 *   • Parentheses for grouping
 */
public class ExpressionEvaluator {

    /* --------------------------------------------------------------------- */
    /*  Construction                                                         */
    /* --------------------------------------------------------------------- */

    private final Map<String, Object> symbols;
    private final Map<String, DefStatement> userFunctions;

    public ExpressionEvaluator(Map<String, Object> symbols) {
        this(symbols, new HashMap<>());
    }

    public ExpressionEvaluator(Map<String, Object> symbols, Map<String, DefStatement> userFunctions) {
        this.symbols = symbols;
        this.userFunctions = userFunctions != null ? userFunctions : new HashMap<>();
    }

    /* --------------------------------------------------------------------- */
    /*  Public API                                                           */
    /* --------------------------------------------------------------------- */

    public Object evaluate(String expression) {
        Parser p = new Parser(expression);
        return p.parseExpression();
    }

    public boolean evaluateCondition(String condition) {
        Object v = evaluate(condition);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0.0;
        if (v instanceof String s) return !s.isEmpty();
        return false;
    }

    /* --------------------------------------------------------------------- */
    /*  Lexer                                                                */
    /* --------------------------------------------------------------------- */

    private enum TokType {NUMBER, STRING, IDENT, OP, LPAREN, RPAREN, COMMA, EOF}

    private record Token(TokType type, String text) {
    }

    private static class Lexer {
        private final String src;
        private int idx = 0;

        Lexer(String src) {
            this.src = src.trim();
        }

        Token next() {
            skipWS();
            if (idx >= src.length()) return new Token(TokType.EOF, "");
            char ch = src.charAt(idx);
            // String literal
            if (ch == '"') {
                StringBuilder sb = new StringBuilder();
                idx++; // skip opening
                while (idx < src.length() && src.charAt(idx) != '"') {
                    sb.append(src.charAt(idx++));
                }
                if (idx < src.length()) idx++; // closing quote
                return new Token(TokType.STRING, sb.toString());
            }
            // Number literal (integer/float) possibly starting with sign handled in parser not lexer
            if (Character.isDigit(ch) || (ch == '.' && idx + 1 < src.length() && Character.isDigit(src.charAt(idx + 1)))) {
                int start = idx;
                while (idx < src.length() && (Character.isDigit(src.charAt(idx)) || src.charAt(idx) == '.')) idx++;
                return new Token(TokType.NUMBER, src.substring(start, idx));
            }
            // Identifier (letters/digits/$)
            if (Character.isLetter(ch)) {
                // Generic keyword detection: if the remaining characters start with any keyword,
                // emit that keyword token and continue scanning from the character *after* it.
                final String[] TEXT_OPERATORS = {"AND", "OR"};
                String upperRest = src.substring(idx).toUpperCase();
                for (String op : TEXT_OPERATORS) {
                    if (upperRest.startsWith(op)) {
                        idx += op.length();
                        return new Token(TokType.IDENT, op);
                    }
                }
                int start = idx;
                while (idx < src.length()) {
                    char c = src.charAt(idx);
                    if (Character.isLetterOrDigit(c) || c == '$') idx++; else break;
                }
                return new Token(TokType.IDENT, src.substring(start, idx));
            }
            // Operators and punctuation

            // Handle two-character comparison operators: <= >= <>
            if (ch == '<' || ch == '>') {
                if (idx + 1 < src.length()) {
                    char n = src.charAt(idx + 1);
                    if (n == '=' || (ch == '<' && n == '>')) {
                        idx += 2; // consume both
                        return new Token(TokType.OP, "" + ch + n);
                    }
                }
                idx++; // single char < or >
                return new Token(TokType.OP, String.valueOf(ch));
            }

            // Single-character tokens & operators
            idx++;
            return switch (ch) {
                case '+', '-', '*', '/', '^', '=' -> new Token(TokType.OP, String.valueOf(ch));
                case '(', '[' -> new Token(TokType.LPAREN, "(");
                case ')', ']' -> new Token(TokType.RPAREN, ")");
                case ',' -> new Token(TokType.COMMA, ",");
                default -> new Token(TokType.OP, String.valueOf(ch));
            };
        }

        private void skipWS() {
            while (idx < src.length() && Character.isWhitespace(src.charAt(idx))) idx++;
        }
    }

    /**
     * Utility for test/diagnostic code: returns the raw token stream produced by the
     * lexer without performing any evaluation or symbol look-ups.
     */
    public static java.util.List<String> tokenize(String expression) {
        Lexer lx = new Lexer(expression);
        java.util.List<String> tokens = new java.util.ArrayList<>();
        Token t;
        while ((t = lx.next()).type != TokType.EOF) {
            tokens.add(t.type + ":" + t.text);
        }
        return tokens;
    }

    /* --------------------------------------------------------------------- */
    /*  Parser                                                               */
    /* --------------------------------------------------------------------- */

    private class Parser {
        private final Lexer lexer;
        private Token look;

        Parser(String src) {
            this.lexer = new Lexer(src);
            this.look = lexer.next();
        }

        private void consume() {
            look = lexer.next();
        }

        private boolean accept(TokType type, String text) {
            if (look.type == type && (text == null || look.text.equalsIgnoreCase(text))) {
                consume();
                return true;
            }
            return false;
        }

        Object parseExpression() { // OR
            Object left = parseAnd();
            while (look.type == TokType.IDENT && look.text.equalsIgnoreCase("OR")) {
                consume();
                Object right = parseAnd();
                left = (toBoolean(left) || toBoolean(right)) ? 1 : 0;
            }
            return left;
        }

        private Object parseAnd() {
            Object left = parseComparison();
            while (look.type == TokType.IDENT && look.text.equalsIgnoreCase("AND")) {
                consume();
                Object right = parseComparison();
                left = (toBoolean(left) && toBoolean(right)) ? 1 : 0;
            }
            return left;
        }

        private Object parseComparison() {
            Object left = parseAdd();
            while (look.type == TokType.OP && ("=<>".indexOf(look.text.charAt(0)) >= 0)) {
                String op = look.text;
                consume();
                // handle <= >= <>
                if ((op.equals("<") || op.equals(">")) && look.type == TokType.OP && look.text.equals("=")) {
                    op += "=";
                    consume();
                } else if (op.equals("<") && look.type == TokType.OP && look.text.equals(">")) {
                    op = "<>";
                    consume();
                }
                Object right = parseAdd();
                left = compareValues(left, right, op);
            }
            return left;
        }

        private Object parseAdd() {
            Object left = parseMul();
            while (look.type == TokType.OP && (look.text.equals("+") || look.text.equals("-"))) {
                String op = look.text;
                consume();
                Object right = parseMul();
                left = performArithmetic(left, right, op);
            }
            return left;
        }

        private Object parseMul() {
            Object left = parsePow();
            while (look.type == TokType.OP && (look.text.equals("*") || look.text.equals("/"))) {
                String op = look.text;
                consume();
                Object right = parsePow();
                left = performArithmetic(left, right, op);
            }
            return left;
        }

        private Object parsePow() {
            Object left = parseUnary();
            while (look.type == TokType.OP && look.text.equals("^")) {
                consume();
                Object right = parseUnary();
                left = performArithmetic(left, right, "^");
            }
            return left;
        }

        private Object parseUnary() {
            if (look.type == TokType.OP && (look.text.equals("+") || look.text.equals("-"))) {
                String op = look.text;
                consume();
                Object val = parseUnary();
                if (op.equals("-")) return performArithmetic(0.0, val, "-");
                return val;
            }
            return parsePrimary();
        }

        private Object parsePrimary() {
            // number
            if (look.type == TokType.NUMBER) {
                String txt = look.text;
                consume();
                return txt.contains(".") ? Double.parseDouble(txt) : Integer.parseInt(txt);
            }
            // string
            if (look.type == TokType.STRING) {
                String s = look.text;
                consume();
                return s;
            }
            // identifier (var, function call, array)
            if (look.type == TokType.IDENT) {
                String name = look.text.toUpperCase();
                consume();
                if (accept(TokType.LPAREN, null)) { // function or array
                    List<Object> args = new ArrayList<>();
                    if (!accept(TokType.RPAREN, null)) {
                        do {
                            args.add(parseExpression());
                        } while (accept(TokType.COMMA, null));
                        expect(TokType.RPAREN);
                    }
                    // function?
                    if (isFunction(name)) {
                        return callFunction(name, args);
                    }
                    // array access
                    return arrayAccess(name, args);
                }
                // Validate variable name syntax (1 letter optionally followed by digit and/or $)
                if (!name.matches("(?i)[A-Z](\\d)?\\$?")) {
                    throw new RuntimeException("Invalid variable name: " + name);
                }
                // simple variable – must be defined
                Object val = symbols.get(name);
                if (val == null) {
                    throw new RuntimeException("Undefined variable: " + name);
                }
                return val;
            }
            // parentheses
            if (accept(TokType.LPAREN, null)) {
                Object val = parseExpression();
                expect(TokType.RPAREN);
                return val;
            }
            return 0;
        }

        private void expect(TokType type) {
            if (look.type != type) throw new RuntimeException("Expected " + type + " but found " + look.type);
            consume();
        }

        /* ------------------------------------------------------------- */
        private boolean toBoolean(Object o) {
            if (o instanceof Boolean b) return b;
            if (o instanceof Number n) return n.doubleValue() != 0.0;
            if (o instanceof String s) return !s.isEmpty();
            return false;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helpers                                                              */
    /* --------------------------------------------------------------------- */

    private boolean compareValues(Object left, Object right, String op) {
        // numeric compare if both numbers else string compare
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            return switch (op) {
                case "=" -> l == r;
                case "<>" -> l != r;
                case "<" -> l < r;
                case ">" -> l > r;
                case "<=" -> l <= r;
                case ">=" -> l >= r;
                default -> false;
            };
        }
        String ls = left.toString();
        String rs = right.toString();
        int cmp = ls.compareTo(rs);
        return switch (op) {
            case "=" -> cmp == 0;
            case "<>" -> cmp != 0;
            case "<" -> cmp < 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case ">=" -> cmp >= 0;
            default -> false;
        };
    }

    private Object performArithmetic(Object l, Object r, String op) {
        // string concatenation for +
        if (op.equals("+") && (l instanceof String || r instanceof String)) {
            return l.toString() + r.toString();
        }
        double ld = toNumber(l);
        double rd = toNumber(r);
        double res = switch (op) {
            case "+" -> ld + rd;
            case "-" -> ld - rd;
            case "*" -> ld * rd;
            case "/" -> rd != 0 ? ld / rd : 0;
            case "^" -> Math.pow(ld, rd);
            default -> 0;
        };
        // If the result is mathematically an integer and operation was not division, return Integer
        if (!op.equals("/") && res == Math.rint(res)) {
            return (int) res;
        }
        return res;
    }

    private double toNumber(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try {return Double.parseDouble(obj.toString());} catch (Exception e) {return 0;}
    }

    /* --------------------------------------------------------------------- */
    /*  Function handling                                                    */
    /* --------------------------------------------------------------------- */

    private interface Builtin extends Function<List<Object>, Object> {}

    private final Map<String, Builtin> builtins = initBuiltins();

    private Map<String, Builtin> initBuiltins() {
        Map<String, Builtin> m = new HashMap<>();
        m.put("INT", args -> (int)Math.floor(toNumber(args.get(0))));
        m.put("RND", args -> {
            double a = toNumber(args.get(0));
            return a <= 0 ? Math.random() : Math.random()*a;
        });
        m.put("SGN", args -> {
            double v = toNumber(args.get(0));
            return v > 0 ? 1 : (v < 0 ? -1 : 0);
        });
        m.put("EXP", args -> Math.exp(toNumber(args.get(0))));
        m.put("LOG", args -> {
            double v = toNumber(args.get(0));
            if (v <= 0) throw new RuntimeException("LOG of non-positive");
            return Math.log(v);
        });
        m.put("SIN", args -> Math.sin(toNumber(args.get(0))));
        m.put("COS", args -> Math.cos(toNumber(args.get(0))));
        m.put("TAN", args -> Math.tan(toNumber(args.get(0))));
        m.put("ATN", args -> Math.atan(toNumber(args.get(0))));
        m.put("SQR", args -> {
            double v = toNumber(args.get(0));
            if (v < 0) throw new RuntimeException("SQR of negative");
            return Math.sqrt(v);
        });
        m.put("ABS", args -> Math.abs(toNumber(args.get(0))));
        // string fns
        m.put("LEFT$", args -> {
            String s = args.get(0).toString();
            int len = (int)toNumber(args.get(1));
            len = Math.max(0, Math.min(len, s.length()));
            return s.substring(0, len);
        });
        m.put("RIGHT$", args -> {
            String s = args.get(0).toString();
            int len = (int)toNumber(args.get(1));
            len = Math.max(0, Math.min(len, s.length()));
            return s.substring(s.length()-len);
        });
        m.put("MID$", args -> {
            String s = args.get(0).toString();
            int start = (int)toNumber(args.get(1)) - 1; // BASIC 1-based
            start = Math.max(0, Math.min(start, s.length()));
            if (args.size()==2) return s.substring(start);
            int len = (int)toNumber(args.get(2));
            len = Math.max(0, Math.min(len, s.length()-start));
            return s.substring(start, start+len);
        });
        m.put("LEN", args -> args.get(0).toString().length());
        m.put("STR$", args -> {
            double v = toNumber(args.get(0));
            String s;
            if (v == Math.rint(v)) {
                s = String.valueOf((int) v);
            } else {
                s = String.valueOf(v);
            }
            if (v >= 0) s = " " + s; // leading space for positive numbers
            return s;
        });
        return m;
    }

    private boolean isFunction(String name) {
        return builtins.containsKey(name) || userFunctions.containsKey(name);
    }

    private Object callFunction(String name, List<Object> args) {
        if (builtins.containsKey(name)) {
            return builtins.get(name).apply(args);
        }
        // user function
        DefStatement def = userFunctions.get(name);
        if (def == null) throw new RuntimeException("Undefined function " + name);
        if (args.size()!=1) throw new RuntimeException("Function "+name+" expects 1 arg");
        Map<String,Object> temp = new HashMap<>(symbols);
        temp.put(def.getParameterName(), args.get(0));
        ExpressionEvaluator sub = new ExpressionEvaluator(temp, userFunctions);
        return sub.evaluate(def.getExpression());
    }

    /* --------------------------------------------------------------------- */
    /*  Array handling                                                       */
    /* --------------------------------------------------------------------- */

    private Object arrayAccess(String name, List<Object> indices) {
        Object arr = symbols.get("ARRAY:"+name);
        if (arr == null) return 0;
        Object current = arr;
        for (Object idxObj : indices) {
            int idx = (int) toNumber(idxObj) - Dialect.ARRAY_OFFSET;
            if (current instanceof Object[] array) {
                if (idx<0 || idx>=array.length) return 0;
                current = array[idx];
            } else {
                return 0;
            }
        }
        return current;
    }
} 