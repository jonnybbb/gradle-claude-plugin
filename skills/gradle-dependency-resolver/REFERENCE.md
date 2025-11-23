# Gradle Dependency Resolver - Reference Guide

## Dependency Resolution Mechanism

### How Gradle Resolves Dependencies

1. **Collect Dependencies**: Gather all dependencies from configurations
2. **Build Dependency Graph**: Create graph of all direct and transitive dependencies
3. **Resolve Conflicts**: Apply conflict resolution strategy
4. **Download Artifacts**: Fetch from repositories
5. **Verify Integrity**: Check checksums and signatures

### Dependency Conflict Resolution

**Default Strategy:** Highest version wins

```
App depends on:
  - Library A → Guava 30.0
  - Library B → Guava 32.0
Result: Guava 32.0 is used
```

## Detailed Resolution Strategies

### 1. Dependency Constraints (Best Practice)

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:32.1.3-jre") {
            because("Version 31.x has CVE-2023-2976, upgrade all usages")
        }
        implementation("org.slf4j:slf4j-api:2.0.9") {
            because("Align all SLF4J versions")
        }
    }
}
```

**Advantages:**
- Declarative and clear
- Documents reasoning with `because()`
- Doesn't override explicit declarations
- Gradle 4.6+ feature

### 2. Force Versions

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-jre")
        force("org.slf4j:slf4j-api:2.0.9")

        // Fail on any version conflict
        failOnVersionConflict()

        // Prefer modules from this project
        preferProjectModules()
    }
}
```

**Advantages:**
- Simple and direct
- Applies to all configurations

**Disadvantages:**
- Overrides all version selections
- Less declarative than constraints
- Can hide issues

### 3. Platform Dependencies (BOM)

```kotlin
dependencies {
    // Import Spring Boot BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.1.5"))

    // Use versions from BOM (no version specified)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Override BOM version if needed
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
}
```

**Popular BOMs:**
- Spring Boot: `org.springframework.boot:spring-boot-dependencies`
- Micronaut: `io.micronaut:micronaut-bom`
- Quarkus: `io.quarkus:quarkus-bom`
- Jackson: `com.fasterxml.jackson:jackson-bom`
- JUnit: `org.junit:junit-bom`

### 4. Exclude Transitive Dependencies

```kotlin
dependencies {
    implementation("com.example:library:1.0.0") {
        // Exclude specific module
        exclude(group = "org.slf4j", module = "slf4j-log4j12")

        // Exclude entire group
        exclude(group = "commons-logging")

        // Disable all transitive dependencies
        isTransitive = false
    }
}
```

### 5. Dependency Substitution

```kotlin
configurations.all {
    resolutionStrategy.dependencySubstitution {
        // Replace module with another
        substitute(module("org.slf4j:slf4j-log4j12"))
            .using(module("org.slf4j:slf4j-simple:2.0.9"))
            .because("Standardize on slf4j-simple")

        // Replace with project dependency
        substitute(module("com.example:external-lib:1.0"))
            .using(project(":internal-lib"))
    }
}
```

## Common Dependency Conflicts

### 1. SLF4J Multiple Bindings

**Problem:**
```
SLF4J: Class path contains multiple SLF4J bindings:
  - slf4j-log4j12
  - logback-classic
  - slf4j-simple
```

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

### 2. Guava Android vs JRE

**Problem:**
```
Conflict between:
  - guava:32.1.3-android
  - guava:32.1.3-jre
```

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

### 3. Jackson Version Alignment

**Problem:** Different Jackson modules with different versions

**Solution using BOM:**
```kotlin
dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.15.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
```

### 4. Spring Boot Version Conflicts

**Problem:** Mixed Spring Boot versions

**Solution:**
```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.1.5"))

    // All Spring Boot starters aligned
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
}
```

### 5. Kotlin Standard Library Conflicts

**Problem:** Multiple kotlin-stdlib variants

**Solution:**
```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("1.9.20")
            because("Align all Kotlin stdlib versions")
        }
    }
}
```

## Dependency Analysis Commands

### View Dependency Tree

```bash
# All configurations
gradle dependencies

# Specific configuration
gradle dependencies --configuration compileClasspath
gradle dependencies --configuration runtimeClasspath
gradle dependencies --configuration testImplementation

# For specific module
gradle :app:dependencies --configuration compileClasspath

# Plain text output
gradle dependencies --console=plain > dependencies.txt
```

### Dependency Insight

```bash
# Why is this dependency included?
gradle dependencyInsight --dependency guava

# Specific configuration
gradle dependencyInsight --dependency slf4j-api --configuration compileClasspath

# For specific module
gradle :app:dependencyInsight --dependency spring-boot-starter
```

### Build Environment

```bash
# Classpath and plugin dependencies
gradle buildEnvironment
```

## Dependency Locking

### Enable Locking

```kotlin
// build.gradle.kts
dependencyLocking {
    lockAllConfigurations()
}
```

### Generate Lock Files

```bash
# Generate locks for all configurations
gradle dependencies --write-locks

# Update specific dependency
gradle dependencies --update-locks com.google.guava:guava

# Update all dependencies
gradle dependencies --update-locks
```

