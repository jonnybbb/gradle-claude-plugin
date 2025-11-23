# Gradle Project Analysis - Reference Guide

## Comprehensive Analysis Capabilities

### Multi-Module Project Detection

**Identifying Project Type:**
- Check for `settings.gradle` or `settings.gradle.kts`
- Parse included modules using `include()` statements
- Detect hierarchical vs flat module organization
- Identify composite builds with `includeBuild()`

**Module Relationship Mapping:**
```kotlin
// Flat structure
include(":app")
include(":lib-core")
include(":lib-utils")

// Hierarchical structure
include(":apps:web-app")
include(":apps:cli-app")
include(":libs:core")
include(":libs:utils")
```

### Build File Analysis (Groovy DSL)

**Groovy DSL Patterns:**
```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
}

dependencies {
    api 'com.google.guava:guava:32.1.3-jre'
    implementation 'org.slf4j:slf4j-api:2.0.9'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}

repositories {
    mavenCentral()
}
```

### Build File Analysis (Kotlin DSL)

**Kotlin DSL Patterns:**
```kotlin
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.google.guava:guava:32.1.3-jre")
    implementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

repositories {
    mavenCentral()
}
```

## Configuration Patterns Detection

### Version Catalogs (libs.versions.toml)

**Structure:**
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

**Usage in Build Files:**
```kotlin
dependencies {
    implementation(libs.guava)
    implementation(libs.bundles.spring)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
}
```

### Convention Plugins (buildSrc/)

**Directory Structure:**
```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    ├── java-library-conventions.gradle.kts
    ├── java-application-conventions.gradle.kts
    └── kotlin-library-conventions.gradle.kts
```

**Example Convention Plugin:**
```kotlin
// buildSrc/src/main/kotlin/java-library-conventions.gradle.kts
plugins {
    `java-library`
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### Settings Plugins

**Purpose:** Configure dependency resolution and plugin management
```kotlin
// settings.gradle.kts
plugins {
    id("com.gradle.enterprise") version "3.15.1"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

dependencyResolutionManagement {
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
```

## Plugin Discovery and Analysis

### Core Gradle Plugins

- `java` - Basic Java support
- `java-library` - Java library with API/implementation separation
- `application` - Java application with run task
- `groovy` - Groovy language support
- `scala` - Scala language support
- `war` - WAR file packaging
- `ear` - EAR file packaging

### Community Plugins

**Spring Boot:**
```kotlin
plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
}
```

**Kotlin:**
```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
}
```

**Android:**
```kotlin
plugins {
    id("com.android.application") version "8.1.0"
}
```

**Shadow (Fat JAR):**
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
```

## Dependency Analysis

### Configuration Types

**Java Library Configurations:**
- `api` - Exposed to consumers (transitive)
- `implementation` - Internal only (not transitive)
- `compileOnly` - Compile-time only (e.g., Lombok)
- `runtimeOnly` - Runtime only (e.g., JDBC drivers)
- `testImplementation` - Test compile and runtime
- `testCompileOnly` - Test compile only
- `testRuntimeOnly` - Test runtime only

**Android Configurations:**
- `debugImplementation` - Debug variant only
- `releaseImplementation` - Release variant only
- `androidTestImplementation` - Android instrumented tests

### Transitive Dependency Analysis

**Diamond Dependency Problem:**
```
App
├── Library A (depends on Guava 30.0)
└── Library B (depends on Guava 32.0)
```

**Resolution:** Gradle selects highest version (32.0) unless constrained

### Inter-Module Dependencies

**Project Dependencies:**
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":lib-core"))
    api(project(":lib-api"))  // Exposed to app's consumers
}
```

## Gradle Properties Analysis

### gradle.properties

**Performance Settings:**
```properties
# Build cache
org.gradle.caching=true

# Parallel execution
org.gradle.parallel=true
org.gradle.workers.max=8

# Daemon tuning
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Configuration cache
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

**Project Properties:**
```properties
# Project information
group=com.example
version=1.0.0

# Repository credentials
mavenUsername=user
mavenPassword=secret
```

## Wrapper Configuration Analysis

### gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Key Fields:**
- `distributionUrl` - Gradle version and distribution type (bin/all)
- `networkTimeout` - Download timeout
- Distribution type:
  - `bin` - Binary only (smaller, faster download)
  - `all` - Includes sources and docs (better IDE integration)

## Report Generation

### ASCII Project Tree

