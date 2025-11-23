# Report Formats

## Project Structure Report

```
=== Gradle Project Analysis ===

Project: my-awesome-app
Gradle Version: 8.5
Build Files: Kotlin DSL

--- Project Structure ---
Root: my-awesome-app
├── :app (application)
├── :lib-core (java-library)
└── :lib-utils (java-library)

--- Applied Plugins ---
Root:
  - org.jetbrains.kotlin.jvm (1.9.20)

:app:
  - application
  - org.springframework.boot (3.1.5)

:lib-core:
  - java-library

--- Dependencies ---
:app → :lib-core (implementation)
:lib-core → :lib-utils (api)

External Dependencies:
  - com.google.guava:guava:32.1.3-jre
  - org.springframework.boot:spring-boot-starter-web:3.1.5

--- Configuration Patterns ---
✓ Version catalog: gradle/libs.versions.toml
✓ Convention plugins: buildSrc/
✓ Gradle wrapper: 8.5
⚠ No dependency locking enabled
```

## JSON Format (Optional)

```json
{
  "project": {
    "name": "my-awesome-app",
    "gradleVersion": "8.5",
    "dslType": "kotlin"
  },
  "modules": [
    {
      "name": ":app",
      "type": "application",
      "plugins": ["application", "org.springframework.boot"],
      "dependencies": {
        "implementation": [":lib-core"],
        "external": ["org.springframework.boot:spring-boot-starter-web:3.1.5"]
      }
    }
  ]
}
```

## Markdown Format

```markdown
# Project Analysis: my-awesome-app

## Structure
- **Type**: Multi-module (3 modules)
- **Gradle**: 8.5
- **DSL**: Kotlin

## Modules
1. `:app` - Application
2. `:lib-core` - Library
3. `:lib-utils` - Library

## Key Findings
- ✓ Modern Gradle version
- ✓ Uses version catalog
- ⚠ Consider dependency locking
```
