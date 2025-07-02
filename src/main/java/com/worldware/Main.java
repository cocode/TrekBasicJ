package com.worldware;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Main program for running a BASIC program from the command line.
 */
public class Main {
    // Constants
    private static final String BASIC_FILE_EXTENSION = ".bas";
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_STOP = 1;
    private static final int EXIT_ERROR = 2;
    private static final String TRACE_FILE_NAME = "tracefile.txt";

    public static void main(String[] args) {
        try {
            Arguments arguments = parseArguments(args);
            
            // Find and load the program
            String programPath = findProgramFile(arguments.program);
            Program program = loadProgramWithErrorHandling(programPath);
            
            System.out.printf("Loaded program with %d lines%n", program.size());
            
            // Execute the program
            RunStatus runStatus;
            Executor executor;
            if (arguments.trace) {
                executor = new Executor(program, true);
            } else {
                executor = new Executor(program);
            }
            
            long startTime = arguments.time ? System.currentTimeMillis() : 0;
            
            try {
                runStatus = executor.runProgram();
            } catch (BasicSyntaxError syntaxError) {
                System.err.printf("%s in line %s of file.%n", syntaxError.getMessage(), syntaxError.getLineNumber());
                System.exit(EXIT_ERROR);
                return;
            } catch (BasicRuntimeError runtimeError) {
                System.err.printf("Runtime Error: %s%n", runtimeError.getMessage());
                System.exit(EXIT_ERROR);
                return;
            } finally {
                executor.close();
            }
            
            if (arguments.time) {
                long endTime = System.currentTimeMillis();
                double executionTime = (endTime - startTime) / 1000.0;
                System.out.printf("Execution time: %.5f seconds%n", executionTime);
            }
            
            // Display the program structure if requested
            if (arguments.symbols) {
                System.out.println("Symbol table:");
                System.out.println("Symbol count: " + executor.getSymbolCount());
                // TODO: Add symbol table dump functionality
            }
            
            System.out.println("Program completed with a status of " + runStatus);
            System.exit(determineExitCode(runStatus));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Parse command line arguments
     */
    private static Arguments parseArguments(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Main <program> [--trace] [--symbols] [--time]");
            System.exit(EXIT_ERROR);
        }
        
        Arguments arguments = new Arguments();
        arguments.program = args[0];
        
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--trace", "-t" -> arguments.trace = true;
                case "--symbols", "-s" -> arguments.symbols = true;
                case "--time" -> arguments.time = true;
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(EXIT_ERROR);
                }
            }
        }
        
        return arguments;
    }

    /**
     * Find the program file, adding .bas extension if needed
     */
    private static String findProgramFile(String programName) {
        if (Files.exists(Paths.get(programName))) {
            return programName;
        }
        
        String programWithExtension = programName + BASIC_FILE_EXTENSION;
        if (Files.exists(Paths.get(programWithExtension))) {
            return programWithExtension;
        }
        
        return programName; // Return original name to trigger FileNotFoundException
    }

    /**
     * Load a BASIC program with comprehensive error handling
     */
    private static Program loadProgramWithErrorHandling(String programPath) {
        try {
            return loadProgram(programPath);
        } catch (BasicSyntaxError syntaxError) {
            System.err.printf("%s in line %s of file.%n", syntaxError.getMessage(), syntaxError.getLineNumber());
            System.exit(EXIT_ERROR);
        } catch (IOException fileError) {
            System.err.printf("File not found %s%n", fileError.getMessage());
            System.exit(EXIT_ERROR);
        } catch (IllegalArgumentException valueError) {
            System.err.printf("Value Error %s%n", valueError.getMessage());
            System.exit(EXIT_ERROR);
        }
        return null; // Never reached
    }

    /**
     * Load a BASIC program from a file
     */
    private static Program loadProgram(String programPath) throws IOException, BasicSyntaxError {
        List<String> lines = Files.readAllLines(Paths.get(programPath));
        return BasicLoader.tokenize(lines);
    }

    /**
     * Determine the appropriate exit code based on run status
     */
    private static int determineExitCode(RunStatus runStatus) {
        return switch (runStatus) {
            case END_OF_PROGRAM, END_CMD -> EXIT_SUCCESS;
            case END_STOP -> EXIT_STOP;
            default -> EXIT_ERROR;
        };
    }

    /**
     * Simple argument holder class
     */
    private static class Arguments {
        String program;
        boolean trace = false;
        boolean symbols = false;
        boolean time = false;
    }
}