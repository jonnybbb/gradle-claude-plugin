---
name: gradle-openrewrite
description: This skill should be used when the user asks about "OpenRewrite integration", "automated refactoring", "large-scale migrations", "recipe-based transformations", "bulk code changes", "deterministic refactoring", or wants to use OpenRewrite recipes for Gradle migrations instead of Claude-driven transformations.
---

# OpenRewrite Integration

## Overview

OpenRewrite provides deterministic, large-scale code transformations using recipes. This plugin integrates OpenRewrite as an opt-in execution engine for migrations and refactoring.

For recipe catalog, see [references/recipe-catalog.md](references/recipe-catalog.md).
For custom recipes, see [references/custom-recipes.md](references/custom-recipes.md).

## When to Use OpenRewrite vs Claude

| Scenario | Recommended Engine |
|----------|-------------------|
| Small project (< 10 modules) | Claude (interactive) |
| Large project (100+ modules) | OpenRewrite (bulk) |
| Need reproducible changes | OpenRewrite |
| Need CI/CD automation | OpenRewrite |
| Complex/nuanced edge cases | Claude |
| Exploratory fixes | Claude |
| Auditable transformations | OpenRewrite |

## Quick Start

### Run a Recipe

```bash
# Dry-run first (preview changes)
/openrewrite dry-run org.openrewrite.gradle.MigrateToGradle8

# Apply changes
/openrewrite run org.openrewrite.gradle.MigrateToGradle8
```

### Use with Existing Commands

```bash
# Use OpenRewrite engine for config cache fixes
/fix-config-cache --engine=openrewrite

# Use OpenRewrite for version upgrades
/upgrade 9.0 --engine=openrewrite
```

### List Available Recipes

```bash
/openrewrite list                 # All recipes
/openrewrite list gradle          # Gradle-specific
/openrewrite suggest              # Based on project analysis
```

### Generate Custom Recipe (Phase 3)

For project-specific patterns not covered by standard recipes:

```bash
# Scan project and generate custom recipe
/openrewrite generate-recipe

# Output: .rewrite/generated-migrations.yml
```

### Analyze Project for Migration (Phase 4)

For comprehensive migration planning:

```bash
# Analyze project complexity and get migration recommendations
jbang tools/openrewrite_runner.java . --analyze

# Full migration with intelligent engine selection
/migrate 9.0

# Full migration with all best practices (Kotlin DSL, plugins block, version catalog)
/migrate 9.0 --full

# Preview migration plan without applying
/migrate 9.0 --dry-run
```

This detects:
- System.getProperty/getenv in configuration
- Internal API usage (org.gradle.*.internal.*)
- Eager task configuration (tasks.getByName)
- Direct buildDir access
- Convention API usage
- Project file operations in task actions

Then generates a custom `rewrite.yml` with:
- Automatable transformations
- Manual review items (commented)

## Common Gradle Recipes

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.gradle.MigrateToGradle8` | Migrate deprecated APIs to Gradle 8 |
| `org.openrewrite.gradle.UpdateGradleWrapper` | Update wrapper to specific version |
| `org.openrewrite.gradle.MigrateToVersionCatalog` | Convert dependencies to version catalog |
| `org.openrewrite.kotlin.gradle.MigrateToKotlinDsl` | Convert Groovy DSL to Kotlin DSL |
| `org.openrewrite.gradle.plugins.MigrateToPluginsBlock` | Convert apply plugin to plugins block |

## Configuration

Configure OpenRewrite integration in `.claude/gradle-plugin.local.md`:

```yaml
---
openrewrite:
  enabled: true
  defaultEngine: false      # true = use OpenRewrite by default
  cliVersion: "latest"      # or pin: "2.21.0"
  additionalRecipes:        # extra recipe dependencies
    - "org.openrewrite.recipe:rewrite-spring:LATEST"
  failOnDryRunResults: true