```
=== Project Structure ===
Root: my-awesome-app
├── :app (application)
│   ├── Plugins: application, org.springframework.boot
│   └── Dependencies: :lib-core, spring-boot-starter-web
├── :lib-core (java-library)
│   ├── Plugins: java-library
│   └── Dependencies: :lib-utils, guava
└── :lib-utils (java-library)
    ├── Plugins: java-library
    └── Dependencies: slf4j-api
```

### Dependency Graph

```
=== Dependency Map ===
:app
  → :lib-core (implementation)
  → spring-boot-starter-web:3.1.5 (implementation)

:lib-core
  → :lib-utils (api)
  → guava:32.1.3-jre (implementation)

:lib-utils
  → slf4j-api:2.0.9 (implementation)
```

### Configuration Summary

```
=== Configuration Analysis ===
✓ Version catalog: gradle/libs.versions.toml
✓ Convention plugins: buildSrc/
✓ Gradle wrapper: 8.5
✓ Build cache: enabled
⚠ Configuration cache: not enabled
⚠ Dependency locking: not enabled

Gradle Version: 8.5
Java Toolchain: 17
Modules: 3
Build Files: Kotlin DSL
```

## Recommendations Engine

### Based on Project Size

**Small Projects (1-5 modules):**
- Enable build cache for faster incremental builds
- Consider version catalog for dependency management
- Document plugin versions

**Medium Projects (5-20 modules):**
- Implement convention plugins in buildSrc/
- Enable parallel builds
- Use version catalogs
- Enable build cache and configuration cache

**Large Projects (20+ modules):**
- Convention plugins are essential
- Parallel builds critical
- Configuration cache highly recommended
- Consider composite builds for modularity
- Remote build cache for team collaboration
- Increase daemon heap size (8g+)

### Based on Configuration Patterns

**Missing Version Catalog:**
- Recommendation: Create gradle/libs.versions.toml
- Benefit: Centralized version management
- Migration effort: Low-Medium

**No Convention Plugins:**
- Recommendation: Extract common configuration to buildSrc/
- Benefit: DRY principle, easier maintenance
- Migration effort: Medium

**No Dependency Locking:**
- Recommendation: Enable dependency locking
- Benefit: Reproducible builds
- Migration effort: Low
- Command: `gradle dependencies --write-locks`

### Based on Gradle Version

**Gradle 6.x:**
- Recommendation: Upgrade to 7.x first, then 8.x
- Breaking changes to address
- Test incrementally

**Gradle 7.x:**
- Recommendation: Upgrade to 8.x
- Configuration cache available
- Update deprecated APIs

**Gradle 8.x:**
- Recommendation: Already on modern version
- Ensure all plugins compatible
- Enable configuration cache

## Diagnostic Commands

### Project Structure
```bash
# List all projects
gradle projects

# List tasks for all projects
gradle tasks --all
```

### Build Files
```bash
# Find all build files
find . -name "build.gradle" -o -name "build.gradle.kts"

# Find settings files
find . -name "settings.gradle" -o -name "settings.gradle.kts"
```

### Plugin Information
```bash
# List plugins with versions
gradle buildEnvironment

# Validate plugins
gradle validatePlugins
```

### Dependencies
```bash
# All dependencies
gradle dependencies

# Specific configuration
gradle dependencies --configuration compileClasspath

# Dependency tree for module
gradle :app:dependencies
```

## Common Anti-Patterns

### 1. Duplicate Configuration
**Problem:** Same configuration repeated in multiple modules
**Solution:** Use convention plugins in buildSrc/

### 2. Hardcoded Versions
**Problem:** Version numbers scattered across build files
**Solution:** Use version catalog or ext properties

### 3. Circular Dependencies
**Problem:** Module A depends on B, B depends on A
**Solution:** Extract shared code to common module

### 4. Deep Module Hierarchies
**Problem:** More than 3-4 levels of module nesting
**Solution:** Flatten structure or use composite builds

### 5. Mixed DSL
**Problem:** Some modules use Kotlin DSL, others Groovy
**Solution:** Standardize on one DSL (prefer Kotlin)

## Integration with Other Tools

### IDE Integration
- IntelliJ IDEA: Import Gradle project, sync
- Eclipse: Use Buildship Gradle plugin
- VS Code: Use Gradle extension

### CI/CD Integration
- Use Gradle wrapper for consistency
- Enable build cache for faster CI builds
- Use `--no-daemon` in CI environments
- Generate build scans for analysis

### Build Scans
```bash
gradle build --scan
```
Provides detailed insights into build performance and configuration
