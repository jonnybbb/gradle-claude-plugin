# Dependency Conflict Resolution

**Source**: https://docs.gradle.org/current/userguide/dependency_resolution.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

When multiple versions of the same dependency are requested, Gradle must resolve which version to use.

## Default Resolution Strategy

Gradle uses **highest version** by default:

```
Project depends on:
  - lib-a:1.0 → commons:1.0
  - lib-b:2.0 → commons:2.0

Result: commons:2.0 (highest version wins)
```

## Viewing Conflicts

```bash
# Show dependency tree
./gradlew dependencies

# Show conflicts only
./gradlew dependencies --configuration compileClasspath | grep -i conflict

# Generate report
./gradlew dependencyInsight --dependency guava
```

## Resolution Strategies

### 1. Force Version

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-jre")
    }
}
```

### 2. Fail on Conflict

```kotlin
configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
    }
}
```

### 3. Prefer Project Version

```kotlin
configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}
```

### 4. Version Constraints

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                strictly("[28.0-jre, 32.0-jre[")
                prefer("31.1-jre")
            }
        }
    }
}
```

### 5. Exclude Transitive Dependencies

```kotlin
dependencies {
    implementation("com.example:library:1.0") {
        exclude(group = "org.unwanted", module = "unwanted")
    }
}

// Exclude globally
configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}
```

### 6. Dependency Substitution

```kotlin
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.slf4j:slf4j-log4j12"))
            .using(module("org.slf4j:slf4j-api:2.0.9"))
    }
}
```

## Version Selection

### Rich Versions

```kotlin
dependencies {
    implementation("com.google.guava:guava") {
        version {
            strictly("[29.0-jre, 32.0-jre[")  // Range
            prefer("31.1-jre")                // Preferred
            reject("30.0-jre")                // Exclude specific
        }
    }
}
```

### Platform Dependencies

```kotlin
dependencies {
    // Import BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Use without version
    implementation("org.springframework:spring-core")  // Version from BOM
}
```

## Capability Conflicts

When multiple artifacts provide same capability:

```kotlin
configurations.all {
    resolutionStrategy.capabilitiesResolution {
        withCapability("com.google.guava:guava") {
            selectHighestVersion()
        }
    }
}
```

## Common Conflict Scenarios

### Scenario 1: Different Major Versions

```
- App → lib-a:1.0 → guava:30.1-jre
- App → lib-b:2.0 → guava:31.1-jre

Solution: Force or constrain to compatible version
```

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre")
    }
}
```

### Scenario 2: SLF4J Multiple Bindings

```
- Multiple SLF4J implementations on classpath
- logback-classic, slf4j-simple, slf4j-log4j12

Solution: Exclude unwanted bindings
```

```kotlin
configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
}
```

### Scenario 3: Different Artifacts, Same Classes

```
- servlet-api vs javax.servlet-api
- Both contain javax.servlet classes

Solution: Exclude older artifact
```

```kotlin
dependencies {
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    
    configurations.all {
        exclude(group = "javax.servlet", module = "servlet-api")
    }
}
```

## Debugging Conflicts

### Dependency Insight

```bash
# Why is this version selected?
./gradlew dependencyInsight --dependency guava --configuration compileClasspath

# Output shows:
# - Selected version
# - Requested versions
# - Conflict resolution reason
```

### Build Scan Analysis

```bash
./gradlew build --scan

# In build scan:
# - Navigate to Dependencies
# - View resolution results
# - See conflict explanations
```

### Resolution Result API

```kotlin
configurations.compileClasspath.get().incoming.resolutionResult.allComponents {
    if (moduleVersion?.version != requestedVersion) {
        println("${moduleVersion?.module} requested ${requestedVersion} but got ${moduleVersion?.version}")
    }
}
```

## Best Practices

1. **Use version catalogs** for consistent versions
2. **Use platforms/BOMs** for version alignment
3. **Fail on conflict** in strict projects
4. **Document forced versions** with `because`
5. **Review dependencies** regularly

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-jre")
        because("Security vulnerability in older versions")
    }
}
```

## Version Alignment

### Gradle Platform

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.slf4j:slf4j-api:2.0.9")
        api("org.slf4j:slf4j-simple:2.0.9")
    }
}
```

### Consume Platform

```kotlin
dependencies {
    implementation(platform(project(":platform")))
    implementation("org.slf4j:slf4j-api")  // Version from platform
}
```

## Troubleshooting

### Multiple Versions Present

```bash
# Find all versions of a dependency
./gradlew dependencies | grep -i "guava"

# See why each is included
./gradlew dependencyInsight --dependency guava
```

### NoSuchMethodError at Runtime

Usually caused by wrong version:

```bash
# Find actual version on classpath
./gradlew dependencies --configuration runtimeClasspath | grep library

# Force correct version
```

```kotlin
configurations.all {
    resolutionStrategy.force("com.example:library:2.0")
}
```

## Related Documentation

- [Dependency Management](dependency-management.md): Basics
- [Dependency Constraints](dependency-constraints.md): Advanced constraints
- [Version Catalogs](version-catalogs.md): Centralized versions

## Quick Reference

```kotlin
// Force version
configurations.all {
    resolutionStrategy.force("group:artifact:version")
}

// Fail on conflict
configurations.all {
    resolutionStrategy.failOnVersionConflict()
}

// Exclude transitive
dependencies {
    implementation("group:artifact:version") {
        exclude(group = "unwanted", module = "unwanted")
    }
}

// Version constraint
dependencies {
    constraints {
        implementation("group:artifact") {
            version { prefer("1.0") }
        }
    }
}

// Check conflicts
./gradlew dependencyInsight --dependency artifact
```
