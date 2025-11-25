# Dependency Conflict Resolution

Strategies for resolving version conflicts.

## Understanding Conflicts

Conflicts occur when different dependencies require different versions of the same library.

```
my-app
├── library-a:1.0 → guava:30.0
└── library-b:1.0 → guava:31.0
     ↑ CONFLICT: which guava version?
```

## Default Resolution

Gradle uses **highest version wins**:

```
guava:30.0 vs guava:31.0 → guava:31.0 selected
```

## Resolution Strategies

### Force Version

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

### Prefer Project Modules

```kotlin
configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}
```

## Dependency Constraints (Preferred)

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre") {
            because("Security fix in 31.1")
        }
    }
}
```

## Rich Version Constraints

```kotlin
dependencies {
    implementation("com.google.guava:guava") {
        version {
            strictly("[30.0, 32.0[")  // Range
            prefer("31.1-jre")         // Preferred within range
            reject("31.0-jre")         // Excluded version
        }
    }
}
```

## Exclude Transitives

```kotlin
// Exclude from specific dependency
dependencies {
    implementation("com.example:library:1.0") {
        exclude(group = "org.unwanted", module = "unwanted-lib")
    }
}

// Global exclusion
configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}
```

## Dependency Substitution

```kotlin
configurations.all {
    resolutionStrategy.dependencySubstitution {
        // Replace module with another
        substitute(module("org.slf4j:slf4j-log4j12"))
            .using(module("org.slf4j:slf4j-simple:2.0.9"))
        
        // Replace with project
        substitute(module("com.example:my-lib"))
            .using(project(":my-lib"))
    }
}
```

## Platform/BOM Usage

```kotlin
dependencies {
    // Import platform (recommendations)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Enforce platform (overrides transitives)
    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Use without version
    implementation("org.springframework:spring-core")
}
```

## Diagnosing Conflicts

### View Dependencies

```bash
./gradlew dependencies --configuration compileClasspath
```

### Why This Version?

```bash
./gradlew dependencyInsight --dependency guava --configuration compileClasspath
```

### Build Scan

```bash
./gradlew build --scan
# Check Dependencies section
```

## Common Conflict Scenarios

### SLF4J Multiple Bindings

```kotlin
configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "log4j", module = "log4j")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
}
```

### Jakarta vs Javax

```kotlin
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("javax.servlet:javax.servlet-api"))
            .using(module("jakarta.servlet:jakarta.servlet-api:6.0.0"))
    }
}
```

### Different Artifact Names

```kotlin
// When same classes in different artifacts
configurations.all {
    resolutionStrategy.capabilitiesResolution {
        withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:0")
        }
    }
}
```

## Best Practices

1. Use **version catalogs** for consistency
2. Use **platforms/BOMs** for version alignment
3. Use **constraints** over force when possible
4. **Document** version choices with `because()`
5. Use **failOnVersionConflict()** in strict projects
6. Review dependencies with **build scans** regularly
