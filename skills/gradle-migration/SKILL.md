---
name: gradle-migration
description: This skill should be used when the user asks to "upgrade Gradle version", "migrate to Gradle 8", "fix deprecation warnings", "update Gradle wrapper", "migrate from Gradle 7 to 8", or mentions breaking changes after upgrade, deprecated APIs, migration strategy, or version compatibility issues.
---

# Gradle Migration

## Overview

Migrating Gradle versions requires addressing breaking changes and deprecations. Upgrade one major version at a time for best results.

For Gradle 7→8 details, see [references/gradle-7-to-8.md](references/gradle-7-to-8.md).
For API migrations, see [references/api-changes.md](references/api-changes.md).

## Quick Start

### Upgrade Wrapper

```bash
./gradlew wrapper --gradle-version 8.11
```

### Check for Issues

```bash
./gradlew build --warning-mode=all
```

## Common Fixes (Gradle 8)

### Removed APIs

```kotlin
// ❌ Removed
archiveName = "app.jar"

// ✅ Replacement
archiveFileName.set("app.jar")
```

```kotlin
// ❌ Removed
archivesBaseName = "myapp"

// ✅ Replacement
base.archivesName.set("myapp")
```

### Task Registration

```kotlin
// ❌ Deprecated
tasks.create("myTask") { }

// ✅ Recommended
tasks.register("myTask") { }
```

## Migration Strategy

1. **Create branch**: `git checkout -b gradle-upgrade`
2. **Update wrapper**: `./gradlew wrapper --gradle-version X.Y`
3. **Build with warnings**: `./gradlew build --warning-mode=all`
4. **Fix deprecations**
5. **Enable configuration cache**
6. **Test thoroughly**

## Quick Reference

| Old API | New API |
|---------|---------|
| archiveName | archiveFileName.set() |
| archivesBaseName | base.archivesName.set() |
| tasks.create() | tasks.register() |
| getByName() | named() |
| compile | implementation |

## Version Requirements

| Gradle | Min Java | Recommended |
|--------|----------|-------------|
| 7.x | 8 | 11 |
| 8.x | 8 | 17 |
| 9.x | 17 | 21 |

## Related Files

- [references/gradle-7-to-8.md](references/gradle-7-to-8.md) - Detailed 7→8 guide
- [references/api-changes.md](references/api-changes.md) - API migration reference
- [references/checklist.md](references/checklist.md) - Migration checklist
