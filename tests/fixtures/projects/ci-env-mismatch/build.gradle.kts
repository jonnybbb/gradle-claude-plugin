// Test Fixture: ci-env-mismatch
// Purpose: Build that behaves differently based on CI environment variable
// Scenario: A validation task that requires CI=true to pass

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

// Tag the build scan with CI or LOCAL based on environment
develocity {
    buildScan {
        val isCI = providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false)
        tag(if (isCI.get()) "CI" else "LOCAL")

        // Capture environment info for debugging
        value("CI_ENV", providers.environmentVariable("CI").orElse("not set").get())
        value("user.name", providers.systemProperty("user.name").orElse("unknown").get())
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