---
```

## How It Works

The plugin uses a hybrid approach:

1. **Gradle Tooling API** for orchestration:
   - Detects Gradle version
   - Understands project structure
   - Monitors progress

2. **Init Script Injection** for execution:
   - Generates temporary init script
   - Applies OpenRewrite Gradle plugin
   - Executes requested recipes
   - No modification to existing build files

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Plugin Command │ ──▶ │  Tooling API     │ ──▶ │  Init Script    │
│  /openrewrite   │     │  (orchestration) │     │  (execution)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
```

## Recipe Categories

### Migration Recipes
- `MigrateToGradle8` - Deprecated API migrations
- `MigrateToGradle7` - Gradle 6 to 7 migrations
- `UpdateGradleWrapper` - Wrapper version updates

### Best Practices
- `MigrateToPluginsBlock` - Modern plugin application
- `MigrateToVersionCatalog` - Centralized dependencies
- `MigrateToKotlinDsl` - Kotlin DSL conversion

### Search Recipes (Analysis Only)
- `FindGradleProject` - Find Gradle projects
- `FindPlugins` - Find plugin usage
- `FindDependency` - Find dependency declarations

## Troubleshooting

### Recipe Not Found

```bash
# Check if recipe exists
/openrewrite list | grep MigrateToGradle

# May need additional recipe dependency
# Add to .claude/gradle-plugin.local.md:
# additionalRecipes:
#   - "org.openrewrite.recipe:rewrite-gradle:LATEST"
```

### Configuration Cache Conflict

OpenRewrite plugin may not be configuration cache compatible:

```bash
# Plugin automatically adds --no-configuration-cache when needed
```

### Partial Application

Some recipes may not transform all patterns:

```bash
# Run OpenRewrite first for bulk changes
/openrewrite run org.openrewrite.gradle.MigrateToGradle8

# Then use Claude for remaining edge cases
/fix-config-cache --auto
```

## Migration Orchestrator (Phase 4)

For complex, multi-step migrations, use the migration orchestrator:

```bash
/migrate <target-version> [--full] [--dry-run] [--no-verify]
```

The orchestrator:
1. **Analyzes** project complexity (SMALL/MEDIUM/LARGE)
2. **Selects** optimal engine:
   - Small projects (< 10 modules): Claude primary
   - Large projects (> 50 modules): OpenRewrite primary
   - Medium projects: Hybrid approach
3. **Plans** migration steps with estimates
4. **Executes** with verification at each step
5. **Reports** comprehensive results

### Engine Selection Logic

| Project Size | Strategy |
|--------------|----------|
| Small (< 10 modules) | Claude primary + OpenRewrite optional |
| Medium (10-50 modules) | Hybrid: OpenRewrite bulk + Claude edge cases |
| Large (> 50 modules) | OpenRewrite primary + Claude fallback |

### Example Output

```
═══════════════════════════════════════════════════════════════
                    PROJECT ANALYSIS
═══════════════════════════════════════════════════════════════

Complexity: MEDIUM
Recommended Strategy: Hybrid: OpenRewrite bulk + Claude edge cases

Migration Plan:
┌────┬──────────────────────────────────────┬────────────┬───────────┐
│ #  │ Step                                 │ Engine     │ Est. Time │
├────┼──────────────────────────────────────┼────────────┼───────────┤
│ 1  │ Create git checkpoint                │ Git        │ < 1 min   │
│ 2  │ Update Gradle wrapper                │ OpenRw     │ < 1 min   │
│ 3  │ Migrate deprecated APIs (Gradle 8)   │ OpenRw     │ 2-5 min   │
│ 4  │ Fix 12 edge cases                    │ Claude     │ 5-10 min  │
│ 5  │ Verify build                         │ Gradle     │ 2-5 min   │
└────┴──────────────────────────────────────┴────────────┴───────────┘
```

## Related Resources

- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [Gradle Recipes](https://docs.openrewrite.org/recipes/gradle)
- [Recipe Catalog](references/recipe-catalog.md)
- [Writing Custom Recipes](references/custom-recipes.md)
