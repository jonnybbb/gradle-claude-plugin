# Settings File Configuration

**Source**: https://docs.gradle.org/current/userguide/settings_file_basics.html  
**Gradle Version**: 7.0+

## Overview

The settings file (`settings.gradle.kts` or `settings.gradle`) configures the build structure and declares subprojects.

## Basic Structure

```kotlin
// settings.gradle.kts
rootProject.name = "my-application"

include("app")
include("lib")
include("common")
```

## Including Projects

### Simple Include

```kotlin
include("app")
include("lib")
```

### Nested Projects

```kotlin
include("backend:api")
include("backend:core")
include("frontend:web")
```

### Multiple at Once

```kotlin
include(
    "app",
    "lib",
    "common"
)
```

## Project Paths

```kotlin
// Custom project directory
include("lib")
project(":lib").projectDir = file("libraries/lib")
```

## Repositories

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

## Plugin Management

```kotlin
pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.21"
    }
    
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "my-plugin") {
                useModule("com.example:my-plugin:1.0")
            }
        }
    }
}
```

## Build Cache

```kotlin
buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = System.getenv("CI") != null
    }
}
```

## Version Catalogs

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
```

## Composite Builds

```kotlin
includeBuild("../library")
includeBuild("../common")
```

## Complete Example

```kotlin
// settings.gradle.kts
rootProject.name = "my-application"

// Plugin repositories
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency repositories
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
    
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

// Build cache
buildCache {
    local {
        isEnabled = true
    }
}

// Subprojects
include("app")
include("lib")
include("common")
```

## Related Documentation

- [Multi-Project](multi-project.md): Multi-project builds
- [Composite Builds](composite-builds.md): Composite builds
- [Build Cache](build-cache.md): Cache configuration

## Quick Reference

```kotlin
// Minimal
rootProject.name = "my-app"
include("app", "lib")

// Complete
pluginManagement {
    repositories { gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

buildCache {
    local { isEnabled = true }
}

include("app", "lib")
```
