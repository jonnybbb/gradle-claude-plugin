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

## Consider OpenRewrite for Large-Scale Migrations

For large codebases or teams requiring repeatable, auditable migrations, consider using [OpenRewrite](https://docs.openrewrite.org/) alongside this plugin:

| Use Case | This Plugin | OpenRewrite |
|----------|-------------|-------------|
| Interactive exploration | Best | - |
| Understanding issues | Best | - |
| Small projects (< 10 modules) | Great | Good |
| Large codebases (100+ modules) | Good | Best |
| CI/CD automated migrations | - | Best |
| Auditable, reproducible changes | - | Best |
| Custom migration rules | - | Best |

**Recommended workflow for large migrations:**

1. Use `/doctor` and `/fix-config-cache --dry-run` to understand what needs fixing
2. Apply [OpenRewrite Gradle recipes](https://docs.openrewrite.org/recipes/gradle) for bulk transformations:
   ```bash
   # Example: Migrate to Gradle 8
   ./gradlew rewriteRun -Drewrite.activeRecipes=org.openrewrite.gradle.MigrateToGradle8
   ```
3. Use this plugin for edge cases, custom code, and verification

OpenRewrite excels at deterministic, large-scale refactoring. This plugin excels at interactive guidance, analysis, and handling nuanced cases that require context.

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

### Disabling Hooks

To disable automatic warnings, create `.claude/gradle-plugin.local.md` in your project:

```markdown
---
hooks:
  enabled: true        # Set to false to disable all hooks
  sessionStart: true   # Warnings when opening project
  postToolUse: true    # Warnings when editing build files
---
```

See [hooks/settings.template.md](hooks/settings.template.md) for the full template.

## Develocity Integration

If your team uses [Develocity](https://gradle.com/develocity/) (formerly Gradle Enterprise), the plugin can:
- Query build success rates and failure patterns
- Analyze cache hit rates and performance trends
- Identify flaky tests across builds
- Include Develocity data in `/doctor` reports

**Setup**: See [Develocity setup guide](skills/develocity/references/setup.md)

## Installation

### Option 1: Claude Marketplace (Recommended)

Install directly from the Claude Code marketplace:

```bash
claude plugin install gradle-claude-plugin
```

Or search for "gradle-claude-plugin" in the Claude Code marketplace UI.

### Option 2: Global Manual Installation (All Projects)

Install once to make the plugin available in all your Claude Code sessions:

```bash
# Clone the repository
git clone https://github.com/jonnybbb/gradle-claude-plugin.git ~/gradle-claude-plugin

# Create Claude plugins directory if it doesn't exist
mkdir -p ~/.claude/plugins

# Symlink to Claude Code plugins
ln -s ~/gradle-claude-plugin ~/.claude/plugins/gradle-claude-plugin
```

### Option 3: Project-Specific Installation

Add the plugin to a specific project only:

```bash
# In your project root
mkdir -p .claude/plugins

# Clone as a submodule (recommended for teams)
git submodule add https://github.com/jonnybbb/gradle-claude-plugin.git .claude/plugins/gradle-claude-plugin

# Or symlink to a local clone
ln -s /path/to/gradle-claude-plugin .claude/plugins/gradle-claude-plugin
```

### Option 4: Direct Clone into Project

```bash
# In your project root
mkdir -p .claude/plugins
git clone https://github.com/jonnybbb/gradle-claude-plugin.git .claude/plugins/gradle-claude-plugin
```

Add `.claude/plugins/gradle-claude-plugin` to your `.gitignore` if you don't want to commit it.

### Verify Installation

Restart Claude Code (or start a new session), then run:

```bash
/doctor
```

If the plugin is loaded correctly, you'll see the Gradle health check output.

### Requirements

- **Java 25+** — Required for JBang tools
- **JBang 0.115.0+** — Install via `curl -Ls https://sh.jbang.dev | bash` or `brew install jbang`
- **Gradle 6.0+** — In the projects you want to analyze

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
