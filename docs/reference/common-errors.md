# Common Build Errors and Solutions

**Gradle Version**: 7.0+, optimized for 8+

## Dependency Resolution Errors

### Could not find dependency

**Error:**
```
Could not find com.example:library:1.0.0
```

**Solutions:**
1. Check repository configuration
2. Verify artifact exists
3. Check network/proxy settings

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://repo.example.com") }
}
```

### Version conflict

**Error:**
```
Conflict found for the following module:
  - com.google.guava:guava between versions 30.1-jre and 31.0-jre
```

**Solutions:**
```kotlin
// Force specific version
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:31.1-jre")
}

// Or use constraints
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre")
    }
}
```

## Compilation Errors

### Cannot find symbol

**Error:**
```
error: cannot find symbol
  symbol:   class MyClass
  location: package com.example
```

**Solutions:**
1. Check dependencies declared
2. Verify source sets
3. Check module visibility (Kotlin)

```kotlin
dependencies {
    implementation(project(":lib"))
}
```

### Incompatible Java version

**Error:**
```
Execution failed for task ':compileJava'.
> error: invalid target release: 17
```

**Solutions:**
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## Task Execution Errors

### Task not found

**Error:**
```
Task 'build' not found in root project
```

**Solutions:**
1. Apply required plugin
2. Check task name spelling
3. Check project path

```kotlin
plugins {
    java
}
```

### Circular dependency

**Error:**
```
Circular dependency between the following tasks:
:compileJava
\--- :processResources
     \--- :compileJava (*)
```

**Solutions:**
- Remove circular task dependencies
- Use proper task ordering

```kotlin
tasks.named("compileJava") {
    // Remove dependsOn("processResources")
}
```

## Configuration Cache Errors

### Task.project access

**Error:**
```
Configuration cache problem: task ':myTask' of type 'MyTask': 
invocation of 'Task.project' at execution time is unsupported
```

**Solution:**
```kotlin
// ❌ Bad
tasks.register("bad") {
    doLast {
        println(project.name)
    }
}

// ✅ Good
tasks.register("good") {
    val projectName = project.name
    doLast {
        println(projectName)
    }
}
```

## Build Cache Errors

### Cache entry corrupted

**Error:**
```
Build cache entry ... is corrupted
```

**Solutions:**
```bash
# Clean cache
rm -rf ~/.gradle/caches/build-cache-1

# Disable cache temporarily
./gradlew build --no-build-cache
```

## Memory Errors

### OutOfMemoryError

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

### Metaspace error

**Error:**
```
java.lang.OutOfMemoryError: Metaspace
```

**Solution:**
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## Quick Reference

```bash
# Clean build
./gradlew clean build

# Refresh dependencies
./gradlew build --refresh-dependencies

# Debug
./gradlew build --info
./gradlew build --debug
./gradlew build --stacktrace

# Clear cache
rm -rf ~/.gradle/caches
```
