# Multi-Project Build Organization

**Source**: https://docs.gradle.org/current/userguide/multi_project_builds.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Multi-project builds organize related projects into a single build tree, enabling code reuse, dependency management, and unified build execution.

## Basic Structure

```
my-application/
├── settings.gradle.kts       # Declares subprojects
├── build.gradle.kts          # Root project configuration
├── app/                      # Application module
│   └── build.gradle.kts
├── lib/                      # Library module
│   └── build.gradle.kts
└── common/                   # Shared module
    └── build.gradle.kts
```

## Settings File

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-application"

include("app")
include("lib")
include("common")

// Nested projects
include("backend:api")
include("backend:core")
```

**Groovy:**
```groovy
rootProject.name = 'my-application'

include 'app'
include 'lib'
include 'common'
include 'backend:api', 'backend:core'
```

## Root Project Configuration

**build.gradle.kts:**
```kotlin
plugins {
    id("java") version "8.5" apply false
}

allprojects {
    group = "com.example"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    
    dependencies {
        testImplementation("junit:junit:4.13.2")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
}
```

## Project Dependencies

**app/build.gradle.kts:**
```kotlin
plugins {
    application
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":common"))
}

application {
    mainClass.set("com.example.Main")
}
```

## Convention Plugins

**buildSrc/src/main/kotlin/java-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}
```

**Apply in subprojects:**
```kotlin
plugins {
    id("java-conventions")
}
```

## Dependency Management

### Version Catalogs

**gradle/libs.versions.toml:**
```toml
[versions]
junit = "5.10.0"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
```

**Use in projects:**
```kotlin
dependencies {
    testImplementation(libs.junit.jupiter)
}
```

### Platform Projects

**platform/build.gradle.kts:**
```kotlin
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("com.google.guava:guava:31.1-jre")
        api("org.slf4j:slf4j-api:2.0.9")
    }
}
```

**Consume platform:**
```kotlin
dependencies {
    implementation(platform(project(":platform")))
    implementation("com.google.guava:guava")  // Version from platform
}
```

## Execution Patterns

### Run Specific Project

```bash
./gradlew :app:build
./gradlew :lib:test
```

### Run All Projects

```bash
./gradlew build      # All projects
./gradlew clean      # All projects
```

### Parallel Execution

**gradle.properties:**
```properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

## Project Access

### From Root to Subprojects

```kotlin
// build.gradle.kts (root)
subprojects {
    if (name == "app") {
        // Configure app project
    }
}

project(":app") {
    // Direct configuration
}
```

### From Subproject to Subproject

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":lib"))
}
```

## Shared Configuration

### allprojects

```kotlin
allprojects {
    repositories {
        mavenCentral()
    }
}
```

### subprojects

```kotlin
subprojects {
    apply(plugin = "java-library")
    
    dependencies {
        testImplementation("junit:junit:4.13.2")
    }
}
```

### Conditional Configuration

```kotlin
subprojects {
    plugins.withType<JavaPlugin> {
        // Only for projects with Java plugin
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
}
```

## Best Practices

1. **Use convention plugins** instead of allprojects/subprojects
2. **Avoid cross-project configuration** (task dependencies)
3. **Use version catalogs** for dependency management
4. **Enable parallel execution**
5. **Use project isolation** (avoid direct project references in tasks)

## Related Documentation

- [Composite Builds](composite-builds.md): Multiple builds working together
- [buildSrc](buildSrc.md): Convention plugins
- [Settings File](settings-gradle.md): Settings configuration
- [Dependency Management](dependency-management.md): Dependencies

## Quick Reference

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("app", "lib", "common")

// build.gradle.kts (root)
subprojects {
    apply(plugin = "java-library")
    
    repositories {
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation(project(":lib"))
    implementation(project(":common"))
}
```
