# Version Catalogs

**Source**: https://docs.gradle.org/current/userguide/platforms.html  
**Gradle Version**: 7.0+, stable in 8+

## Overview

Version catalogs provide centralized dependency version management, type-safe accessors, and dependency bundles.

## Creating a Catalog

**gradle/libs.versions.toml:**
```toml
[versions]
kotlin = "1.9.21"
spring = "6.1.0"
junit = "5.10.0"

[libraries]
# Simple notation
guava = "com.google.guava:guava:31.1-jre"

# With version reference
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
spring-core = { module = "org.springframework:spring-core", version.ref = "spring" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }

# Rich versions
guava-latest = { module = "com.google.guava:guava", version = { strictly = "[29, 32[", prefer = "31.1-jre" } }

[bundles]
kotlin = ["kotlin-stdlib", "kotlin-reflect"]
spring = ["spring-core", "spring-context", "spring-beans"]
testing = ["junit-jupiter"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version = "3.2.0" }
```

## Using Catalog

**build.gradle.kts:**
```kotlin
dependencies {
    // Single library
    implementation(libs.guava)
    implementation(libs.kotlin.stdlib)
    
    // Bundle
    testImplementation(libs.bundles.testing)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
}
```

## Type-Safe Accessors

Gradle generates type-safe accessors:

```kotlin
// libs.versions.toml: kotlin-stdlib
dependencies {
    implementation(libs.kotlin.stdlib)  // Type-safe
}

// libs.versions.toml: junit-jupiter
dependencies {
    testImplementation(libs.junit.jupiter)  // Type-safe
}
```

## Multiple Catalogs

**settings.gradle.kts:**
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
        create("testLibs") {
            from(files("gradle/test-libs.versions.toml"))
        }
    }
}
```

**Usage:**
```kotlin
dependencies {
    implementation(libs.guava)
    testImplementation(testLibs.junit.jupiter)
}
```

## Sharing Catalogs

### Via Plugin

```kotlin
// published-catalog/build.gradle.kts
plugins {
    `version-catalog`
    `maven-publish`
}

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))
    }
}
```

### Consuming Published Catalog

**settings.gradle.kts:**
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from("com.example:catalog:1.0")
        }
    }
}
```

## Overriding Versions

**build.gradle.kts:**
```kotlin
dependencies {
    implementation(libs.guava) {
        version {
            require("32.0-jre")
        }
    }
}
```

## Best Practices

1. **One catalog per repository** (in monorepo)
2. **Group related dependencies**
3. **Use bundles for common sets**
4. **Version references for families**
5. **Document major version choices**

## Migration

### From Properties

**Before (gradle.properties):**
```properties
kotlin.version=1.9.21
spring.version=6.1.0
```

```kotlin
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${property("kotlin.version")}")
}
```

**After (libs.versions.toml):**
```toml
[versions]
kotlin = "1.9.21"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
```

```kotlin
dependencies {
    implementation(libs.kotlin.stdlib)
}
```

## Quick Reference

```toml
# gradle/libs.versions.toml
[versions]
guava = "31.1-jre"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }

[bundles]
testing = ["junit-jupiter", "mockito"]

[plugins]
kotlin = { id = "org.jetbrains.kotlin.jvm", version = "1.9.21" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.guava)
    testImplementation(libs.bundles.testing)
}

plugins {
    alias(libs.plugins.kotlin)
}
```
