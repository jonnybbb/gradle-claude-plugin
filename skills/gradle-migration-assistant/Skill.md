---
name: gradle-migration-assistant
description: Guides Gradle version migrations from 6/7 to 8/9, identifies deprecated APIs, and provides upgrade paths with compatibility checks.
version: 1.0.0
---

# Gradle Migration Assistant

Guide Gradle version migrations with automated checks.

## When to Use

Invoke when users want to:
- Upgrade Gradle versions
- Migrate from Gradle 6/7 to 8/9
- Fix deprecated API warnings
- Update gradle wrapper
- Check plugin compatibility

## Migration Workflow

### 1. Update Wrapper
```bash
./gradlew wrapper --gradle-version=8.5
```

### 2. Fix Deprecated APIs

```kotlin
// ❌ Deprecated
compile 'com.google.guava:guava:32.1.3-jre'

// ✅ Modern
implementation("com.google.guava:guava:32.1.3-jre")
```

Common changes:
- `compile` → `implementation`
- `runtime` → `runtimeOnly`
- `Project.convention` → `extensions`
- `baseName` → `archiveBaseName.set()`

### 3. Update Plugins

- Spring Boot 3+ for Gradle 8+
- Kotlin 1.9+ for Gradle 8+
- Android Gradle Plugin 7+ for Gradle 7+

### 4. Test Build
```bash
./gradlew build --warning-mode=all
```

## Breaking Changes (Gradle 8)

1. Removed `compile`/`runtime` configurations
2. Removed `Project.convention` API
3. Configuration cache improvements
4. Stricter task validation

## Best Practices

1. Migrate incrementally (don't skip versions)
2. Fix all deprecation warnings first
3. Update plugins to latest versions
4. Test thoroughly at each step
