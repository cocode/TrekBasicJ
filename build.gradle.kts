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