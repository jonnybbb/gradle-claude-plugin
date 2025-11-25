# Dependency Management

**Source**: https://docs.gradle.org/current/userguide/dependency_management.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Gradle's dependency management resolves, downloads, and makes dependencies available to your build.

## Declaring Dependencies

**Kotlin DSL:**
```kotlin
dependencies {
    // Implementation (not exposed to consumers)
    implementation("com.google.guava:guava:31.1-jre")
    
    // API (exposed to consumers)
    api("org.slf4j:slf4j-api:2.0.9")
    
    // Compile only (not at runtime)
    compileOnly("org.projectlombok:lombok:1.18.30")
    
    // Runtime only
    runtimeOnly("com.h2database:h2:2.2.224")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

**Groovy DSL:**
```groovy
dependencies {
    implementation 'com.google.guava:guava:31.1-jre'
    api 'org.slf4j:slf4j-api:2.0.9'
    compileOnly 'org.projectlombok:lombok:1.18.30'
    runtimeOnly 'com.h2database:h2:2.2.224'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

## Configuration Types

### Java Plugin Configurations

- **implementation**: Private dependencies
- **api**: Public dependencies (java-library only)
- **compileOnly**: Compile-time only
- **runtimeOnly**: Runtime only
- **testImplementation**: Test dependencies
- **annotationProcessor**: Annotation processors

### Custom Configurations

```kotlin
val integrationTest by configurations.creating {
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    integrationTest("org.testcontainers:testcontainers:1.19.3")
}
```

## Dependency Notation

### Module Dependencies

```kotlin
// Standard notation
implementation("group:artifact:version")

// Map notation
implementation(group = "com.google.guava", name = "guava", version = "31.1-jre")

// Classifier
implementation("org.example:library:1.0:tests")

// Extension
implementation("org.example:library:1.0@aar")
```

### Project Dependencies

```kotlin
dependencies {
    implementation(project(":lib"))
    implementation(project(":common"))
}
```

### File Dependencies

```kotlin
dependencies {
    implementation(files("libs/custom.jar"))
    implementation(fileTree("libs") {
        include("**/*.jar")
    })
}
```

## Version Management

### Version Catalogs

**gradle/libs.versions.toml:**
```toml
[versions]
guava = "31.1-jre"
junit = "5.10.0"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }

[bundles]
testing = ["junit-jupiter"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = "1.9.21" }
```

**Usage:**
```kotlin
dependencies {
    implementation(libs.guava)
    testImplementation(libs.bundles.testing)
}
```

### Dependency Constraints

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                require("[28.0-jre, 32.0-jre[")
                prefer("31.1-jre")
            }
            because("security updates")
        }
    }
}
```

### Dependency Locking

```kotlin
dependencyLocking {
    lockAllConfigurations()
}
```

```bash
# Generate locks
./gradlew dependencies --write-locks

# Update locks
./gradlew dependencies --update-locks com.google.guava:guava
```

## Transitive Dependencies

### Excluding Transitives

```kotlin
dependencies {
    implementation("com.example:library:1.0") {
        exclude(group = "org.unwanted", module = "unwanted-lib")
    }
}
```

### Force Versions

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-jre")
    }
}
```

### Fail on Conflict

```kotlin
configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
    }
}
```

## Repositories

```kotlin
repositories {
    mavenCentral()
    google()
    mavenLocal()
    
    maven {
        url = uri("https://repo.example.com/maven2")
        credentials {
            username = "user"
            password = "password"
        }
    }
}
```

## Related Documentation

- [Version Catalogs](version-catalogs.md): Centralized version management
- [Conflict Resolution](conflict-resolution.md): Resolving conflicts
- [Dependency Constraints](dependency-constraints.md): Advanced constraints

## Quick Reference

```kotlin
dependencies {
    implementation("group:artifact:version")
    api(project(":lib"))
    testImplementation(libs.junit.jupiter)
}

// Version catalog (gradle/libs.versions.toml)
[libraries]
guava = "com.google.guava:guava:31.1-jre"
```
