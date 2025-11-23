---
name: gradle-project-analysis
description: Analyzes Gradle project structure, dependencies, and configuration. Use when asked about project organization, modules, or build file analysis.
version: 1.0.0
---

# Gradle Project Analysis

Analyze Gradle projects to understand structure, dependencies, and configuration.

## When to Use

Invoke this skill when the user asks about:
- Project structure or organization
- Module dependencies and relationships
- Build file configuration
- Applied plugins
- Gradle setup

## Analysis Process

1. **Detect Project Type**
   - Check for settings.gradle[.kts] to identify multi-module projects
   - List all modules and their relationships

2. **Analyze Build Files**
   - Parse build.gradle[.kts] for plugins, dependencies, tasks
   - Support both Kotlin DSL and Groovy DSL

3. **Identify Configuration Patterns**
   - Version catalogs (gradle/libs.versions.toml)
   - Convention plugins (buildSrc/)
   - Gradle properties
   - Wrapper configuration

4. **Generate Report**
   - Project hierarchy tree
   - Applied plugins per module
   - Inter-module dependencies
   - Configuration recommendations

## Example Commands

```bash
# View project structure
gradle projects

# List build files
find . -name "build.gradle*" -o -name "settings.gradle*"
```

## Output Format

Provide a structured report with:
- Project name and type
- Module hierarchy
- Plugin inventory
- Dependency map
- Recommendations for improvements
