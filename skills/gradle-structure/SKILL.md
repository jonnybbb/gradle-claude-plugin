---
name: gradle-structure
description: This skill should be used when the user asks to "organize multi-project build", "share build logic", "set up composite build", "structure monorepo", "use buildSrc", "create convention plugins", "gradle init", "create new Gradle project", "start new project", or mentions settings.gradle, include(), includeBuild(), project organization, or allprojects/subprojects patterns.
---

# Gradle Build Structure

## Overview

Proper build structure enables code sharing, parallel execution, and maintainability. Choose patterns based on project size and team needs.

For multi-project setup, see [references/multi-project.md](references/multi-project.md).
For composite builds, see [references/composite.md](references/composite.md).

## Quick Start

### Create New Project (gradle init)

```bash
# Interactive mode
gradle init

# Non-interactive with options
gradle init --type java-application --dsl kotlin --test-framework junit-jupiter

# Available types:
# - basic              - Empty project
# - java-application   - Java app with main class
# - java-library       - Java library
# - kotlin-application - Kotlin app
# - kotlin-library     - Kotlin library
# - groovy-application - Groovy app
# - groovy-library     - Groovy library
# - scala-application  - Scala app
# - scala-library      - Scala library
# - cpp-application    - C++ app
# - cpp-library        - C++ library
```

**Generated structure (java-application)**:
```
my-project/
├── gradle/
│   ├── libs.versions.toml    # Version catalog
│   └── wrapper/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/
│       └── test/java/
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

### Multi-Project Structure

```
my-app/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   └── build.gradle.kts
├── core/
│   └── build.gradle.kts
└── shared/
    └── build.gradle.kts
```

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("app", "core", "shared")
```

### Convention Plugins (buildSrc)

```kotlin
// buildSrc/src/main/kotlin/java-conventions.gradle.kts
plugins { `java-library` }

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

Apply: `plugins { id("java-conventions") }`

### Composite Builds

```kotlin
// settings.gradle.kts
includeBuild("../my-library")
```

## Quick Reference

| Pattern | Use Case |
|---------|----------|
| Multi-project | Monorepo, related modules |
| Convention plugins | Shared build logic |
| Composite build | Multiple repositories |
| Platform project | Version alignment |

## Best Practices

1. **Use convention plugins over allprojects/subprojects** - Convention plugins provide type safety, IDE support, and are testable. The allprojects and subprojects blocks create implicit coupling and are harder to maintain.

2. **Use version catalogs for dependencies** - Define all dependency versions in `gradle/libs.versions.toml` for centralized management and type-safe accessors.

3. **Avoid cross-project configuration** - Never configure another project from a build script. Instead, use convention plugins or platform dependencies.

4. **Enable parallel execution** - Add `org.gradle.parallel=true` to gradle.properties for faster builds.

5. **Keep buildSrc focused** - Only include shared build logic, not application code. Consider included builds for larger codebases.

## When to Use Each Pattern

| Scenario | Recommended Pattern |
|----------|---------------------|
| Small project, few shared settings | Convention plugins in buildSrc |
| Medium project, team standards | Convention plugins + version catalogs |
| Large monorepo | Composite builds + convention plugins |
| Multiple repositories, shared plugins | Published convention plugins |
| Version alignment only | Platform projects |

## JBang Tools

```bash
# Analyze project structure and configuration
jbang ${CLAUDE_PLUGIN_ROOT}/tools/gradle-analyzer.java /path/to/project --json
```

## Related Files

- [references/multi-project.md](references/multi-project.md) - Multi-project patterns
- [references/composite.md](references/composite.md) - Composite build setup
- [references/buildsrc.md](references/buildsrc.md) - buildSrc best practices
