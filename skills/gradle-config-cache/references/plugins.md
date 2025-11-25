# Plugin Configuration Cache Compatibility

Plugin compatibility status for configuration cache.

## Fully Compatible (Gradle 8+)

These plugins work without issues:

| Plugin | Version | Notes |
|--------|---------|-------|
| java | Built-in | ✅ |
| java-library | Built-in | ✅ |
| application | Built-in | ✅ |
| kotlin-jvm | 1.8+ | ✅ |
| kotlin-multiplatform | 1.9+ | ✅ |
| Spring Boot | 3.0+ | ✅ |
| Android Gradle Plugin | 8.0+ | ✅ |
| Shadow | 8.0+ | ✅ |
| Spotless | 6.0+ | ✅ |
| JUnit Platform | 1.8+ | ✅ |

## Partially Compatible

These plugins may have issues:

| Plugin | Version | Known Issues |
|--------|---------|--------------|
| Protobuf | 0.9+ | Some tasks incompatible |
| ANTLR | 4.x | Check version |
| JaCoCo | Built-in | Report tasks may have issues |

## Check Plugin Status

```bash
# Run build and check for plugin-related warnings
./gradlew build --configuration-cache 2>&1 | grep -i "plugin"
```

## Reporting Issues

If a plugin isn't compatible:

1. Check plugin's GitHub issues
2. Search for "configuration cache"
3. Report issue with reproduction steps
4. Consider contributing a fix

## Workarounds

### Disable for Specific Tasks

```kotlin
tasks.named("problematicTask") {
    notCompatibleWithConfigurationCache("Plugin X not compatible")
}
```

### Plugin Updates

Always use latest plugin versions:

```kotlin
// settings.gradle.kts
pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.21"
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}
```
