// Test Fixture: flaky-tests
// Purpose: Tests that fail intermittently due to random behavior
// This fixture publishes build scans to Develocity for flaky test detection testing
// Server URL is configurable via DEVELOCITY_SERVER environment variable

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "flaky-tests"

val develocityServerUrl = providers.environmentVariable("DEVELOCITY_SERVER")
    .getOrElse(
        throw GradleException("DEVELOCITY_SERVER environment variable is required")
    )

develocity {
    server = develocityServerUrl
    buildScan {
        uploadInBackground = false
        publishing.onlyIf { true } // Always publish

        // Add tags from environment for test filtering
        val testTag = providers.environmentVariable("E2E_TEST_TAG").orNull
        if (testTag != null) {
            tag(testTag)
        }
        tag("flaky-tests-fixture")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
