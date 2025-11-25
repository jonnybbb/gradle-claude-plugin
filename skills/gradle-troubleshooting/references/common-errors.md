# Common Gradle Errors

Quick reference for frequent errors and fixes.

## Dependency Errors

### Could Not Find

```
Could not find com.example:library:1.0.0
```

**Causes**:
- Repository not configured
- Artifact doesn't exist
- Network/proxy issues

**Fix**:
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://custom.repo.com") }
}
```

### Version Conflict

```
Conflict: guava between 30.1-jre and 31.0-jre
```

**Fix**:
```kotlin
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:31.1-jre")
}
```

### Duplicate Classes

```
Duplicate class found in modules A and B
```

**Fix**:
```kotlin
configurations.all {
    exclude(group = "org.duplicate", module = "module")
}
```

## Compilation Errors

### Cannot Find Symbol

```
error: cannot find symbol
```

**Causes**:
- Missing dependency
- Wrong source set
- Import missing

**Fix**: Check `dependencies` block

### Incompatible Java Version

```
error: invalid target release: 17
```

**Fix**:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## Task Errors

### Task Not Found

```
Task 'xyz' not found in root project
```

**Fix**: `./gradlew tasks --all` to list available tasks

### Circular Dependency

```
Circular dependency between tasks
```

**Fix**: Remove circular `dependsOn` relationships

## Memory Errors

### OutOfMemoryError: Java heap space

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g
```

### OutOfMemoryError: Metaspace

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## Configuration Cache

### Task.project at execution time

**Fix**: Capture values during configuration:
```kotlin
val name = project.name
doLast { println(name) }
```

See gradle-config-cache skill for more patterns.
