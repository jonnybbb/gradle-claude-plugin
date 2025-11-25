# Gradle Expert Plugin for Claude Code

A Claude Code plugin that provides Gradle expertise through curated skills, JBang analysis tools, and workflow agents.

## Who Benefits Most

| User Type | Value | Why |
|-----------|-------|-----|
| **Java developers** (not Gradle specialists) | High | Structured guidance for unfamiliar territory |
| **Teams adopting config cache** | High | Systematic detection and fixes for all 16+ issue patterns |
| **Gradle version migrations** | Medium-High | API change mappings, deprecation fixes |
| **Build engineers** | Medium | Time savings, consistency—but you likely know most of this |

**Honest assessment**: If you're a Gradle expert, vanilla Claude Code with good prompting gets you 80% there. This plugin provides the other 20%: tested accuracy, structured workflows, and programmatic introspection.

## What Sets This Apart

### 1. JBang Tools with Gradle Tooling API
Unlike CLI commands, these provide **programmatic introspection**:

```bash
# Actual project model analysis (not just text parsing)
jbang tools/gradle-analyzer.java /path/to/project --json

# Configuration cache issue detection
jbang tools/cache-validator.java /path/to/project

# Task dependency and performance analysis
jbang tools/task-analyzer.java /path/to/project
```

### 2. Tested Accuracy
Skills are validated against test fixtures with documented issues. The AI responses are tested to ensure they actually detect problems—not just generate plausible-sounding advice.

### 3. Structured Workflows
The `/doctor` command runs a systematic health check. You don't need to know what questions to ask.

## Active Automation (NEW)

The plugin now includes **auto-fix capabilities** for common Gradle issues:

```bash
# Detect and fix config cache issues
/fix-config-cache --auto

# Upgrade to newer Gradle version
/upgrade 9.0 --auto

# Optimize build performance
/optimize-performance --auto

# Preview changes without applying
/fix-config-cache --dry-run
/upgrade 9.0 --dry-run
/optimize-performance --dry-run
```

### Configuration Cache Fixes

The `config-cache-fixer.java` tool detects 19+ issue patterns:

| Category | Examples | Confidence |
|----------|----------|------------|
| Provider API | `System.getProperty` → `providers.systemProperty` | HIGH (auto) |
| Task Avoidance | `tasks.create` → `tasks.register` | HIGH (auto) |
| Deprecated API | `$buildDir` → `layout.buildDirectory` | HIGH (auto) |
| Service Injection | `project.copy` → injected `FileSystemOperations` | MEDIUM (manual) |

### Migration Fixes

The `migration-fixer.java` tool handles Gradle version migrations (7→8, 8→9):

| Category | Examples | Confidence |
|----------|----------|------------|
| Deprecated Properties | `archivesBaseName` → `base { archivesName }` | HIGH (auto) |
| Task Avoidance | `task name { }` → `tasks.register("name") { }` | HIGH (auto) |
| API Changes | `mainClassName` → `mainClass` | HIGH (auto) |
| Convention Deprecations | Top-level `sourceCompatibility` → `java { }` block | MEDIUM (manual) |

### Performance Fixes

The `performance-fixer.java` tool identifies performance optimizations:

| Category | Examples | Impact |
|----------|----------|--------|
| Parallel | `org.gradle.parallel=true` | HIGH |
| Caching | `org.gradle.caching=true` | HIGH |
| JVM Args | `-Xmx2g -XX:+UseG1GC` | MEDIUM |
| Task Avoidance | `tasks.all` → `tasks.configureEach` | MEDIUM |

See [ROADMAP.md](ROADMAP.md) for the full automation roadmap (all phases complete).

## Proactive Hooks (Phase 6)

The plugin automatically detects Gradle projects and validates build file changes:

### SessionStart Hook

When you open a Gradle project, the plugin:
- Detects if parallel execution and build cache are enabled
- Checks for eager task creation patterns
- Warns about outdated Gradle versions (< 8.x)
- Suggests relevant commands (`/doctor`, `/optimize-performance`)

### PostToolUse Hook

After editing build files (`build.gradle`, `settings.gradle`, etc.), the plugin:
- Validates for configuration cache compatibility issues
- Warns about deprecated patterns (`$buildDir`, `tasks.create`)
- Suggests `/fix-config-cache` for automatic fixes

