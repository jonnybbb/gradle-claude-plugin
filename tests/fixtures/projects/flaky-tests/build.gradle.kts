// Test Fixture: flaky-tests
// Purpose: Tests that fail intermittently due to random behavior
// Scenario: Tests use random values that sometimes cause assertion failures

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

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Tag the build scan with flakiness info
develocity {
    buildScan {
        val runId = providers.environmentVariable("RUN_ID").orElse("local").get()
        value("runId", runId)
        tag("FLAKY_TEST_FIXTURE")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Configure test retries to detect flakiness
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}
