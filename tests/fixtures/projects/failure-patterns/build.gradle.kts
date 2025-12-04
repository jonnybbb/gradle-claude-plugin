// Test Fixture: failure-patterns
// Purpose: Demonstrate various failure patterns for build scan analysis
// Scenarios:
//   - FAILURE_TYPE=compilation - Compilation error
//   - FAILURE_TYPE=test - Test assertion failure
//   - FAILURE_TYPE=dependency - Dependency resolution failure
//   - FAILURE_TYPE=resource - Resource exhaustion (OOM simulation)
//   - FAILURE_TYPE=none (default) - Clean build

plugins {
    java
    application
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    mainClass.set("com.example.Main")
}

val failureType = providers.environmentVariable("FAILURE_TYPE").orElse("none").get()

// Tag the build scan with failure type
develocity {
    buildScan {
        tag("FAILURE_PATTERNS_FIXTURE")
        value("failureType", failureType)
    }
}

dependencies {
    // Base dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Conditional: Add a non-existent dependency to cause resolution failure
    if (failureType == "dependency") {
        implementation("com.example.nonexistent:fake-library:99.99.99")
    }
}

// Task to generate compilable or broken source file
tasks.register("generateSources") {
    val outputDir = layout.buildDirectory.dir("generated/sources/java/main")
    val failType = failureType

    outputs.dir(outputDir)

    doLast {
        val generatedDir = outputDir.get().asFile.resolve("com/example/generated")
        generatedDir.mkdirs()

        val sourceContent = if (failType == "compilation") {
            // Invalid Java - missing semicolon and invalid syntax
            """
            package com.example.generated;

            public class GeneratedCode {
                public void brokenMethod() {
                    String message = "This will not compile"  // Missing semicolon
                    int x = "not a number";  // Type mismatch
                }
            }
            """.trimIndent()
        } else {
            // Valid Java
            """
            package com.example.generated;

            public class GeneratedCode {
                public void workingMethod() {
                    String message = "This compiles fine";
                    System.out.println(message);
                }
            }
            """.trimIndent()
        }

        generatedDir.resolve("GeneratedCode.java").writeText(sourceContent)
    }
}

// Add generated sources to compilation
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/java/main"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("generateSources")
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // Pass failure type to tests
    systemProperty("FAILURE_TYPE", failureType)

    // Configure resource constraints for OOM test
    if (failureType == "resource") {
        // Very low heap to trigger OOM
        jvmArgs("-Xmx16m", "-Xms16m")
    }
}