### Lock File Location

```
gradle.lockfile  # For all configurations
```

### Lock File Example

```
# Lock file contents
com.google.guava:guava:32.1.3-jre
org.slf4j:slf4j-api:2.0.9
org.junit.jupiter:junit-jupiter:5.10.1
```

## Version Catalogs

### Creating Version Catalog

```toml
# gradle/libs.versions.toml

[versions]
kotlin = "1.9.20"
spring-boot = "3.1.5"
junit = "5.10.1"
guava = "32.1.3-jre"

[libraries]
# Simple notation
guava = { module = "com.google.guava:guava", version.ref = "guava" }

# Detailed notation
spring-boot-starter-web = {
    module = "org.springframework.boot:spring-boot-starter-web",
    version.ref = "spring-boot"
}

# Without version (from BOM)
jackson-databind = { module = "com.fasterxml.jackson.core:jackson-databind" }

[bundles]
spring-boot-web = [
    "spring-boot-starter-web",
    "spring-boot-starter-validation",
    "spring-boot-starter-actuator"
]

testing = [
    "junit-jupiter",
    "mockito-core",
    "assertj-core"
]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

### Using Version Catalog

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

// build.gradle.kts
dependencies {
    // Single library
    implementation(libs.guava)

    // Bundle
    implementation(libs.bundles.spring.boot.web)
    testImplementation(libs.bundles.testing)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
}
```

## Repository Configuration

### Repository Priority

```kotlin
repositories {
    // Searched in order
    mavenLocal()          // 1. Local Maven cache
    mavenCentral()        // 2. Maven Central
    google()              // 3. Google's Maven repo
    gradlePluginPortal()  // 4. Gradle Plugin Portal

    // Custom repository
    maven {
        url = uri("https://repo.example.com/maven")
        credentials {
            username = providers.gradleProperty("repoUser").orNull
            password = providers.gradleProperty("repoPassword").orNull
        }
    }
}
```

### Content Filtering

```kotlin
repositories {
    mavenCentral {
        content {
            // Only use for specific groups
            includeGroup("org.springframework.boot")
            includeGroup("com.google.guava")

            // Exclude specific groups
            excludeGroup("com.example.internal")
        }
    }

    maven {
        url = uri("https://internal.repo.example.com/maven")
        content {
            // Only internal dependencies
            includeGroup("com.example.internal")
        }
    }
}
```

### Repository Credentials

```kotlin
repositories {
    maven {
        url = uri("https://repo.example.com/maven")
        credentials {
            username = providers.gradleProperty("mavenUsername").orNull
            password = providers.gradleProperty("mavenPassword").orNull
        }
    }
}
```

```properties
# gradle.properties (not committed to version control)
mavenUsername=user
mavenPassword=secret
```

## Dependency Verification

### Enable Verification

```bash
# Generate verification metadata
gradle --write-verification-metadata sha256 help

# Output: gradle/verification-metadata.xml
```

### Verification Metadata

```xml
<!-- gradle/verification-metadata.xml -->
<verification-metadata>
   <components>
      <component group="com.google.guava" name="guava" version="32.1.3-jre">
         <artifact name="guava-32.1.3-jre.jar">
            <sha256 value="..." />
         </artifact>
      </component>
   </components>
</verification-metadata>
```

## Transitive Dependency Management

### Understanding Transitive Dependencies

```
App
├── Library A
│   ├── Guava 30.0 (transitive)
│   └── SLF4J 1.7.x (transitive)
└── Library B
    ├── Guava 32.0 (transitive)
    └── SLF4J 2.0.x (transitive)
```

Gradle selects:
- Guava 32.0 (highest version)
- SLF4J 2.0.x (highest version)

### Viewing Transitive Dependencies

```bash
# Show all levels
gradle dependencies --configuration compileClasspath

# Find specific transitive dependency
gradle dependencyInsight --dependency guava --configuration compileClasspath
```

### Managing Transitive Dependencies

```kotlin
// Option 1: Exclude and add explicitly
dependencies {
    implementation("com.example:library:1.0") {
        exclude(group = "com.google.guava")
    }
    implementation("com.google.guava:guava:32.1.3-jre")
}

// Option 2: Use constraints
dependencies {
    implementation("com.example:library:1.0")

    constraints {
        implementation("com.google.guava:guava:32.1.3-jre")
    }
}

// Option 3: Disable transitivity
dependencies {
    implementation("com.example:library:1.0") {
        isTransitive = false
    }
    // Add required dependencies explicitly
}
```

## Best Practices

1. **Use Version Catalogs**: Centralize version management
2. **Prefer Constraints Over Force**: More declarative and maintainable
3. **Document with because()**: Explain why versions are forced/excluded
4. **Use Platforms/BOMs**: Align related dependencies
5. **Enable Dependency Locking**: Ensure reproducible builds
6. **Regular Updates**: Keep dependencies up-to-date
7. **Security Scanning**: Check for CVEs regularly
8. **Content Filtering**: Optimize repository searches
9. **Dependency Verification**: Validate artifact integrity
10. **Monitor Transitive Dependencies**: Understand what you're including
