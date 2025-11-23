---
name: gradle-build-structuring
description: Organizes multi-module Gradle projects, implements build conventions, structures composite builds, and optimizes project layout. Claude uses this when you ask about project organization, multi-module setup, or build structure optimization.
---

# Gradle Build Structuring Skill

This skill enables Claude to organize and structure Gradle projects effectively, from single-module to complex multi-module builds with shared conventions.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask about "project structure" or "how to organize modules"
- Want to "create multi-module project"
- Need "build conventions" or "shared configuration"
- Request "composite build" setup
- Ask about "project layout" or "module organization"

## Project Structures

### 1. Single-Module Project

```
my-project/
├── gradle/
│   └── libs.versions.toml
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── gradle.properties
```

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-project"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
```

### 2. Multi-Module Project (Flat Structure)

```
my-project/
├── app/
│   ├── src/
│   └── build.gradle.kts
├── lib-core/
│   ├── src/
│   └── build.gradle.kts
├── lib-utils/
│   ├── src/
│   └── build.gradle.kts
├── buildSrc/
│   ├── src/main/kotlin/
│   │   ├── java-conventions.gradle.kts
│   │   └── kotlin-conventions.gradle.kts
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts (root)
├── settings.gradle.kts
└── gradle.properties
```

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-project"

include(":app")
include(":lib-core")
include(":lib-utils")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

**Root build.gradle.kts:**
```kotlin
plugins {
    id("java-conventions") apply false
}

allprojects {
    group = "com.example"
    version = "1.0.0"
}

subprojects {
    repositories {
        mavenCentral()
    }
}
```

**Module Dependencies:**
```kotlin
// app/build.gradle.kts
plugins {
    id("java-conventions")
    application
}

dependencies {
    implementation(project(":lib-core"))
}

// lib-core/build.gradle.kts
plugins {
    id("java-conventions")
}

dependencies {
    api(project(":lib-utils"))
}
```

### 3. Multi-Module Project (Hierarchical Structure)

```
my-project/
├── apps/
│   ├── web-app/
│   │   └── build.gradle.kts
│   └── cli-app/
│       └── build.gradle.kts
├── libs/
│   ├── core/
│   │   └── build.gradle.kts
│   ├── utils/
│   │   └── build.gradle.kts
│   └── api/
│       └── build.gradle.kts
├── buildSrc/
├── build.gradle.kts
└── settings.gradle.kts
```

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-project"

// Apps
include(":apps:web-app")
include(":apps:cli-app")

// Libraries
include(":libs:core")
include(":libs:utils")
include(":libs:api")
```

### 4. Composite Build

```
company-projects/
├── platform/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── app-one/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── app-two/
    ├── build.gradle.kts
    └── settings.gradle.kts
```

**app-one/settings.gradle.kts:**
```kotlin
rootProject.name = "app-one"

includeBuild("../platform") {
    dependencySubstitution {
        substitute(module("com.example:platform")).using(project(":"))
    }
}
```

**app-one/build.gradle.kts:**
```kotlin
dependencies {
    implementation("com.example:platform:1.0.0")  // Resolved from composite
}
```

## Build Conventions

### Convention Plugins (buildSrc)

**buildSrc/build.gradle.kts:**
```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
```

**buildSrc/src/main/kotlin/java-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
```

**buildSrc/src/main/kotlin/spring-boot-conventions.gradle.kts:**
```kotlin
plugins {
    id("java-conventions")
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

springBoot {
    buildInfo()
}
```

**Usage in modules:**
```kotlin
// app/build.gradle.kts
plugins {
    id("spring-boot-conventions")
    application
}

// No need to repeat common configuration
```

## Version Catalog

**gradle/libs.versions.toml:**
```toml
[versions]
kotlin = "1.9.20"
spring-boot = "3.1.5"
junit = "5.10.1"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }

# Spring Boot
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa", version.ref = "spring-boot" }

# Testing
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }

