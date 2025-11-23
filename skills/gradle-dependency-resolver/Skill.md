---
name: gradle-dependency-resolver
description: Resolves dependency conflicts, analyzes transitive dependencies, and manages versions. Use for dependency conflicts or version management issues.
version: 1.0.0
---

# Gradle Dependency Resolver

Resolve dependency conflicts and manage versions effectively.

## When to Use

Invoke when users report:
- Dependency conflicts
- Version mismatches
- Transitive dependency issues
- "Could not resolve" errors
- Dependency management questions

## Analysis Commands

```bash
# View dependency tree
gradle dependencies --configuration compileClasspath

# Investigate specific dependency
gradle dependencyInsight --dependency <lib>
```

## Resolution Strategies

### 1. Dependency Constraints (Recommended)
```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:32.1.3-jre") {
            because("Version 31.x has CVE")
        }
    }
}
```

### 2. Force Version
```kotlin
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:32.1.3-jre")
}
```

### 3. Platform/BOM
```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.1.5"))
}
```

## Resolution Workflow

1. Identify conflict with `dependencies` task
2. Analyze with `dependencyInsight`
3. Choose resolution strategy
4. Apply fix in build.gradle[.kts]
5. Verify resolution
