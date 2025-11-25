---
description: Detect and fix configuration cache issues automatically
argument-hint: "[--auto] [--dry-run]"
allowed-tools: Read, Edit, Glob, Grep, Bash, AskUserQuestion
---

# Configuration Cache Fix Workflow

You are running the configuration cache fix workflow. This command analyzes the Gradle project for configuration cache compatibility issues and helps fix them.

## Arguments

Parse the command arguments:
- `--auto`: Automatically apply all HIGH confidence fixes without prompting
- `--dry-run`: Show what would be fixed without making changes
- (default): Interactive mode - review each fix before applying

## Workflow Steps

### Step 1: Analyze Project

Run the config-cache-fixer tool to identify all issues:

```bash
jbang tools/config-cache-fixer.java . --json
```

Parse the JSON output to get the complete fix plan.

### Step 2: Display Summary

Present a clear summary of findings:

```
═══════════════════════════════════════════════════════════════
           CONFIGURATION CACHE ANALYSIS
═══════════════════════════════════════════════════════════════

Found N issues in M files:

  HIGH confidence (auto-fixable):  X
    • System.getProperty → providers.systemProperty
    • System.getenv → providers.environmentVariable
    • tasks.create → tasks.register
    • tasks.getByName → tasks.named
    • $buildDir → layout.buildDirectory

  MEDIUM confidence (manual review): Y
    • project.copy/exec/delete in task actions
    • Task.project access at execution time

  LOW confidence (optional): Z
    • System calls in doLast (allowed but not ideal)

═══════════════════════════════════════════════════════════════
```

### Step 3: Apply Fixes

Based on the mode:

**--auto mode:**
1. Apply all HIGH confidence fixes automatically
2. Show progress as each fix is applied
3. Skip MEDIUM/LOW - explain they need manual review
4. Run verification after all fixes

**--dry-run mode:**
1. List all fixes that would be applied
2. Show before/after for each
3. Don't modify any files
4. Report total scope of changes

**Interactive mode (default):**
1. For each fix, show:
   - File and line number
   - Original code
   - Proposed replacement
   - Explanation
2. Ask user: Apply / Skip / View more context
3. Track applied vs skipped
4. Run verification after

### Step 4: Verify Changes

After applying fixes (not in dry-run mode):

```bash
./gradlew help --configuration-cache 2>&1
```

Report the result:
- If successful: Configuration cache is now working!
- If failed: Show the error and suggest next steps

### Step 5: Summary Report

Provide final summary:

```
═══════════════════════════════════════════════════════════════
                    FIX SUMMARY
═══════════════════════════════════════════════════════════════

Applied:  X fixes (HIGH confidence)
Skipped:  Y fixes (user choice or MEDIUM/LOW)
Remaining: Z issues need manual attention

Verification: ✓ Build successful with configuration cache

Next steps:
  • Review MEDIUM confidence issues manually
  • Run /doctor for full health check
  • See gradle-config-cache skill for detailed guidance

═══════════════════════════════════════════════════════════════
```

## Example Usage

```bash
# Interactive mode - review each fix
/fix-config-cache

# Auto mode - apply all safe fixes automatically
/fix-config-cache --auto

# Dry run - see what would change
/fix-config-cache --dry-run
```

## Tool Requirements

This command requires:
- JBang installed (for running config-cache-fixer.java)
- Gradle wrapper in the project (for verification)
- Write access to build files (for applying fixes)

## Related

- `/doctor` - Full build health check
- `/optimize` - Performance optimization
- `gradle-config-cache` skill - Detailed configuration cache guidance
