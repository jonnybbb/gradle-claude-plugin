---
name: gradle-dependency-resolver
description: Resolves Gradle dependency conflicts, analyzes transitive dependencies, manages version constraints, and optimizes dependency configurations. Claude uses this when you ask about dependency conflicts, version mismatches, transitive dependencies, or dependency resolution errors.
---

# Gradle Dependency Resolver Skill

This skill enables Claude to analyze, troubleshoot, and resolve complex dependency issues in Gradle projects.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Report "dependency conflict" or "version conflict"
- Ask about "transitive dependencies"
- Encounter "Could not resolve dependency" errors
- Request "dependency tree" or "dependency graph"
- Ask to "fix version mismatch"
- Inquire about "dependency resolution strategy"
- Want to "upgrade dependencies" or "update versions"

## Capabilities

### 1. Dependency Conflict Detection

**Identify Conflicts:**
```bash
# View dependency tree with conflicts highlighted
gradle dependencies --configuration compileClasspath

# Find specific dependency paths
gradle dependencyInsight --dependency guava --configuration compileClasspath
```

**Common Conflict Scenarios:**
- **Diamond Problem**: Multiple paths to same dependency with different versions
- **Version Alignment**: Related dependencies requiring same version
- **Platform Conflicts**: Incompatible platform/BOM versions

### 2. Version Conflict Resolution

**Kotlin DSL:**
```kotlin
dependencies {
    // Force specific version
    implementation("com.google.guava:guava:32.1.3-jre") {
        because("version 31.x has security vulnerabilities")
    }

    // Exclude transitive dependency
    implementation("com.example:library:1.0.0") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }

    // Strictly enforce version
    implementation("org.slf4j:slf4j-api") {
        version {
            strictly("2.0.9")
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Force versions across configuration
        force("com.google.guava:guava:32.1.3-jre")

        // Fail on version conflict
        failOnVersionConflict()

        // Prefer specific versions
        preferProjectModules()
    }
}
```

**Groovy DSL:**
```groovy
dependencies {
    implementation('com.google.guava:guava:32.1.3-jre') {
        because 'version 31.x has security vulnerabilities'
    }

    implementation('com.example:library:1.0.0') {
        exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    }
}

configurations.all {
    resolutionStrategy {
        force 'com.google.guava:guava:32.1.3-jre'
        failOnVersionConflict()
    }
}
```

### 3. Dependency Constraints

**Platform Dependencies:**
```kotlin
dependencies {
    // Import BOM (Bill of Materials)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.1.5"))

    // Use versions from BOM
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Override BOM version if needed
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
}
```

**Dependency Constraints:**
```kotlin
dependencies {
    constraints {
        implementation("org.slf4j:slf4j-api:2.0.9") {
            because("CVE-2021-42550 affects older versions")
        }
    }
}
```

### 4. Version Catalogs

**libs.versions.toml:**
```toml
[versions]
kotlin = "1.9.20"
spring-boot = "3.1.5"
guava = "32.1.3-jre"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }

[bundles]
spring = ["spring-boot-starter", "spring-boot-actuator"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

**Using Version Catalog:**
```kotlin
dependencies {
    implementation(libs.guava)
    implementation(libs.bundles.spring)
}
```

### 5. Transitive Dependency Analysis

**Dependency Insight:**
```bash
# Find why a dependency is included
gradle dependencyInsight --dependency slf4j-api

# Show all paths to dependency
gradle dependencyInsight --dependency guava --configuration runtimeClasspath
```

**Managing Transitive Dependencies:**
```kotlin
dependencies {
    // Transitive dependencies excluded
    implementation("com.example:library:1.0.0") {
        isTransitive = false
    }

    // Specific exclusions
    implementation("org.hibernate:hibernate-core:6.3.1") {
        exclude(group = "org.jboss.logging")
    }
}
```

### 6. Dependency Locking

**Enable Locking:**
```kotlin
// build.gradle.kts
dependencyLocking {
    lockAllConfigurations()
}
```

**Generate Lock Files:**
```bash
# Generate lockfiles
gradle dependencies --write-locks

# Update specific dependency
gradle dependencies --update-locks org.example:library

# Verify locked dependencies
gradle buildEnvironment --write-verification-metadata sha256
```

### 7. Repository Configuration

**Repository Priority:**
```kotlin
repositories {
    // Order matters - searched in sequence
    mavenLocal()
    mavenCentral()
    google()
    maven {
        url = uri("https://repo.spring.io/milestone")
        content {
            includeGroup("org.springframework.boot")
        }
    }
}
```

**Content Filtering:**
```kotlin
repositories {
    mavenCentral {
        content {
            excludeGroup("com.example.internal")
        }
    }

    maven {
        url = uri("https://internal.repo.example.com/maven")
        credentials {
            username = providers.gradleProperty("repoUser").orNull
            password = providers.gradleProperty("repoPassword").orNull
        }
        content {
            includeGroup("com.example.internal")
        }
    }
}
```

## Dependency Resolution Workflow

### Step 1: Identify Conflicts

```bash
gradle dependencies --configuration compileClasspath > deps.txt
# Review deps.txt for conflicts marked with (*)
```

### Step 2: Analyze Conflict Paths

```bash
gradle dependencyInsight --dependency <conflicted-lib> --configuration compileClasspath
```

### Step 3: Choose Resolution Strategy

**Option A: Force Version**
```kotlin
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:32.1.3-jre")
}
```

**Option B: Use Platform/BOM**
```kotlin
dependencies {
    implementation(platform("com.google.guava:guava-bom:32.1.3-jre"))
}
```

**Option C: Exclude Transitive**
```kotlin
dependencies {
    implementation("com.example:lib:1.0") {
        exclude(group = "com.google.guava")
    }
}
```

### Step 4: Verify Resolution

```bash
gradle dependencies --configuration compileClasspath
# Confirm no conflicts remain
```

## Helper Scripts

**scripts/detect-conflicts.sh:**
```bash
#!/bin/bash
PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Dependency Conflict Detection ==="

gradle dependencies --configuration compileClasspath | grep -E "\(\*\)" || echo "No conflicts detected"
```

**scripts/generate-graph.sh:**
```bash
#!/bin/bash
PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Dependency Graph Generation ==="
gradle dependencies --configuration compileClasspath --console=plain > dependency-graph.txt
echo "Graph saved to dependency-graph.txt"
```

## Common Issues

### Issue 1: SLF4J Multiple Bindings

**Problem:** Multiple SLF4J implementations on classpath

**Solution:**
```kotlin
configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "org.slf4j", module = "slf4j-simple")
}
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
}
```

### Issue 2: Guava Version Conflicts

**Problem:** Android and JRE Guava variants conflict

**Solution:**
```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion("32.1.3-jre")
            because("Standardize on JRE variant")
        }
    }
}
```

### Issue 3: Spring Boot Version Alignment

**Problem:** Spring Boot libraries have mismatched versions

**Solution:**
```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.1.5"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // All Spring Boot dependencies aligned to 3.1.5
}
```

## Best Practices

1. Use version catalogs for centralized version management
2. Prefer platforms/BOMs over forced versions
3. Document exclusions with `because()` clauses
4. Enable dependency locking for reproducible builds
5. Use `dependencyInsight` to understand resolution
6. Avoid forced versions when possible (use constraints instead)
7. Keep dependencies up-to-date with security patches
8. Use content filtering for multi-repo setups
9. Leverage dependency verification for security
10. Regular dependency audits with `dependencyUpdates` plugin
