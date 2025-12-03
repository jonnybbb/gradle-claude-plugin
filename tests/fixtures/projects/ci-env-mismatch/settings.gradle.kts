// Test Fixture: ci-env-mismatch
// Purpose: Simulate "works in CI but not locally" due to missing CI environment variable
// This fixture publishes build scans to Develocity for end-to-end testing
// Server URL is configurable via DEVELOCITY_SERVER environment variable

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "ci-env-mismatch"

val develocityServerUrl = providers.environmentVariable("DEVELOCITY_SERVER")
    .orElse("https://ge.gradle.org")
    .get()

develocity {
    server = develocityServerUrl
    buildScan {
        uploadInBackground = false
        publishing.onlyIf { true } // Always publish
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
