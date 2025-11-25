// =============================================================================
// Example: Multi-Project Build - settings.gradle.kts
// =============================================================================
// Demonstrates a well-structured multi-project Gradle build.
// =============================================================================

rootProject.name = "my-application"

// =============================================================================
// Plugin Management
// =============================================================================

pluginManagement {
    // Custom plugin repositories
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Add company repository if needed
        // maven("https://artifacts.company.com/maven")
    }
    
    // Include local plugin builds (composite build)
    // includeBuild("../gradle-plugins")
}

// =============================================================================
// Dependency Resolution
// =============================================================================

dependencyResolutionManagement {
    // Prefer settings-level repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        mavenCentral()
        // google() // For Android projects
    }
    
    // Version catalog is automatically loaded from gradle/libs.versions.toml
}

// =============================================================================
// Feature Previews
// =============================================================================

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// Enables: projects.app, projects.core.api instead of project(":app")

// =============================================================================
// Project Structure
// =============================================================================

// Root modules
include("app")
include("core")
include("common")

// Nested modules under 'services'
include("services:user-service")
include("services:order-service")
include("services:notification-service")

// Nested modules under 'libs'
include("libs:database")
include("libs:messaging")
include("libs:security")

// =============================================================================
// Custom Project Directories (optional)
// =============================================================================

// If a module lives in a non-standard location:
// project(":legacy-module").projectDir = file("legacy/old-module")

// =============================================================================
// Build Cache Configuration
// =============================================================================

buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 7
    }
    
    // Remote cache (for CI/team sharing)
    // remote<HttpBuildCache> {
    //     url = uri("https://cache.company.com/cache/")
    //     isPush = System.getenv("CI") != null
    //     credentials {
    //         username = System.getenv("CACHE_USER") ?: ""
    //         password = System.getenv("CACHE_PASSWORD") ?: ""
    //     }
    // }
}

// =============================================================================
// Resulting Structure:
// =============================================================================
//
// my-application/
// ├── settings.gradle.kts      (this file)
// ├── build.gradle.kts         (root build - optional)
// ├── gradle.properties
// ├── gradle/
// │   ├── libs.versions.toml
// │   └── wrapper/
// ├── buildSrc/                (convention plugins)
// │   ├── build.gradle.kts
// │   └── src/main/kotlin/
// │       └── java-conventions.gradle.kts
// ├── app/
// │   ├── build.gradle.kts
// │   └── src/
// ├── core/
// │   ├── build.gradle.kts
// │   └── src/
// ├── common/
// │   ├── build.gradle.kts
// │   └── src/
// ├── services/
// │   ├── user-service/
// │   │   ├── build.gradle.kts
// │   │   └── src/
// │   ├── order-service/
// │   │   ├── build.gradle.kts
// │   │   └── src/
// │   └── notification-service/
// │       ├── build.gradle.kts
// │       └── src/
// └── libs/
//     ├── database/
//     │   ├── build.gradle.kts
//     │   └── src/
//     ├── messaging/
//     │   ├── build.gradle.kts
//     │   └── src/
//     └── security/
//         ├── build.gradle.kts
//         └── src/
// =============================================================================
