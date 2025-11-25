# Gradle Claude Plugin

Get expert Gradle help directly in Claude Code. Fix configuration cache issues, optimize slow builds, and migrate between Gradle versions—all with commands that analyze your actual project.

## What Can You Do?

### Run a Health Check
```bash
/doctor
```
Get a comprehensive analysis of your Gradle project: configuration issues, performance bottlenecks, outdated patterns, and actionable recommendations.

### Fix Configuration Cache Issues
```bash
/fix-config-cache --auto      # Detect and fix issues automatically
/fix-config-cache --dry-run   # Preview changes first
```
Automatically fixes 19+ patterns including `System.getProperty` → `providers.systemProperty`, `tasks.create` → `tasks.register`, and `$buildDir` → `layout.buildDirectory`.

### Upgrade Gradle Versions
```bash
/upgrade 9.0 --auto           # Migrate to Gradle 9
/upgrade 9.0 --dry-run        # See what would change
```
Handles deprecated APIs, task configuration changes, and property migrations between Gradle 7→8→9.

### Speed Up Your Build
```bash
/optimize-performance --auto  # Apply performance improvements
```
Enables parallel execution, build caching, optimizes JVM settings, and replaces eager task patterns.

### Ask About Anything Gradle

The plugin includes expertise on:
- **Configuration cache** — Enable it, fix incompatibilities
- **Build performance** — Diagnose slow builds, tune JVM/parallelism
- **Dependencies** — Version catalogs, conflict resolution, platforms
- **Task development** — Cacheable tasks, Worker API, annotations
- **Plugin development** — Extensions, testing, publishing
- **Multi-project builds** — Composite builds, included builds
- **Troubleshooting** — Common errors and debugging techniques

Just ask naturally: "Why is my build slow?" or "How do I make this task cacheable?"

## Automatic Warnings

The plugin watches your work and warns you proactively:

**When you open a Gradle project:**
- Missing parallel execution or build cache settings
- Outdated Gradle versions (< 8.x)
- Eager task creation patterns

**When you edit build files:**
- Configuration cache compatibility issues
- Deprecated patterns like `$buildDir` or `tasks.create`
- Suggests `/fix-config-cache` when issues are detected

## Develocity Integration

If your team uses [Develocity](https://gradle.com/develocity/) (formerly Gradle Enterprise), the plugin can:
- Query build success rates and failure patterns
- Analyze cache hit rates and performance trends
- Identify flaky tests across builds
- Include Develocity data in `/doctor` reports

**Setup**: See [Develocity setup guide](skills/develocity/references/setup.md)

## Installation

```bash
# Clone and symlink to Claude Code plugins
git clone https://github.com/jonnybbb/gradle-claude-plugin.git
ln -s $(pwd)/gradle-claude-plugin ~/.claude/plugins/gradle-claude-plugin
```

**Requirements**: Java 25+, JBang 0.115.0+

## Who Is This For?

| You are... | This plugin helps you... |
|------------|--------------------------|
| **Java developer** (not a Gradle specialist) | Get structured guidance for unfamiliar build territory |
| **Team adopting configuration cache** | Systematically detect and fix all 16+ issue patterns |
| **Migrating Gradle versions** | Navigate API changes and deprecations between 7→8→9 |
| **Build engineer** | Save time with consistent, repeatable analysis |

**Honest note**: If you're a Gradle expert, vanilla Claude Code with good prompting gets you 80% there. This plugin adds tested accuracy, structured workflows, and Tooling API introspection.

## How It Works

The plugin uses JBang tools that connect to Gradle's Tooling API—so it analyzes your actual project model, not just text patterns:

```bash
# Analyze project structure programmatically
jbang tools/gradle-analyzer.java /path/to/project --json

# Validate configuration cache compatibility
jbang tools/cache-validator.java /path/to/project

# Profile build performance
jbang tools/performance-profiler.java /path/to/project
```

Skills and recommendations are tested against real Gradle projects with known issues to ensure accuracy.

## Gradle Version Support

| Version | Support Level |
|---------|---------------|
| Gradle 6.x | Basic analysis |
| Gradle 7.x | Full analysis |
| Gradle 8.x | Recommended |
| Gradle 9.x | Full support |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT
