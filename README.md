# Gradle Claude Plugin

Get expert Gradle help directly in Claude Code. Fix configuration cache issues, optimize slow builds, and migrate between Gradle versions—all with commands that analyze your actual project.

## Installation

### Option 1: From GitHub (Recommended)

Install directly from the GitHub repository:

```
/plugin install jonnybbb/gradle-claude-plugin
```

Or use the interactive menu:
```
/plugin
```
Then select "Add Plugin" and enter `jonnybbb/gradle-claude-plugin`.

### Option 2: Project-Specific Installation

Add the plugin to a specific project only:

```bash
# In your project root
mkdir -p .claude/plugins

# Clone as a submodule (recommended for teams)
git submodule add https://github.com/jonnybbb/gradle-claude-plugin.git .claude/plugins/gradle-claude-plugin

# Or clone directly
git clone https://github.com/jonnybbb/gradle-claude-plugin.git .claude/plugins/gradle-claude-plugin
```

Add `.claude/plugins/gradle-claude-plugin` to your `.gitignore` if you don't want to commit it.

### Option 3: Local Development

For local development or testing:

```
/plugin marketplace add ./path/to/gradle-claude-plugin
```

### Verify Installation

After installation, run:

```
/doctor
```

If the plugin is loaded correctly, you'll see the Gradle health check output.

### Requirements

- **Java 25+** — Required for JBang tools
- **JBang 0.115.0+** — Install via `curl -Ls https://sh.jbang.dev | bash` or `brew install jbang`
- **Gradle 6.0+** — In the projects you want to analyze

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

### Run Large-Scale Migrations with OpenRewrite
```bash
/migrate 9.0                  # Full migration with OpenRewrite + Claude
/migrate 9.0 --dry-run        # Preview all changes first
/openrewrite suggest          # Get recipe recommendations for your project
/openrewrite run <recipe>     # Run a specific OpenRewrite recipe
```
For large codebases, combines OpenRewrite's deterministic bulk transformations with Claude's context-aware fixes for edge cases.

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

# Run OpenRewrite recipes via Tooling API
jbang tools/openrewrite_runner.java /path/to/project --analyze --json
```

Skills and recommendations are tested against real Gradle projects with known issues to ensure accuracy.

## OpenRewrite Integration

This plugin includes built-in [OpenRewrite](https://docs.openrewrite.org/) integration for large-scale migrations. Use `/migrate` for the best of both worlds:

| Use Case | Recommended Approach |
|----------|---------------------|
| Small projects (< 10 modules) | `/fix-config-cache --auto` or `/upgrade --auto` |
| Large codebases (100+ modules) | `/migrate` (uses OpenRewrite + Claude) |
| CI/CD automated migrations | `/openrewrite run <recipe>` |
| Understanding issues first | `/doctor` then `/openrewrite suggest` |

**How `/migrate` works:**

1. Analyzes project complexity (SMALL/MEDIUM/LARGE)
2. Recommends strategy: Claude-primary, OpenRewrite-primary, or Hybrid
3. Runs OpenRewrite recipes for bulk transformations
4. Uses Claude for edge cases OpenRewrite can't handle
5. Verifies build compiles and tests pass

```bash
# Full migration workflow
/migrate 9.0 --dry-run    # Preview everything first
/migrate 9.0              # Execute the migration
```

**Direct OpenRewrite access:**

```bash
/openrewrite suggest                    # Recommend recipes for your project
/openrewrite list gradle                # List available Gradle recipes
/openrewrite run MigrateToGradle8       # Run a specific recipe
/openrewrite dry-run MigrateToGradle8   # Preview changes
```

## Develocity Integration

If your team uses [Develocity](https://gradle.com/develocity/) (formerly Gradle Enterprise), the plugin can:
- Query build success rates and failure patterns
- Analyze cache hit rates and performance trends
- Identify flaky tests across builds
- Include Develocity data in `/doctor` reports

**Setup** (optional):

1. Add the Develocity MCP server to your project:
```bash
claude mcp add --transport http develocity https://dv.yourcompany.com/mcp \
  --header "Authorization: Bearer YOUR_ACCESS_KEY"
```

2. Grant the plugin permission to use it by adding to `.claude/settings.local.json`:
```json
{
  "permissions": {
    "allow": ["mcp__develocity__*"]
  }
}
```

**Full Setup Guide**: See [Develocity setup guide](skills/develocity/references/setup.md)

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

Apache License 2.0 - see [LICENSE](LICENSE) for details.
