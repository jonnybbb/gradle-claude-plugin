# Gradle Expert Framework - Examples

Practical examples demonstrating Gradle best practices and tool usage.

## Build Patterns

Example build scripts showing modern Gradle patterns.

| File | Description |
|------|-------------|
| [config-cache-compatible.gradle.kts](build-patterns/config-cache-compatible.gradle.kts) | Configuration cache compatible build patterns |
| [lazy-task-registration.gradle.kts](build-patterns/lazy-task-registration.gradle.kts) | Lazy vs eager task APIs comparison |
| [incremental-task.gradle.kts](build-patterns/incremental-task.gradle.kts) | Incremental task implementation |
| [libs.versions.toml](build-patterns/libs.versions.toml) | Complete version catalog example |
| [CHEATSHEET.md](build-patterns/CHEATSHEET.md) | Quick reference for patterns |

## Sample Projects

Complete project structures you can use as templates.

### Multi-Project Build

A well-structured multi-module Gradle project:

```
sample-projects/multi-project/
├── settings.gradle.kts      # Project includes, caching, feature previews
├── build.gradle.kts         # Root build configuration
├── gradle.properties        # Optimized performance settings
├── buildSrc/
│   ├── build.gradle.kts     # Convention plugin dependencies
│   └── src/main/kotlin/
│       └── java-conventions.gradle.kts  # Reusable convention plugin
└── app/
    └── build.gradle.kts     # Application module example
```

| File | Description |
|------|-------------|
| [settings.gradle.kts](sample-projects/multi-project/settings.gradle.kts) | Settings with modules, cache, feature previews |
| [build.gradle.kts](sample-projects/multi-project/build.gradle.kts) | Root build with allprojects/subprojects |
| [gradle.properties](sample-projects/multi-project/gradle.properties) | Optimized performance settings |
| [buildSrc/build.gradle.kts](sample-projects/multi-project/buildSrc/build.gradle.kts) | Convention plugin build |
| [java-conventions.gradle.kts](sample-projects/multi-project/buildSrc/src/main/kotlin/java-conventions.gradle.kts) | Reusable Java conventions |
| [app/build.gradle.kts](sample-projects/multi-project/app/build.gradle.kts) | Application module using conventions |

## Tool Outputs

Example outputs from the framework tools to understand what they produce.

| File | Tool | Description |
|------|------|-------------|
| [build-health-check-output.txt](tool-outputs/build-health-check-output.txt) | build-health-check.java | Overall health score with recommendations |
| [task-analyzer-output.txt](tool-outputs/task-analyzer-output.txt) | task-analyzer.java | Task API analysis and fixes |
| [migration-agent-output.txt](tool-outputs/migration-agent-output.txt) | migration-agent.ts | Migration analysis report |

## Using These Examples

### Copy Build Patterns

```bash
# Copy a pattern to your project
cp examples/build-patterns/config-cache-compatible.gradle.kts ./build.gradle.kts

# Copy version catalog
cp examples/build-patterns/libs.versions.toml ./gradle/libs.versions.toml
```

### Use Multi-Project Template

```bash
# Copy entire template
cp -r examples/sample-projects/multi-project/* ./my-new-project/

# Customize settings.gradle.kts with your module names
```

### Test Tools

```bash
# Run health check on your project
jbang tools/build-health-check.java /path/to/project

# Compare output with examples
diff <(jbang tools/task-analyzer.java .) examples/tool-outputs/task-analyzer-output.txt
```

## Key Takeaways

### Configuration Cache Compatibility
- Use `providers.*` for property access
- Use `layout.*` for file references  
- Inject services instead of using `project` in task actions
- See [config-cache-compatible.gradle.kts](build-patterns/config-cache-compatible.gradle.kts)

### Task Registration
- Always use `tasks.register()` instead of `tasks.create()`
- Use `tasks.named()` instead of `tasks.getByName()`
- See [lazy-task-registration.gradle.kts](build-patterns/lazy-task-registration.gradle.kts)

### Project Structure
- Use version catalogs for dependency management
- Use convention plugins in buildSrc
- Enable parallel, caching, and configuration cache
- See [multi-project/](sample-projects/multi-project/)

### Performance
- Check [gradle.properties](sample-projects/multi-project/gradle.properties) for optimal settings
- Run `build-health-check.java` to identify improvements
- Target 80%+ lazy task registration score
