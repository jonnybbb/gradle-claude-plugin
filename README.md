# Gradle Claude Plugin

Get expert Gradle help directly in Claude Code. Fix configuration cache issues, optimize slow builds, migrate between Gradle versions, and analyze build health with Develocity—all with commands that analyze your actual project.

## Prerequisites

### Required

- **Java 25+** - Required for JBang tools
- **JBang 0.134.0+** - Script runner for analysis tools
- **Gradle 6.0+** - In the projects you want to analyze

### Optional

- **Develocity** - For build analytics, flaky test detection, and cross-build insights

### Installing JBang

JBang is required to run the analysis tools that power this plugin's diagnostics capabilities.

**macOS (Homebrew):**
```bash
brew install jbangdev/tap/jbang
```

**Linux/macOS (SDKMan):**
```bash
sdk install jbang
```

**Linux/macOS (curl):**
```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

**Windows (Scoop):**
```powershell
scoop bucket add jbangdev https://github.com/jbangdev/scoop-bucket
scoop install jbang
```

**Windows (Chocolatey):**
```powershell
choco install jbang
```

For more installation options, see the [JBang installation guide](https://www.jbang.dev/download/).

### Verify Installation

```bash
# Check JBang
jbang --version

# Check Java
java --version
```

## Installation

Add the marketplace and install the plugin:

```
/plugin marketplace add jonnybbb/gradle-claude-plugin
/plugin install gradle
```

### Verify Installation

After installation, run:

```
/gradle:doctor
```

If the plugin is loaded correctly, you'll see the Gradle health check output.

## What Can You Do?

### Local Build Analysis

#### Run a Health Check
```bash
/gradle:doctor
```
Get a comprehensive analysis of your Gradle project: configuration issues, performance bottlenecks, outdated patterns, and actionable recommendations.

#### Fix Configuration Cache Issues
```bash
/gradle:fix-config-cache --auto      # Detect and fix issues automatically
/gradle:fix-config-cache --dry-run   # Preview changes first
```
Automatically fixes 19+ patterns including `System.getProperty` → `providers.systemProperty`, `tasks.create` → `tasks.register`, and `$buildDir` → `layout.buildDirectory`.

#### Upgrade Gradle Versions
```bash
/gradle:upgrade 9.0 --auto           # Migrate to Gradle 9
/gradle:upgrade 9.0 --dry-run        # See what would change
```
Handles deprecated APIs, task configuration changes, and property migrations between Gradle 7→8→9.

#### Run Large-Scale Migrations with OpenRewrite
```bash
/gradle:migrate 9.0                  # Full migration with OpenRewrite + Claude
/gradle:migrate 9.0 --dry-run        # Preview all changes first
/gradle:openrewrite suggest          # Get recipe recommendations for your project
/gradle:openrewrite run <recipe>     # Run a specific OpenRewrite recipe
```
For large codebases, combines OpenRewrite's deterministic bulk transformations with Claude's context-aware fixes for edge cases.

#### Speed Up Your Build
```bash
/gradle:optimize-performance --auto  # Apply performance improvements
```
Enables parallel execution, build caching, optimizes JVM settings, and replaces eager task patterns.

---

### Develocity Build Analytics

These commands require [Develocity integration](#develocity-integration) to be configured.

#### Get Build Insights
```bash
/gradle:build-insights
```
Get a high-level overview of your build health from Develocity:
- **Success rate** with trend comparison (this week vs last week)
- **Cache hit rate** and performance metrics
- **Flaky tests** detected across builds
- **CI vs Local** environment comparison
- **Actionable recommendations** based on findings

#### Diagnose Specific Issues
```bash
/gradle:diagnose flaky-tests         # Deep-dive into flaky test patterns
/gradle:diagnose outcome-mismatch    # Why CI passes but local fails (or vice versa)
/gradle:diagnose failure-patterns    # Analyze recurring build failures
```

Each diagnose topic launches a specialized agent that:
- Queries your Develocity build history
- Analyzes patterns across multiple builds
- Provides root cause analysis
- Recommends prioritized fixes

**Flaky Tests Analysis:**
- Pareto analysis (which tests cause 80% of flaky failures?)
- Tolerance calculation (is your flakiness level acceptable?)
- Threshold-based recommendations (fix, quarantine, or disable)
- Root cause classification (race conditions, timing, external dependencies)

**Outcome Mismatch Analysis:**
- Side-by-side CI vs LOCAL environment comparison
- Missing environment variables detection
- Tool version differences (Java, Gradle, OS)
- Specific fix recommendations

**Failure Patterns Analysis:**
- Groups similar failures across builds
- Classifies verification vs non-verification failures
- Prioritizes infrastructure issues (they block everyone)
- Tracks failure trends over time

---

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

## Real-World Use Cases

### "Our CI builds take 12 minutes. Help!"

You're staring at a build that's been getting slower every sprint. No one knows why.

```
> /gradle:doctor
```

The plugin analyzes your project and finds: parallel execution disabled, no build cache, eager task creation in 3 plugins, and JVM heap set too low. You run `/gradle:optimize-performance --auto` and watch your build drop to 4 minutes.

**What you get:** A prioritized list of fixes with expected impact, not just generic "enable caching" advice.

---

### "Configuration cache errors everywhere after enabling it"

Your team wants the 30-80% speedup from configuration cache. You enable it and get 47 errors across your multi-module project.

```
> /gradle:fix-config-cache --dry-run
```

The plugin categorizes every issue: 23 `System.getProperty` calls, 12 `Task.project` accesses, 8 `tasks.create` patterns, and 4 custom plugin problems. It shows exactly what each fix looks like before you approve.

```
> /gradle:fix-config-cache --auto
```

47 errors → 0. Build time drops from 45s to 12s on incremental builds.

**What you get:** Pattern-aware transformations that understand Kotlin DSL vs Groovy, not regex-based find-and-replace.

---

### "We need to upgrade from Gradle 7 to 9 for the new Java version"

Your 200-module monorepo is stuck on Gradle 7.6. Management wants Java 21. You have 3 days.

```
> /gradle:migrate 9.0 --dry-run
```

The plugin:
1. Analyzes your project complexity (LARGE: 200 modules, 15 custom plugins)
2. Recommends OpenRewrite for bulk transformations
3. Identifies 340 deprecated API usages and 28 breaking changes
4. Shows you exactly what will change before anything happens

```
> /gradle:migrate 9.0
```

OpenRewrite handles the bulk transformations. Claude fixes the edge cases your custom plugins introduce. The build compiles. Tests pass.

**What you get:** A migration that would take weeks done in hours, with verification at each step.

---

### "Our CI has been red a lot lately. What's going on?"

Builds keep failing but no one knows if it's flaky tests, infrastructure issues, or actual code problems.

```
> /gradle:build-insights
```

You get an immediate overview: 23% failure rate (up from 15% last week), 37% of failures are infrastructure issues (dependency resolution, OOM), and 3 flaky tests are causing noise.

```
> /gradle:diagnose failure-patterns
```

The plugin groups 35 failures into 8 patterns and classifies them:
- **Non-verification (fix immediately):** 6 OOM errors, 4 dependency resolution failures
- **Verification (normal priority):** 12 compilation errors from a bad merge, 8 test failures

**What you get:** Prioritized action items—fix the OOM and dependency issues first because they block everyone. The compilation errors affect individual PRs.

---

### "Tests pass locally but fail on CI"

Classic "works on my machine" problem. You've wasted hours trying to reproduce CI failures.

```
> /gradle:diagnose outcome-mismatch
```

The plugin compares your recent local builds against CI builds from Develocity:

```
┌──────────────────────┬─────────────────┬─────────────────┐
│ Variable             │ CI (passing)    │ LOCAL (failing) │
├──────────────────────┼─────────────────┼─────────────────┤
│ CI                   │ "true"          │ (not set)       │
│ DATABASE_URL         │ "jdbc:..."      │ (not set)       │
│ Java version         │ 21.0.2          │ 17.0.1          │
└──────────────────────┴─────────────────┴─────────────────┘
```

**What you get:** The exact environment differences causing the mismatch, with specific fix commands.

---

### "We have flaky tests but don't know which ones to fix first"

Your test suite has accumulated flaky tests over time. Fixing all of them would take weeks.

```
> /gradle:diagnose flaky-tests
```

The plugin runs a Pareto analysis:

```
Top 3 tests cause 52% of all flaky failures:

