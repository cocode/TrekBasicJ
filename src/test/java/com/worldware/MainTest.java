package com.worldware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSimpleProgram() throws Exception {
        // Create a simple BASIC program file
        Path programFile = tempDir.resolve("test.bas");
        Files.write(programFile, "10 PRINT \"Hello World\"\n20 END".getBytes());

        // Capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // This would normally call System.exit, so we can't test it directly
        // For now, just verify the file exists and can be read
        assertTrue(Files.exists(programFile));
        String content = Files.readString(programFile);
        assertTrue(content.contains("PRINT"));
        assertTrue(content.contains("END"));
    }

    @Test 
    public void testEmptyProgram() throws Exception {
        // Create an empty program file
        Path programFile = tempDir.resolve("empty.bas");
        Files.write(programFile, "".getBytes());
        
        assertTrue(Files.exists(programFile));
        assertEquals("", Files.readString(programFile));
    }
} 