[bundles]
spring-boot-web = ["spring-boot-starter-web", "spring-boot-starter-data-jpa"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

**Using Version Catalog:**
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
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(libs.bundles.spring.boot.web)
    testImplementation(libs.junit.jupiter)
}
```

## Project Organization Patterns

### Pattern 1: Domain-Driven Structure

```
my-app/
├── domain/
│   ├── user/
│   ├── order/
│   └── payment/
├── infrastructure/
│   ├── database/
│   ├── messaging/
│   └── cache/
├── application/
│   ├── web-api/
│   └── cli/
└── shared/
    ├── common/
    └── utils/
```

### Pattern 2: Layer-Based Structure

```
my-app/
├── presentation/
│   ├── web/
│   └── api/
├── business-logic/
│   ├── services/
│   └── domain/
├── data-access/
│   ├── repositories/
│   └── entities/
└── cross-cutting/
    ├── security/
    └── logging/
```

### Pattern 3: Feature-Based Structure

```
my-app/
├── features/
│   ├── user-management/
│   ├── order-processing/
│   └── reporting/
├── core/
│   ├── domain/
│   └── infrastructure/
└── shared/
```

## Dependency Management

### Module Dependencies

**API vs Implementation:**
```kotlin
// lib-core/build.gradle.kts
dependencies {
    // Exposed to consumers
    api(project(":lib-api"))
    api("com.google.guava:guava:32.1.3-jre")

    // Internal only
    implementation(project(":lib-internal"))
    implementation("org.slf4j:slf4j-api:2.0.9")
}
```

**Dependency Constraints:**
```kotlin
// Root build.gradle.kts
subprojects {
    dependencies {
        constraints {
            implementation("com.google.guava:guava:32.1.3-jre")
            implementation("org.slf4j:slf4j-api:2.0.9")
        }
    }
}
```

## Build Organization Best Practices

1. **Use buildSrc for Conventions:** Centralize shared configuration
2. **Version Catalogs:** Manage versions in one place
3. **API vs Implementation:** Minimize transitive dependencies
4. **Logical Module Grouping:** Group by domain/layer/feature
5. **Consistent Naming:** Use clear, descriptive module names
6. **Documentation:** Include README in each module
7. **Gradle Properties:** Use `gradle.properties` for configuration

## Module Naming Conventions

```
:app                    # Application entry point
:lib-core              # Core library
:lib-utils             # Utility library
:feature-user          # Feature module
:api                   # API module
:impl                  # Implementation module
:test-utils            # Test utilities
```

## Build Performance for Multi-Module

**gradle.properties:**
```properties
# Enable parallel builds
org.gradle.parallel=true
org.gradle.workers.max=8

# Enable build cache
org.gradle.caching=true

# Enable configuration cache
org.gradle.configuration-cache=true

# Increase daemon memory
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## Composite Build Use Cases

1. **Platform/Library Development:** Develop library and app together
2. **Microservices:** Share common code across services
3. **Plugin Development:** Test plugins in real projects
4. **Large Codebases:** Break into independently buildable units

## Migration from Monolith to Multi-Module

### Step 1: Identify Module Boundaries

Analyze code dependencies to identify natural boundaries:
- Domain modules
- Infrastructure modules
- Shared utilities

### Step 2: Create Module Structure

```kotlin
// settings.gradle.kts
include(":module-a")
include(":module-b")
```

### Step 3: Extract Code

Move code to modules while maintaining dependencies:
```kotlin
// module-a/build.gradle.kts
dependencies {
    implementation(project(":shared"))
}
```

### Step 4: Refactor Dependencies

Replace internal dependencies with proper module dependencies.

### Step 5: Apply Conventions

Use buildSrc to standardize configuration across modules.

## Best Practices Summary

1. Start simple, add complexity as needed
2. Use buildSrc for shared conventions
3. Leverage version catalogs for dependency management
4. Apply convention plugins consistently
5. Document module responsibilities
6. Monitor build performance
7. Use composite builds for large organizations
8. Keep module dependencies minimal and clear