┌─────────────────────────────────────┬────────┬─────────┬────────────┐
│ Test                                │ Flakes │ % Total │ Cumulative │
├─────────────────────────────────────┼────────┼─────────┼────────────┤
│ UserServiceTest.testConcurrentLogin │ 45     │ 28%     │ 28%        │
│ PaymentTest.testTimeout             │ 25     │ 16%     │ 44%        │
│ DbTest.testPoolExhaustion           │ 12     │ 8%      │ 52%        │
└─────────────────────────────────────┴────────┴─────────┴────────────┘

Fixing these 3 tests would eliminate 52% of flaky failures.
```

It also calculates your tolerance threshold and recommends specific actions (fix, quarantine, or disable) based on each test's flakiness rate.

**What you get:** A data-driven prioritization that maximizes impact with minimal effort.

---

### "Why did this task run? It should have been cached!"

You're debugging a flaky cache miss in CI. The task runs on some machines but not others.

```
You: Why does :app:processResources keep running?
```

The plugin analyzes your task's inputs and outputs, checks for absolute paths, non-deterministic inputs, or missing `@PathSensitive` annotations. It finds that a timestamp gets embedded in a generated file, invalidating the cache on every build.

**What you get:** Root cause analysis, not just "check your inputs."

---

### "I need to write a custom Gradle task that's cacheable"

You're implementing a code generator. You want it fast, cacheable, and configuration-cache compatible.

```
> /gradle:create-task GenerateApi worker-api
```

The plugin scaffolds a complete task implementation with:
- Proper `@InputFiles`, `@OutputDirectory` annotations
- Worker API for parallel execution
- `@CacheableTask` with correct path sensitivity
- Configuration cache compatible patterns (no `project` access at execution time)
- A test that verifies cache behavior

**What you get:** Production-ready task code, not a minimal example you'll spend hours debugging.

---

### "I inherited this build and have no idea what's wrong"

New job. 50-module project. Build takes 8 minutes. No documentation. Previous build engineer left.

```
> /gradle:doctor
```

You get a complete health report:
- Gradle 7.4 (outdated, missing 2 years of performance improvements)
- Configuration cache: incompatible (23 issues)
- Build cache: enabled but 34% hit rate (should be 80%+)
- Parallel execution: disabled
- 12 deprecated APIs
- 3 plugins with known performance issues

Each finding links to the fix. You now have a roadmap.

**What you get:** Instant expertise on an unfamiliar codebase.

---

## Who Is This For?

| You are... | This plugin helps you... |
|------------|--------------------------|
| **Java developer** (not a Gradle specialist) | Get structured guidance for unfamiliar build territory |
| **Team adopting configuration cache** | Systematically detect and fix all 16+ issue patterns |
| **Migrating Gradle versions** | Navigate API changes and deprecations between 7→8→9 |
| **Build engineer** | Save time with consistent, repeatable analysis |
| **Team with Develocity** | Get actionable insights from build data without writing queries |

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

This plugin includes built-in [OpenRewrite](https://docs.openrewrite.org/) integration for large-scale migrations. Use `/gradle:migrate` for the best of both worlds:

| Use Case | Recommended Approach |
|----------|---------------------|
| Small projects (< 10 modules) | `/gradle:fix-config-cache --auto` or `/gradle:upgrade --auto` |
| Large codebases (100+ modules) | `/gradle:migrate` (uses OpenRewrite + Claude) |
| CI/CD automated migrations | `/gradle:openrewrite run <recipe>` |
| Understanding issues first | `/gradle:doctor` then `/gradle:openrewrite suggest` |

**How `/gradle:migrate` works:**

1. Analyzes project complexity (SMALL/MEDIUM/LARGE)
2. Recommends strategy: Claude-primary, OpenRewrite-primary, or Hybrid
3. Runs OpenRewrite recipes for bulk transformations
4. Uses Claude for edge cases OpenRewrite can't handle
5. Verifies build compiles and tests pass

```bash
# Full migration workflow
/gradle:migrate 9.0 --dry-run    # Preview everything first
/gradle:migrate 9.0              # Execute the migration
```

**Direct OpenRewrite access:**

```bash
/gradle:openrewrite suggest                    # Recommend recipes for your project
/gradle:openrewrite list gradle                # List available Gradle recipes
/gradle:openrewrite run MigrateToGradle8       # Run a specific recipe
/gradle:openrewrite dry-run MigrateToGradle8   # Preview changes
```

## Develocity Integration

[Develocity](https://gradle.com/develocity/) (formerly Gradle Enterprise) provides build analytics that power the plugin's cross-build analysis features. With Develocity configured, you can:

- **Query build history** — Success rates, failure patterns, performance trends
- **Analyze cache performance** — Hit rates, time saved, optimization opportunities
- **Detect flaky tests** — Identify tests that pass on retry, track flakiness over time
- **Compare environments** — Why does CI pass but local fail?
- **Prioritize fixes** — Focus on issues with the highest impact

### Commands Requiring Develocity

| Command | What It Does |
|---------|--------------|
| `/gradle:build-insights` | Overview of build health with trends |
| `/gradle:diagnose flaky-tests` | Deep analysis of flaky test patterns |
| `/gradle:diagnose outcome-mismatch` | Compare CI vs local environments |
| `/gradle:diagnose failure-patterns` | Analyze recurring build failures |

### Setup

1. **Get a Develocity access key** from your Develocity server (Settings → Access Keys)

2. **Add the Develocity MCP server** to your project:
```bash
claude mcp add --transport http develocity https://develocity.yourcompany.com/mcp \
  --header "Authorization: Bearer YOUR_ACCESS_KEY"
