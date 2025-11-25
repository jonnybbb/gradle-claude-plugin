# Gradle 7 to 8 Migration Guide

Detailed guide for migrating from Gradle 7.x to 8.x.

## Prerequisites

- Gradle 7.6+ (latest 7.x recommended)
- Java 8+ (Java 17 recommended)
- All deprecation warnings resolved

## Breaking Changes

### Removed APIs

#### Archive Properties

```kotlin
// ❌ Removed in Gradle 8
archiveName = "app.jar"
archiveBaseName = "app"
archivesBaseName = "myapp"
destinationDir = file("dist")

// ✅ Gradle 8+ replacements
archiveFileName.set("app.jar")
archiveBaseName.set("app")
base.archivesName.set("myapp")
destinationDirectory.set(file("dist"))
```

#### Task Creation

```kotlin
// ❌ Deprecated (still works but warns)
tasks.create("myTask") { }
tasks.getByName("test") { }

// ✅ Recommended
tasks.register("myTask") { }
tasks.named("test") { }
```

#### Configuration Access

```kotlin
// ❌ Removed
configurations.compile
configurations.testCompile
configurations.runtime

// ✅ Replacements
configurations.implementation
configurations.testImplementation
configurations.runtimeOnly
```

### Plugin Convention Changes

```kotlin
// ❌ Removed - JavaPluginConvention
project.convention.getPlugin(JavaPluginConvention::class.java)

// ✅ Use extension instead
project.extensions.getByType<JavaPluginExtension>()
```

### Property Access

```kotlin
// ❌ Changed behavior
val value: String = property("key") as String

// ✅ Explicit provider
val value = providers.gradleProperty("key").get()
```

## Step-by-Step Migration

### Step 1: Update to Latest 7.x

```bash
./gradlew wrapper --gradle-version 7.6.4
./gradlew build --warning-mode=all
```

Fix all deprecation warnings before proceeding.

### Step 2: Update to Gradle 8

```bash
./gradlew wrapper --gradle-version 8.11
```

### Step 3: Fix Compilation Errors

Address any removed API errors:

```kotlin
// Common fixes
tasks.jar {
    // archiveName → archiveFileName
    archiveFileName.set("my-app.jar")
}

// base plugin
plugins {
    base
}
base.archivesName.set("my-app")
```

### Step 4: Enable Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

### Step 5: Test Thoroughly

```bash
./gradlew clean build test
./gradlew build --configuration-cache
```

## Common Migration Fixes

### Jar Task

```kotlin
// Before (Gradle 7)
tasks.jar {
    archiveName = "app.jar"
    destinationDir = file("$buildDir/libs")
}

// After (Gradle 8)
tasks.jar {
    archiveFileName.set("app.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}
```

### Copy Task

```kotlin
// Before
tasks.register<Copy>("copyDocs") {
    destinationDir = file("$buildDir/docs")
}

// After
tasks.register<Copy>("copyDocs") {
    destinationDir = layout.buildDirectory.dir("docs").get().asFile
    // Or better:
    into(layout.buildDirectory.dir("docs"))
}
```

### Java Extension

```kotlin
// Before
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// After (preferred)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## New Features in Gradle 8

### Configuration Cache (Stable)

```properties
org.gradle.configuration-cache=true
```

### Improved Toolchains

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
```

### Better Error Messages

Gradle 8 provides more actionable error messages with suggested fixes.

## Troubleshooting

### "Cannot find method archiveName"

Replace with `archiveFileName.set("name.jar")`

### "Cannot find property archivesBaseName"

Use `base.archivesName.set("name")`

### Plugin Compatibility Issues

Update plugins to latest versions:
- Kotlin 1.8+
- Spring Boot 3.0+
- Android Gradle Plugin 8.0+
