plugins {
    id("java")
    id("application")
}

group = "com.worldware"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

application {
    mainClass.set("com.worldware.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

// Ensure clean build by making run task depend on clean
tasks.named("run") {
    dependsOn("clean", "classes")
}

// Configure run task to accept program arguments
tasks.named<JavaExec>("run") {
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split("\\s+").toList()
    }
}

// ---- BASIC interpreter integration test task ----
val testSuiteDir = file("test_suite")

tasks.register("runTestSuite") {
    group = "verification"
    description = "Runs all .bas programs found in test_suite directory with the Java interpreter"
    dependsOn("classes")

    doLast {
        if (!testSuiteDir.exists()) {
            println("No test_suite directory found – skipping.")
            return@doLast
        }
        val failures = mutableListOf<String>()
        val basFiles = testSuiteDir.walk().filter { it.isFile && it.extension.equals("bas", true) }.sortedBy { it.name }.toList()
        if (basFiles.isEmpty()) {
            println("No .bas files found in test_suite.")
            return@doLast
        }
        basFiles.forEach { prog ->
            println("\n>>> Running ${'$'}{prog.relativeTo(projectDir)}")
            val result = javaexec {
                mainClass.set("com.worldware.Main")
                classpath = sourceSets["main"].runtimeClasspath
                args = listOf(prog.absolutePath)
                // show interpreter output
            }
            if (result.exitValue != 0) {
                failures.add(prog.name)
            }
        }
        if (failures.isNotEmpty()) {
            throw GradleException("Interpreter failed on ${'$'}{failures.size} program(s): ${'$'}{failures.joinToString()}")
        } else {
            println("\nAll programs executed successfully.")
        }
    }
}