## Remaining Limitations

- **Integrate with Gradle Enterprise** — No build scan data integration
- **Remember project context** — Each session starts fresh

Contributions welcome for remaining features.

## Quick Start

### Requirements

- Java 25+ (for JBang tools)
- JBang 0.115.0+
- Gradle 6.0+ (for analyzed projects)

### Installation

```bash
# Clone and symlink to Claude Code plugins
git clone https://github.com/your-org/claude-gradle-plugin.git
ln -s $(pwd)/claude-gradle-plugin ~/.claude/plugins/gradle-expert
```

### Usage

```bash
# Comprehensive health check
/doctor

# Detect and auto-fix configuration cache issues
/fix-config-cache --auto

# Validate configuration cache compatibility
jbang tools/cache-validator.java /path/to/project

# Generate structured fix plan (JSON output for tooling)
jbang tools/config-cache-fixer.java /path/to/project --json

# Analyze project structure
jbang tools/gradle-analyzer.java /path/to/project --json

# Profile build performance
jbang tools/performance-profiler.java /path/to/project
```

## Skills (10)

| Skill | Use Case |
|-------|----------|
| `gradle-config-cache` | Enable configuration cache, fix incompatibilities |
| `gradle-performance` | Diagnose slow builds, tune JVM/parallelism |
| `gradle-dependencies` | Version catalogs, conflict resolution, platforms |
| `gradle-task-development` | Cacheable tasks, Worker API, annotations |
| `gradle-plugin-development` | Extensions, testing, publishing |
| `gradle-migration` | API changes between major versions |
| `gradle-build-cache` | Remote cache setup, CI/CD patterns |
| `gradle-structure` | Multi-project layouts, composite builds |
| `gradle-troubleshooting` | Common errors, debugging techniques |
| `gradle-doctor` | Holistic project health assessment |

## Tools (9)

All tools use Gradle Tooling API for accurate introspection:

| Tool | Purpose |
|------|---------|
| `gradle-analyzer.java` | Project structure, plugins, dependencies |
| `cache-validator.java` | Configuration cache compatibility |
| `config-cache-fixer.java` | Generate config cache fix plans |
| `migration-fixer.java` | Generate migration fix plans (7→8, 8→9) |
| `performance-fixer.java` | Generate performance optimization plans |
| `task-analyzer.java` | Task inputs/outputs, cacheability |
| `performance-profiler.java` | Build timing, bottlenecks |
| `build-health-check.java` | Overall project health |
| `quick-validate.java` | Fast build file validation (for hooks) |

## Test Fixtures

Five projects for validation:

| Fixture | Purpose | Gradle | Java |
|---------|---------|--------|------|
| `simple-java` | Healthy baseline | 9.2.1 | 25 |
| `config-cache-broken` | 16 documented issues | 9.2.1 | — |
| `multi-module` | Scale testing (4 modules) | 9.2.1 | — |
| `spring-boot` | Framework compatibility | 9.2.1 | 25 |
| `legacy-groovy` | Migration testing | 7.6.4 | 11 |

## Architecture

```
skills/           → User-facing skill definitions (10 SKILL.md files)
agents/           → Workflow orchestration (doctor.md, migration.md)
tools/            → JBang scripts with Gradle Tooling API
docs/reference/   → Curated documentation (skills link here)
test-fixtures/    → Projects for validation
tests/            → JUnit tests including AI-powered accuracy tests
```

## Comparison With Vanilla AI

| Aspect | Vanilla Claude | With This Plugin |
|--------|---------------|------------------|
| Knowledge accuracy | Variable | Tested against fixtures |
| Issue detection | Pattern matching | Tooling API introspection |
| Workflow structure | User-driven | Pre-built systematic checks |
| Consistency | Depends on prompting | Repeatable, validated |

## Version Support

- **Gradle 6.x**: Basic analysis
- **Gradle 7.x**: Full analysis
- **Gradle 8.x**: Recommended
- **Gradle 9.x**: Full support (default for fixtures)

## Contributing

The plugin would benefit most from:
1. **Auto-fix capabilities** — Detect AND fix issues
2. **Gradle Enterprise integration** — Pull build scan data
3. **Automatic tool invocation** — Skills that run tools, not just inform
4. **Project memory** — Remember issues across sessions

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT
