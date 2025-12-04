// Test Fixture: ci-env-mismatch
// Purpose: Simulate "works in CI but not locally" due to missing CI environment variable
// This fixture publishes build scans to Develocity for end-to-end testing
// Server URL is configurable via DEVELOCITY_SERVER environment variable

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "ci-env-mismatch"

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
        tag("ci-env-mismatch-fixture")

        // Tag CI vs LOCAL builds
        if (providers.environmentVariable("CI").orNull != null) {
            tag("CI")
        } else {
            tag("LOCAL")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
