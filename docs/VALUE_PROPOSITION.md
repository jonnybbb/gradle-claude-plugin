# Gradle Claude Plugin: What It Does

A Claude Code plugin that embeds Gradle expertise directly in your AI assistant. Instead of context-switching to Stack Overflow or Gradle docs, get immediate, contextual guidance.

## Problem Statement

Gradle builds are complex. Common pain points:

- **Configuration cache adoption** requires systematic code changes across build scripts
- **Performance bottlenecks** hide in daemon tuning, parallelization gaps, and cache misses
- **Version migrations** (7→8, 8→9) involve dozens of API deprecations
- **Custom task development** needs proper input/output declarations for correctness

Traditional approach: Search docs → Copy patterns → Adapt to your project → Debug issues → Repeat.

## What This Plugin Provides

### 1. Contextual Skills (10 domains)

Skills inject Gradle domain knowledge into Claude's responses. When you ask about performance, Claude draws from curated optimization patterns rather than generic advice.

| Skill | Use Case |
|-------|----------|
| `gradle-config-cache` | Enable configuration cache, fix incompatibilities |
| `gradle-performance` | Diagnose slow builds, tune JVM/parallelism |
| `gradle-dependencies` | Version catalogs, conflict resolution, platforms |
| `gradle-task-development` | Cacheable tasks, Worker API, proper annotations |
| `gradle-plugin-development` | Extension objects, test strategies, publishing |
| `gradle-migration` | API changes between major versions |
| `gradle-build-cache` | Remote cache setup, CI/CD patterns |
| `gradle-structure` | Multi-project layouts, composite builds |
| `gradle-troubleshooting` | Common errors, debugging techniques |
| `gradle-doctor` | Holistic project health assessment |

Each skill references curated documentation—not raw Gradle docs—optimized for AI consumption.

### 2. Diagnostic Tools (JBang)

Run actual analysis on your project:

```bash
# Analyze project structure
jbang tools/gradle-analyzer.java /path/to/project --json

# Detect configuration cache issues
jbang tools/cache-validator.java /path/to/project

# Profile build performance
jbang tools/performance-profiler.java /path/to/project

# Examine task implementations
jbang tools/task-analyzer.java /path/to/project
```

Tools use Gradle Tooling API for accurate introspection—not string parsing.

### 3. Guided Workflows

Commands orchestrate multi-step operations:

- `/doctor` - Comprehensive project health check
- `/upgrade` - Guided version upgrade with API mapping
- `/fix-config-cache` - Fix configuration cache compatibility issues
- `/optimize-performance` - Performance audit with prioritized fixes
- `/create-task` - Scaffolds properly-annotated custom tasks

### 4. Autonomous Agents

For complex tasks requiring iteration:

- **Doctor Agent** - Analyzes project, produces prioritized findings report
- **Migration Agent** - Plans and executes version upgrades

## Technical Value

### Configuration Cache Enablement

**Before:** Trial and error with `--configuration-cache` flag, cryptic error messages.

**After:** Plugin identifies exact issues and provides copy-paste fixes:

```kotlin
// Problem: System.getProperty at configuration time
val dbUrl = System.getProperty("db.url")

// Fix: Provider API defers evaluation
val dbUrl = providers.systemProperty("db.url")
```

Coverage: System properties, environment variables, eager task creation, project access in task actions, deprecated APIs.

### Performance Optimization

Plugin diagnoses:
- Missing parallelization (`org.gradle.parallel`)
- Suboptimal JVM args (heap, metaspace, GC tuning)
- Cache misses (local/remote, fingerprinting issues)
- Build avoidance failures (task inputs/outputs)

Provides benchmarking methodology, not just "try this."

### Migration Support

Maps deprecated APIs to replacements:

| Gradle 7.x | Gradle 8.x |
|------------|------------|
| `project.buildDir` | `layout.buildDirectory` |
| `tasks.create()` | `tasks.register()` |
| `configurations.compile` | `configurations.implementation` |
| `task.dependsOn { }` | `task.configure { dependsOn() }` |

With context for why each change matters.

## Integration

### Installation

```bash
# Add to Claude Code
claude plugins install claude-gradle-plugin
```

Or clone and symlink to `~/.claude/plugins/`.

### Requirements

- Java 25+ (for JBang tools)
- JBang 0.115.0+
- Gradle 6.0+ (analyzed projects)

### Test Fixtures

Five projects for validation:
- `simple-java` - Healthy baseline
- `config-cache-broken` - 16 intentional issues
- `legacy-groovy` - Migration testing
- `multi-module` - Scale testing (4 modules)
- `spring-boot` - Framework compatibility

## Comparison

| Approach | Context | Accuracy | Iteration Speed |
|----------|---------|----------|-----------------|
| Gradle Docs | Generic | High | Slow (manual lookup) |
| Stack Overflow | Outdated | Variable | Slow (search → evaluate) |
| Gradle Enterprise | Project-specific | High | Fast (dashboards) |
| **This Plugin** | Project-specific | High | Fast (conversational) |

This plugin complements (not replaces) Gradle Enterprise. GE provides build telemetry; this plugin provides expert interpretation and actionable fixes.

## Limitations

- Requires Claude Code (not generic LLMs)
- Tools require JBang + Java 25+
- Skills optimized for Gradle 6+; limited Gradle 5 support
- No direct integration with Gradle Enterprise APIs (yet)

## Next Steps

1. Run `/doctor` on a project
2. Ask about a specific build issue with context
3. Use tools for data-driven analysis

The plugin is most valuable when you provide your actual build files—not hypothetical scenarios.
