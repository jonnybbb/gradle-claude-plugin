---
description: Orchestrate a complete Gradle migration with intelligent engine selection
argument-hint: <target-version> [--full] [--dry-run] [--no-verify]
allowed-tools: Read, Edit, Write, Glob, Grep, Bash, AskUserQuestion, Task
---

# Gradle Migration Command

You are executing a comprehensive Gradle migration. This command uses the migration-orchestrator-agent for complex migrations.

Arguments provided: $ARGUMENTS

## Arguments

- `<target-version>`: Target Gradle version (e.g., 9.0, 8.14)
- `--full`: Full migration including best practices (Kotlin DSL, plugins block, version catalog)
- `--dry-run`: Plan the migration without applying changes
- `--no-verify`: Skip verification steps (not recommended)

## Workflow

### Step 1: Analyze Project

First, gather project metrics to determine the optimal migration strategy:

```bash
# Count modules and build files
echo "=== Project Analysis ==="
echo "Build files:"
find . -name "build.gradle*" -not -path "*/build/*" -not -path "*/.gradle/*" | wc -l

echo "Settings files:"
find . -name "settings.gradle*" -not -path "*/build/*" | head -1

echo "Current Gradle version:"
grep distributionUrl gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sed 's/.*gradle-//' | sed 's/-.*//'
```

### Step 2: Determine Strategy

Based on project size:

| Project Size | Strategy |
|--------------|----------|
| Small (< 10 modules) | Claude-driven with OpenRewrite support |
| Medium (10-50 modules) | Hybrid: OpenRewrite bulk + Claude edge cases |
| Large (> 50 modules) | OpenRewrite primary + Claude fallback |

### Step 3: Generate Migration Plan

```bash
# Get suggestions
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . --suggest --json

# Generate custom recipe for edge cases
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . --generate-recipe
```

Display the plan to the user:

```
═══════════════════════════════════════════════════════════════
                    MIGRATION PLAN
═══════════════════════════════════════════════════════════════

Target: Gradle <target-version>
Strategy: [Based on analysis]

Steps:
1. Create git checkpoint
2. Update Gradle wrapper
3. Run OpenRewrite migrations
4. Apply custom recipe
5. Fix edge cases with Claude
6. Verify build
7. Run tests (optional)

Proceed? [Y/n]
═══════════════════════════════════════════════════════════════
```

### Step 4: Execute Migration

If `--dry-run` is NOT set:

#### 4.1 Create Checkpoint
```bash
git add -A
git stash push -m "pre-migration-checkpoint"
```

#### 4.2 Update Wrapper
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
  --recipe=org.openrewrite.gradle.UpdateGradleWrapper \
  --dry-run

# If looks good:
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
  --recipe=org.openrewrite.gradle.UpdateGradleWrapper
```

#### 4.3 Run Bulk Migrations
```bash
# For Gradle 8 migration
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
  --recipe=org.openrewrite.gradle.MigrateToGradle8

# For Gradle 9 migration (when available)
# jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
#   --recipe=org.openrewrite.gradle.MigrateToGradle9
```

#### 4.4 Apply Custom Recipe
```bash
# If .rewrite/generated-migrations.yml exists:
jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
  --recipe=com.generated.ProjectMigrations
```

#### 4.5 Fix Edge Cases
For patterns that OpenRewrite couldn't fix:
- Read each file with issues
- Apply fixes using Edit tool
- Verify each fix

#### 4.6 Verify
```bash
# Compile check
./gradlew assemble --no-build-cache 2>&1 | tail -20

# Configuration cache check (if not --no-verify)
./gradlew help --configuration-cache 2>&1 | tail -10
```

### Step 5: Report Results

```
═══════════════════════════════════════════════════════════════
                    MIGRATION COMPLETE
═══════════════════════════════════════════════════════════════

Status: SUCCESS

Summary:
  - Files modified: X
  - OpenRewrite fixes: Y
  - Claude fixes: Z

Verification:
  ✓ Build compiles
  ✓ Configuration cache compatible

Next steps:
  - Review changes: git diff
  - Run tests: ./gradlew test
  - Commit: git commit -m "Migrate to Gradle <version>"
═══════════════════════════════════════════════════════════════
```

## Full Migration Mode (--full)

When `--full` is specified, also include:

1. **Kotlin DSL Migration**
   ```bash
   jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
     --recipe=org.openrewrite.kotlin.gradle.MigrateToKotlinDsl \
     --additional-deps=org.openrewrite.recipe:rewrite-kotlin:LATEST
   ```

2. **Plugins Block Migration**
   ```bash
   jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
     --recipe=org.openrewrite.gradle.plugins.MigrateToPluginsBlock
   ```

3. **Version Catalog Migration**
   ```bash
   jbang ${CLAUDE_PLUGIN_ROOT}/tools/openrewrite_runner.java . \
     --recipe=org.openrewrite.gradle.MigrateToVersionCatalog
   ```

## Error Recovery

If any step fails:

1. Show the error
2. Offer options:
   - Retry
   - Skip and continue
   - Rollback to checkpoint
   - Abort

```bash
# Rollback command
git stash pop
```

## Example Usage

```bash
# Basic migration to Gradle 9
/migrate 9.0

# Full migration with all best practices
/migrate 9.0 --full

# Preview migration plan only
/migrate 9.0 --dry-run

# Migration without verification (faster but riskier)
/migrate 9.0 --no-verify
```

## Related Commands

- `/openrewrite suggest` - See recommended recipes
- `/openrewrite generate-recipe` - Generate custom recipe
- `/fix-config-cache` - Fix remaining configuration cache issues
- `/doctor` - Verify build health after migration
