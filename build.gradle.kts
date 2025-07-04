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
    // Print System.out/System.err from tests unconditionally so we can inspect debug output
    testLogging {
        showStandardStreams = true
    }
}

// Configure run task to accept program arguments
tasks.named<JavaExec>("run") {
    // Forward stdin so interactive BASIC programs can read user input instead of immediately seeing EOF
    standardInput = System.`in`
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split("\\s+").toList()
    }
}

// ---- BASIC interpreter integration test task: delegates to Java runner ----

tasks.register<JavaExec>("runTestSuite") {
    group = "verification"
    description = "Runs all .bas programs in test_suite using TestSuiteRunner"
    dependsOn("classes")

    mainClass.set("com.worldware.TestSuiteRunner")
    classpath = sourceSets["main"].runtimeClasspath
}