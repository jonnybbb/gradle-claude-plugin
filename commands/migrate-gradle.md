---
description: Migrate Gradle project to a newer version with automatic fixes
argument-hint: "<target-version> [--auto] [--dry-run]"
allowed-tools: Read, Edit, Glob, Grep, Bash, AskUserQuestion
---

# Gradle Migration Workflow

You are running the Gradle migration workflow. This command analyzes the project for deprecated APIs and helps migrate to a newer Gradle version.

## Arguments

Parse the command arguments:
- `<target-version>`: Target Gradle version (e.g., 9.0, 8.11)
- `--auto`: Automatically apply all HIGH confidence fixes without prompting
- `--dry-run`: Show what would be fixed without making changes
- (default): Interactive mode - review each fix before applying

## Workflow Steps

### Step 1: Detect Current State

Run the migration-fixer tool to analyze the project:

```bash
jbang tools/migration-fixer.java . --target <VERSION> --json
```

Parse the JSON output to understand:
- Current Gradle version
- Migration path (e.g., 7→8 → 8→9)
- All deprecated APIs and their replacements
- Confidence levels for each fix

### Step 2: Display Migration Summary

Present a clear summary:

```
═══════════════════════════════════════════════════════════════
              GRADLE MIGRATION PLAN
═══════════════════════════════════════════════════════════════

Current Version: 7.6.4
Target Version:  9.0
Migration Path:  7 → 8 → 9

┌─────────────────────────┬───────┬─────────────┐
│ Category                │ Count │ Auto-fixable│
├─────────────────────────┼───────┼─────────────┤
│ DEPRECATED_PROPERTY     │     6 │ ✓ 6         │
│ TASK_AVOIDANCE          │     7 │ ✓ 7         │
│ DEPRECATED_API          │     4 │ ✓ 4         │
│ CONVENTION_DEPRECATION  │     2 │ ✗ 0         │
│ PERFORMANCE             │     3 │ ✓ 2         │
├─────────────────────────┼───────┼─────────────┤
│ Total                   │    22 │ 19          │
└─────────────────────────┴───────┴─────────────┘

═══════════════════════════════════════════════════════════════
```

### Step 3: Apply Fixes by Migration Step

Group and apply fixes by migration step (7→8, then 8→9):

**For each step:**
1. Show fixes relevant to that version upgrade
2. Apply in order: HIGH confidence first, then MEDIUM
3. Track progress

```
── Gradle 7 → 8 Migration ──

[1/17] MIG-001: archivesBaseName → base { archivesName }
       File: build.gradle:14
       ✓ Applied

[2/17] MIG-002: mainClassName → mainClass
       File: build.gradle:30
       ✓ Applied

... continue for all 7→8 fixes ...

── Gradle 8 → 9 Migration ──

[18/22] MIG-016: sourceCompatibility convention
        File: build.gradle:82
        ⚠ MEDIUM confidence - review recommended

        Show diff? [Y/n]
```

### Step 4: Update Gradle Wrapper

After applying fixes, offer to update the wrapper:

```
═══════════════════════════════════════════════════════════════
               WRAPPER UPDATE
═══════════════════════════════════════════════════════════════

Ready to update Gradle wrapper from 7.6.4 to 9.0?

Command: ./gradlew wrapper --gradle-version 9.0

[Y]es / [N]o / [C]ustom version
```

If yes, run:
```bash
./gradlew wrapper --gradle-version <VERSION>
```

### Step 5: Verification Build

After updating wrapper, run a verification build:

```bash
./gradlew build --warning-mode all 2>&1
```

Report results:
- Success: Migration complete!
- Warnings: List deprecation warnings for future attention
- Errors: Help debug and suggest fixes

### Step 6: Final Summary

```
═══════════════════════════════════════════════════════════════
               MIGRATION COMPLETE
═══════════════════════════════════════════════════════════════

✓ Migrated from Gradle 7.6.4 → 9.0

Applied fixes: 19
  • 6 deprecated properties updated
  • 7 tasks converted to lazy registration
  • 4 buildDir usages replaced
  • 2 performance settings added

Skipped: 3 (MEDIUM confidence, manual review needed)
  • sourceCompatibility convention (line 82)
  • targetCompatibility convention (line 83)
  • configuration-cache setting

Wrapper: ✓ Updated to 9.0
Build: ✓ Successful

Next steps:
  1. Review skipped MEDIUM confidence items
  2. Run full test suite: ./gradlew check
  3. Consider enabling configuration cache
  4. See gradle-migration skill for detailed guidance

═══════════════════════════════════════════════════════════════
```

## Migration Patterns Reference

### Gradle 7 → 8

| Deprecated | Replacement |
|------------|-------------|
| `archivesBaseName` | `base { archivesName }` |
| `mainClassName` | `mainClass` |
| `archiveName` | `archiveFileName` |
| `destinationDir` | `destinationDirectory` |
| `task name { }` | `tasks.register("name") { }` |
| `$buildDir` | `layout.buildDirectory` |
| `tasks.getByName()` | `tasks.named()` |
| `task.dependsOn other` | `tasks.named("task") { dependsOn("other") }` |

### Gradle 8 → 9

| Deprecated | Replacement |
|------------|-------------|
| Top-level `sourceCompatibility` | `java { sourceCompatibility }` or toolchain |
| Project conventions | Extension APIs |
| Various internal APIs | Public alternatives |

## Example Usage

```bash
# Migrate to Gradle 9.0 (interactive)
/migrate-gradle 9.0

# Auto-apply safe fixes to 8.11
/migrate-gradle 8.11 --auto

# Preview migration to 9.0
/migrate-gradle 9.0 --dry-run
```

## Tool Requirements

- JBang installed (for running migration-fixer.java)
- Gradle wrapper present (for version detection and update)
- Write access to build files

## Related

- `/fix-config-cache` - Fix configuration cache issues
- `/doctor` - Full build health check
- `gradle-migration` skill - Detailed migration guidance
