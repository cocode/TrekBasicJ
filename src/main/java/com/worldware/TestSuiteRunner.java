package com.worldware;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stand-alone runner that executes every .bas file in the test_suite directory
 * using the interpreter (com.worldware.Main) in a separate JVM. The first line
 * may contain a comment of the form "REM EXPECT_EXIT_CODE=n" to specify the
 * expected exit status (default 0). The program reports any mismatches and
 * exits with status 1 if failures are found.
 */
public class TestSuiteRunner {
    private static final Pattern EXPECT_PAT =
            Pattern.compile("EXPECT_EXIT_CODE\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws Exception {
        File projectDir = new File(System.getProperty("user.dir"));
        File suiteDir = new File(projectDir, "test_suite");
        if (!suiteDir.exists()) {
            System.err.println("No test_suite directory found");
            System.exit(1);
        }

        List<File> basFiles = new ArrayList<>();
        Files.walk(suiteDir.toPath())
                .filter(p -> p.toFile().isFile() && p.toString().toLowerCase().endsWith(".bas"))
                .sorted()
                .forEach(p -> basFiles.add(p.toFile()));

        if (basFiles.isEmpty()) {
            System.out.println("No .bas files found in test_suite");
            return;
        }

        List<String> failures = new ArrayList<>();
        for (File prog : basFiles) {
            System.out.println("\n>>> Running " + suiteDir.toPath().relativize(prog.toPath()));

            int expected = readExpectedExitCode(prog);
            int actual = runProgram(prog);

            if (actual != expected) {
                failures.add(prog.getName() + " (expected " + expected + " got " + actual + ")");
            }
        }

        if (failures.isEmpty()) {
            System.out.println("\nAll programs executed successfully.");
        } else {
            System.err.println("\nFailures:\n" + String.join("\n", failures));
            System.exit(1);
        }
    }

    private static int readExpectedExitCode(File file) {
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String first = br.readLine();
            if (first != null) {
                Matcher m = EXPECT_PAT.matcher(first);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception ignored) {}
        return 0; // default
    }

    private static int runProgram(File prog) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add("com.worldware.Main");
        cmd.add(prog.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        proc.waitFor();
        return proc.exitValue();
    }
}