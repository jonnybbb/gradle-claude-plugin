// Test Fixture: build-cache-analytics
// Purpose: Demonstrate build cache behavior for analytics testing
// This fixture publishes build scans to Develocity for cache performance analysis
// Server URL is configurable via DEVELOCITY_SERVER environment variable

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

rootProject.name = "build-cache-analytics"

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
        tag("build-cache-analytics-fixture")

        // Tag the cache scenario
        val cacheScenario = providers.environmentVariable("CACHE_SCENARIO").orElse("default").get()
        tag("cache-scenario-$cacheScenario")
    }
}

// Enable build cache
buildCache {
    local {
        // Use separate cache directories based on CACHE_SCENARIO
        val cacheScenario = providers.environmentVariable("CACHE_SCENARIO").orElse("default").get()
        directory = file("build-cache-$cacheScenario")
        isEnabled = true
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
