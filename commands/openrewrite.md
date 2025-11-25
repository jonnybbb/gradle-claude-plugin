---
description: Run OpenRewrite recipes for automated Gradle migrations and refactoring
argument-hint: <run|dry-run|list|suggest|generate-recipe> [recipe-name] [--options]
allowed-tools: Read, Bash, Glob, Grep
---

Use the gradle-openrewrite skill for OpenRewrite integration guidance.

## Command: /openrewrite

Arguments provided: $ARGUMENTS

Parse the arguments to determine the subcommand:

### Subcommands

1. **run <recipe>** - Apply a recipe to the project
2. **dry-run <recipe>** - Preview changes without applying
3. **list [filter]** - List available recipes (optionally filter by keyword)
4. **suggest** - Analyze project and suggest appropriate recipes
5. **generate-recipe** - Generate custom recipe for project-specific patterns (Phase 3)

### Execution

Based on the subcommand, run the appropriate JBang tool command:

```bash
# For 'run'
jbang tools/openrewrite_runner.java /path/to/project --recipe=<recipe-name>

# For 'dry-run'
jbang tools/openrewrite_runner.java /path/to/project --recipe=<recipe-name> --dry-run

# For 'list'
jbang tools/openrewrite_runner.java /path/to/project --list=<filter>

# For 'suggest'
jbang tools/openrewrite_runner.java /path/to/project --suggest

# For 'generate-recipe'
jbang tools/openrewrite_runner.java /path/to/project --generate-recipe
```

### Generate Custom Recipe (Phase 3)

The `generate-recipe` subcommand scans the project for patterns not covered by standard OpenRewrite recipes and generates a custom `rewrite.yml` file:

```bash
/openrewrite generate-recipe
```

This will:
1. Scan build files and source code for migration patterns
2. Detect project-specific issues (System.getProperty, internal APIs, etc.)
3. Generate `.rewrite/generated-migrations.yml` with custom transformations
4. Mark patterns requiring manual review

**Output includes:**
- Automatable transformations (will be applied by recipe)
- Manual review items (commented in recipe file)
- File locations and line numbers for each pattern

**Workflow:**
```bash
# 1. Generate custom recipe
/openrewrite generate-recipe

# 2. Review the generated recipe
cat .rewrite/generated-migrations.yml

# 3. Preview changes
/openrewrite dry-run com.generated.ProjectMigrations

# 4. Apply changes
/openrewrite run com.generated.ProjectMigrations
```

### Common Recipes

If the user asks for help choosing a recipe, suggest based on their goal:

| Goal | Recipe |
|------|--------|
| Upgrade to Gradle 8 | `org.openrewrite.gradle.MigrateToGradle8` |
| Update Gradle wrapper | `org.openrewrite.gradle.UpdateGradleWrapper` |
| Convert to Kotlin DSL | `org.openrewrite.kotlin.gradle.MigrateToKotlinDsl` |
| Use plugins block | `org.openrewrite.gradle.plugins.MigrateToPluginsBlock` |
| Create version catalog | `org.openrewrite.gradle.MigrateToVersionCatalog` |

### Workflow Recommendation

1. **Always dry-run first**: `/openrewrite dry-run <recipe>`
2. **Review the changes**: Check which files would be modified
3. **Apply if satisfied**: `/openrewrite run <recipe>`
4. **Verify build**: Run `./gradlew build` after changes
5. **Handle edge cases**: Use `/fix-config-cache` for remaining issues

### Error Handling

If the tool fails:
1. Check if the project path is correct
2. Verify Gradle wrapper is present
3. Check if the recipe name is valid (`/openrewrite list`)
4. For Kotlin DSL recipes, ensure rewrite-kotlin dependency is available

### Example Usage

```bash
# Suggest recipes for current project
/openrewrite suggest

# Preview Gradle 8 migration
/openrewrite dry-run org.openrewrite.gradle.MigrateToGradle8

# Apply the migration
/openrewrite run org.openrewrite.gradle.MigrateToGradle8

# List all dependency-related recipes
/openrewrite list dependency

# Update wrapper to specific version
/openrewrite run org.openrewrite.gradle.UpdateGradleWrapper --version=8.14
```

### Output Interpretation

The tool outputs:
- **Files changed**: Number of files modified
- **Changed files list**: Which files were updated
- **Duration**: How long the recipe took
- **Status**: SUCCESS, PREVIEW (dry-run), or FAILED

For dry-run, remember that no actual changes are made - it's safe to run repeatedly.
