plugins {
    java
}

group = "com.gradle.claude.plugin"
version = "1.0.0"

// Load environment variables from local.env if it exists
val localEnvFile = file("local.env")
val localEnv = mutableMapOf<String, String>()
if (localEnvFile.exists()) {
    localEnvFile.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
        .forEach { line ->
            val (key, value) = line.split("=", limit = 2)
            localEnv[key.trim()] = value.trim()
        }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
}

dependencies {
    // Testing framework
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Anthropic SDK for AI-powered tests
    testImplementation("com.anthropic:anthropic-java:2.11.1")

    // Gradle Tooling API for tool tests
    testImplementation("org.gradle:gradle-tooling-api:9.2.1")

    // JSON handling
    testImplementation("com.google.code.gson:gson:2.10.1")

    // YAML parsing for skill/agent frontmatter
    testImplementation("org.yaml:snakeyaml:2.2")

    // Logging
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(1)
    useJUnitPlatform {
        // Exclude AI tests by default - they require API key and may hit rate limits
        // Run AI tests explicitly with: ./gradlew aiTests
        excludeTags("ai")
    }

    // Enable parallel test execution
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Task to run only tool tests (no API key needed)
tasks.register<Test>("toolTests") {
    useJUnitPlatform {
        includeTags("tools")
    }
}

// Task to run only AI-powered tests (needs API key)
// Usage: ./gradlew aiTests
tasks.register<Test>("aiTests") {
    group = "verification"
    description = "Run AI-powered tests that require ANTHROPIC_API_KEY"

    useJUnitPlatform {
        includeTags("ai")
    }

    // Pass API key to tests (from local.env or system env)
    environment("ANTHROPIC_API_KEY", localEnv["ANTHROPIC_API_KEY"] ?: System.getenv("ANTHROPIC_API_KEY") ?: "")

    // Increase timeout for AI tests
    systemProperty("junit.jupiter.execution.timeout.default", "120s")

    // Run AI tests sequentially to avoid rate limits
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Task to run skill quality tests
tasks.register<Test>("skillTests") {
    useJUnitPlatform {
        includeTags("skills")
    }
}

// Task to run hook tests
tasks.register<Test>("hookTests") {
    useJUnitPlatform {
        includeTags("hooks")
    }
}
