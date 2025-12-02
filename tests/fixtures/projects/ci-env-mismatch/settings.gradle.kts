// Test Fixture: ci-env-mismatch
// Purpose: Simulate "works in CI but not locally" due to missing CI environment variable
// This fixture publishes build scans to ge.gradle.org for end-to-end testing

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "ci-env-mismatch"

develocity {
    server = "https://ge.gradle.org"
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
