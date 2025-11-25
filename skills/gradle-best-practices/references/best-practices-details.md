# Gradle Best Practices - Detailed Reference

This document provides comprehensive details on each Gradle best practice with extended examples and explanations.

## 1. Use Kotlin DSL

### Why Kotlin DSL?

The Kotlin DSL offers several advantages:

| Feature | Kotlin DSL | Groovy DSL |
|---------|-----------|------------|
| Type Safety | Strict typing | Dynamic typing |
| IDE Support | Full auto-completion | Limited |
| Compile-time Checks | Yes | No |
| Refactoring Support | Excellent | Limited |
| Learning Curve | Kotlin knowledge required | More forgiving |

### Migration Path

For existing Groovy builds:

1. Start with `settings.gradle` → `settings.gradle.kts`
2. Migrate buildSrc (if present)
3. Migrate root `build.gradle`
4. Migrate subproject build files one at a time

### Common Syntax Differences

```kotlin
// Groovy DSL
apply plugin: 'java'
implementation 'com.example:lib:1.0'
task myTask {
    doLast {
        println 'Hello'
    }
}

// Kotlin DSL
plugins {
    java
}
implementation("com.example:lib:1.0")
tasks.register("myTask") {
    doLast {
        println("Hello")
    }
}
```

## 2. Use Latest Minor Version

### Version Support Policy

Gradle actively supports:
- Current major release (latest minor)
- Previous major release (latest minor only)

### Upgrade Strategy

```bash
# Check current version
./gradlew --version

# View available versions
curl -s https://services.gradle.org/versions/all | jq '.[].version' | head -20

# Upgrade wrapper
./gradlew wrapper --gradle-version 8.14

# Verify upgrade
./gradlew --version

# Run build with full stacktrace to catch deprecations
./gradlew build --warning-mode all
```

### Dealing with Plugin Compatibility

1. Always upgrade Gradle first
2. Check plugin compatibility with new Gradle version
3. Update plugins to their latest compatible versions
4. Use shadow jobs to test in CI before merging

## 3. Apply Plugins Using `plugins` Block

### Plugin Application Methods

| Method | Recommended | Notes |
|--------|-------------|-------|
| `plugins { }` | Yes | Preferred for all plugins |
| `apply(plugin = "...")` | No | Legacy, avoid |
| `buildscript { }` | No | Only for special cases |

### Managing Plugin Versions Centrally

In `settings.gradle.kts`:

```kotlin
pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.20"
        id("com.google.protobuf") version "0.9.4"
    }
}
```

Then in `build.gradle.kts`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")  // Version from settings
    id("com.google.protobuf")
}
```

### Using Version Catalogs

In `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.20"

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

In `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}
```

## 4. Do Not Use Internal APIs

### Identifying Internal APIs

Internal APIs are identified by:
- Package names containing `.internal.`
- Class names ending with `Internal` or `Impl`
- Methods annotated with `@Internal`

### Common Patterns and Public Alternatives

| Internal Pattern | Public Alternative |
|-----------------|-------------------|
| `ProjectInternal` | `Project` interface |
| `TaskInternal` | `Task` interface |
| `DefaultTask` methods not in `Task` | Stick to `Task` interface |
| `*ContainerInternal` | Use public container methods |

### Reporting Missing APIs

If you need functionality not available in public APIs:
1. Search existing issues at https://github.com/gradle/gradle/issues
2. Create a feature request if not found
3. As workaround, copy needed code and extend public types

## 5. Set Build Flags in `gradle.properties`

### Recommended Properties

```properties
# Performance
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.vfs.watch=true

# Memory (adjust based on project size)
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC

# Build features
org.gradle.workers.max=8
```

### Property Precedence

Properties are resolved in this order (highest to lowest):
1. Command-line (`-P` or `-D`)
2. `gradle.properties` in `GRADLE_USER_HOME`
3. `gradle.properties` in project root
4. Environment variables (`ORG_GRADLE_PROJECT_*`)

### Environment-Specific Properties

For CI-specific settings, use environment variables:

```bash
# In CI config
export ORG_GRADLE_PROJECT_ci=true
```

Then in build script:

```kotlin
if (project.hasProperty("ci")) {
    // CI-specific configuration
}
```

## 6. Name Your Root Project

### Why Naming Matters

The root project name is used in:
- Build scan URLs
- Error messages
- Dependency coordinates (for published artifacts)
- Task paths
- Cache keys

### Multi-project Example

```kotlin
// settings.gradle.kts
rootProject.name = "my-application"

include("app")
include("core")
include("data:api")
include("data:impl")
```

This creates task paths like:
- `:app:build`
- `:core:compileJava`
- `:data:api:test`

## 7. No `gradle.properties` in Subprojects

### Why This Is Problematic

- Properties in subprojects are harder to discover
- Can lead to inconsistent behavior
- Makes debugging configuration issues difficult
- Properties don't inherit to sub-subprojects as expected

### Alternatives for Subproject-Specific Configuration

**Using extra properties:**

```kotlin
// build.gradle.kts (subproject)
extra["myProperty"] = "value"
```

**Using extensions:**

```kotlin
// In a convention plugin
abstract class MyExtension {
    abstract val myProperty: Property<String>
}

// Register and configure
extensions.create<MyExtension>("myConfig")
```

**Using convention plugins:**

Create shared configuration in `buildSrc` or an included build.

## Best Practices Verification Script

Run this to check compliance:

```bash
#!/bin/bash
# best-practices-check.sh

echo "=== Gradle Best Practices Check ==="

# Check for Kotlin DSL
if ls *.gradle 2>/dev/null | grep -v gradle.properties > /dev/null; then
    echo "WARNING: Found Groovy DSL files (*.gradle)"
fi

# Check root project name
if ! grep -q "rootProject.name" settings.gradle.kts 2>/dev/null; then
    echo "WARNING: Root project name not set in settings.gradle.kts"
fi

# Check for apply plugin
if grep -r "apply plugin" --include="*.gradle*" . 2>/dev/null; then
    echo "WARNING: Found legacy 'apply plugin' usage"
fi

# Check for internal API usage
if grep -r "org.gradle.*internal" --include="*.kt" --include="*.gradle.kts" . 2>/dev/null; then
    echo "WARNING: Found internal API usage"
fi

# Check for subproject gradle.properties
find . -name "gradle.properties" -not -path "./gradle.properties" | while read f; do
    echo "WARNING: Found gradle.properties in subproject: $f"
done

echo "=== Check complete ==="
```

## Additional Resources

- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [Gradle Best Practices](https://docs.gradle.org/current/userguide/best_practices_general.html)
- [Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Structuring Large Projects](https://docs.gradle.org/current/userguide/structuring_software_products.html)
