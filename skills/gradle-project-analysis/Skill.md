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
   - See `multimodule-analysis.md` for detailed multi-module patterns

2. **Analyze Build Files**
   - Parse build.gradle[.kts] for plugins, dependencies, tasks
   - Support both Kotlin DSL and Groovy DSL
   - See `build-file-patterns.md` for DSL-specific examples

3. **Identify Configuration Patterns**
   - Version catalogs (gradle/libs.versions.toml)
   - Convention plugins (buildSrc/)
   - Gradle properties
   - Wrapper configuration
   - See `configuration-patterns.md` for detailed patterns

4. **Analyze Plugins**
   - Core Gradle plugins
   - Community plugins (Spring Boot, Kotlin, Android, etc.)
   - See `plugin-analysis.md` for plugin identification

5. **Generate Report**
   - Project hierarchy tree
   - Applied plugins per module
   - Inter-module dependencies
   - Configuration recommendations
   - See `report-formats.md` for output examples

## Quick Commands

```bash
# View project structure
gradle projects

# List build files
find . -name "build.gradle*" -o -name "settings.gradle*"

# Check dependencies
gradle dependencies --configuration compileClasspath
```

## Reference Files

For detailed information on specific topics:
- **Multi-module projects**: See `multimodule-analysis.md`
- **Build file patterns**: See `build-file-patterns.md`
- **Configuration patterns**: See `configuration-patterns.md`
- **Plugin analysis**: See `plugin-analysis.md`
- **Report formats**: See `report-formats.md`
- **Recommendations**: See `recommendations.md`
