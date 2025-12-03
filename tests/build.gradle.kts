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

    // Gradle TestKit for running Gradle builds in tests
    testImplementation(gradleTestKit())

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
        // Exclude AI and Develocity tests by default - they require API keys
        // Run AI tests explicitly with: ./gradlew aiTests
        // Run Develocity E2E tests with: ./gradlew develocityE2ETests
        excludeTags("ai", "develocity")
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
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("tools")
    }
}

// Task to run only AI-powered tests (needs API key)
// Usage: ./gradlew aiTests
tasks.register<Test>("aiTests") {
    group = "verification"
    description = "Run AI-powered tests that require ANTHROPIC_API_KEY"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

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
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("skills")
    }
}

// Task to run hook tests
tasks.register<Test>("hookTests") {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("hooks")
    }
}

// Task to run Develocity E2E tests (requires DEVELOCITY_ACCESS_KEY and ANTHROPIC_API_KEY)
// Usage: ./gradlew develocityE2ETests
tasks.register<Test>("develocityE2ETests") {
    group = "verification"
    description = "Run Develocity end-to-end tests that require DEVELOCITY_ACCESS_KEY and ANTHROPIC_API_KEY"

    // Wire up the test source set (required for custom Test tasks)
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("develocity")
    }

    // Pass API keys and server config to tests (from local.env or system env)
    environment("DEVELOCITY_ACCESS_KEY", localEnv["DEVELOCITY_ACCESS_KEY"] ?: System.getenv("DEVELOCITY_ACCESS_KEY") ?: "")
    environment("ANTHROPIC_API_KEY", localEnv["ANTHROPIC_API_KEY"] ?: System.getenv("ANTHROPIC_API_KEY") ?: "")
    environment("DEVELOCITY_SERVER", localEnv["DEVELOCITY_SERVER"] ?: System.getenv("DEVELOCITY_SERVER") ?: "https://ge.gradle.org")

    // Long timeout for E2E tests (build scan indexing can take time)
    systemProperty("junit.jupiter.execution.timeout.default", "10m")

    // Run E2E tests sequentially
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed", "standard_out", "standard_error")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