```

3. **Grant the plugin permission** by adding to `.claude/settings.local.json`:
```json
{
  "permissions": {
    "allow": ["mcp__develocity__*", "mcp__drv__*"]
  }
}
```

**Full Setup Guide**: See [Develocity setup guide](skills/develocity/references/setup.md)

### Without Develocity

The plugin works fine without Develocity—you just won't have access to cross-build analytics. Use these commands instead:

| Instead of... | Use... |
|---------------|--------|
| `/gradle:build-insights` | `/gradle:doctor` (local analysis) |
| `/gradle:diagnose flaky-tests` | Run tests with `--rerun-tasks` locally |
| `/gradle:diagnose outcome-mismatch` | Compare local environment manually |

## Automatic Warnings

The plugin watches your work and warns you proactively:

**When you open a Gradle project:**
- Missing parallel execution or build cache settings
- Outdated Gradle versions (< 8.x)
- Eager task creation patterns

**When you edit build files:**
- Configuration cache compatibility issues
- Deprecated patterns like `$buildDir` or `tasks.create`
- Suggests `/gradle:fix-config-cache` when issues are detected

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

## Command Reference

### Local Analysis (No Develocity Required)

| Command | Description |
|---------|-------------|
| `/gradle:doctor` | Comprehensive local build health check |
| `/gradle:fix-config-cache` | Fix configuration cache issues |
| `/gradle:upgrade <version>` | Upgrade Gradle version |
| `/gradle:migrate <version>` | Large-scale migration with OpenRewrite |
| `/gradle:optimize-performance` | Apply performance improvements |
| `/gradle:create-task <name>` | Scaffold a new Gradle task |
| `/gradle:create-plugin <name>` | Scaffold a new Gradle plugin |
| `/gradle:openrewrite <cmd>` | Run OpenRewrite recipes |

### Develocity Analytics (Requires Setup)

| Command | Description |
|---------|-------------|
| `/gradle:build-insights` | Build health overview with trends |
| `/gradle:diagnose flaky-tests` | Analyze flaky test patterns |
| `/gradle:diagnose outcome-mismatch` | Compare CI vs local environments |
| `/gradle:diagnose failure-patterns` | Analyze recurring failures |

## Gradle Version Support

| Version | Support Level |
|---------|---------------|
| Gradle 6.x | Basic analysis |
| Gradle 7.x | Full support |
| Gradle 8.x | Full support |
| Gradle 9.x | Full support |

## Development

### Local Testing

To test the plugin locally, add it as a local marketplace:

```
/plugin marketplace add ./path/to/gradle-claude-plugin
/plugin install gradle
```

### Project-Specific Installation

For teams that want to bundle the plugin with a project:

```bash
mkdir -p .claude/plugins
git submodule add https://github.com/jonnybbb/gradle-claude-plugin.git .claude/plugins/gradle-claude-plugin
```

### Running Tests

```bash
cd tests/fixtures
./scripts/setup-fixtures.sh
./scripts/test-tools.sh
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.
