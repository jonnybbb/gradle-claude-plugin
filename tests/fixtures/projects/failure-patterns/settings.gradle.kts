// Test Fixture: failure-patterns
// Purpose: Demonstrate various failure patterns for build scan analysis
// This fixture publishes build scans to Develocity for failure pattern testing
// Server URL is configurable via DEVELOCITY_SERVER environment variable

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "failure-patterns"

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
        tag("failure-patterns-fixture")

        // Tag the failure type
        val failureType = providers.environmentVariable("FAILURE_TYPE").orElse("none").get()
        tag("failure-type-$failureType")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
