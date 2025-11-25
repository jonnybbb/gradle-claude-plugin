# Gradle Expert Plugin Roadmap: Active Automation

This roadmap transforms the plugin from passive knowledge to active automation.

## Vision

**Before**: Skills tell you what's wrong → You manually fix it → Hope you got it right

**After**: Tools detect issues → Generate exact fixes → Apply with your approval → Verify they work

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    WORKFLOW COMMANDS                         │
│         /fix-config-cache  /migrate-gradle  /optimize        │
├─────────────────────────────────────────────────────────────┤
│                    FIX APPLIER AGENT                         │
│     Interactive review → User approval → Edit application    │
├─────────────────────────────────────────────────────────────┤
│                    FIX GENERATOR TOOLS                       │
│  config-cache-fixer.java  migration-fixer.java  perf-fixer  │
├─────────────────────────────────────────────────────────────┤
│                    ANALYSIS TOOLS (existing)                 │
│     cache-validator.java  gradle-analyzer.java  etc.         │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Configuration Cache Fixer ✅ HIGH VALUE

**Goal**: Auto-fix the most common configuration cache issues.

### Deliverables

1. **`tools/config-cache-fixer.java`** - JBang tool that:
   - Analyzes build files for config cache issues
   - Outputs structured JSON with exact fixes
   - Includes confidence levels (HIGH/MEDIUM/LOW)
   - Marks which fixes are safe to auto-apply

2. **Fix patterns to implement**:
   | Pattern | Confidence | Auto-fixable |
   |---------|------------|--------------|
   | `System.getProperty()` → `providers.systemProperty()` | HIGH | ✓ |
   | `System.getenv()` → `providers.environmentVariable()` | HIGH | ✓ |
   | `tasks.create()` → `tasks.register()` | HIGH | ✓ |
   | `tasks.getByName()` → `tasks.named()` | HIGH | ✓ |
   | `$buildDir` → `layout.buildDirectory` | HIGH | ✓ |
   | `project.copy/exec/delete` in doLast | MEDIUM | ✗ (needs refactor) |
   | `Task.project` access | MEDIUM | ✗ (needs refactor) |

3. **Output format**:
   ```json
   {
     "project": "/path/to/project",
     "analyzed_files": ["build.gradle.kts", "settings.gradle.kts"],
     "fixes": [
       {
         "id": "FIX-001",
         "file": "build.gradle.kts",
         "line": 27,
         "column": 1,
         "issue": "System.getProperty at configuration time",
         "category": "PROVIDER_API",
         "confidence": "HIGH",
         "auto_fixable": true,
         "original": "val dbUrl = System.getProperty(\"db.url\", \"default\")",
         "replacement": "val dbUrl = providers.systemProperty(\"db.url\").orElse(\"default\")",
         "explanation": "Configuration cache cannot serialize System.getProperty. Use Provider API."
       }
     ],
     "summary": {
       "total": 16,
       "high_confidence": 10,
       "medium_confidence": 4,
       "low_confidence": 2,
       "auto_fixable": 10
     }
   }
   ```

### Testing
- Test against `config-cache-broken` fixture (16 known issues)
- Verify fix generation accuracy
- Ensure fixes compile after application

### Success Criteria
- Detects all 16 issues in test fixture
- Generates valid Kotlin replacement code
- HIGH confidence fixes are always correct

---

## Phase 2: Fix Applier Agent

**Goal**: Interactive agent to apply fixes with user control.

### Deliverables

1. **`agents/fix-applier.md`** - Agent that:
   - Reads fix JSON from Phase 1
   - Displays summary table of all fixes
   - Offers apply modes: Auto/Interactive/Dry-run
   - Shows before/after diff for each fix
   - Uses Edit tool to apply changes
   - Runs verification after applying

2. **Apply modes**:
   - **Auto**: Apply all HIGH confidence fixes without prompting
   - **Interactive**: Review each fix individually
   - **Dry-run**: Show what would change, no modifications

3. **User interaction flow**:
   ```
   Fix 1 of 8: System.getProperty at configuration time

   File: build.gradle.kts:27
   Confidence: HIGH (safe to auto-apply)

   ━━━ Before ━━━
   val dbUrl = System.getProperty("db.url", "jdbc:h2:mem:test")

   ━━━ After ━━━
   val dbUrl = providers.systemProperty("db.url").orElse("jdbc:h2:mem:test")

   [A]pply / [S]kip / [V]iew context / Apply [R]emaining / [Q]uit
   ```

4. **Safety features**:
   - Never auto-apply MEDIUM/LOW confidence fixes
   - Always verify with Gradle after applying
   - Show git diff of all changes at end
   - Offer rollback if verification fails

### Testing
- Test full workflow against fixtures
- Verify Edit tool applies correctly
- Test all user interaction paths

### Success Criteria
- Applies fixes without corrupting files
- Verification catches any mistakes
- User always understands what's being changed

---

## Phase 3: Workflow Command

**Goal**: Single command to run the full fix workflow.

### Deliverables

1. **`commands/fix-config-cache.md`**:
   ```markdown
   ---
   name: fix-config-cache
   description: Detect and fix configuration cache issues
   argument-hint: "[--auto] [--dry-run]"
   ---

   # Configuration Cache Fix Workflow

   ## Step 1: Analyze
   Run config-cache-fixer to identify all issues.

   ## Step 2: Generate Fix Plan
   Create structured fix plan with confidence levels.

   ## Step 3: Apply
   Based on flags:
   - --auto: Apply HIGH confidence fixes automatically
   - --dry-run: Show fixes without applying
   - default: Interactive mode via fix-applier agent

   ## Step 4: Verify
   Run `gradle build --configuration-cache` to confirm.
   ```

2. **Command behavior**:
   ```bash
   /fix-config-cache              # Interactive mode
   /fix-config-cache --auto       # Auto-apply HIGH confidence
   /fix-config-cache --dry-run    # Show what would change
   ```

### Testing
- End-to-end test on all fixtures
- Test flag combinations
- Verify command output formatting

### Success Criteria
- Single command fixes common issues
- Clear feedback at each step
- Works on real-world projects

---

## Phase 4: Migration Fixer

**Goal**: Auto-fix deprecated APIs for version migrations.

### Deliverables

1. **`tools/migration-fixer.java`** - Generates fixes for:

   **Gradle 7 → 8**:
   | Deprecated | Replacement |
   |------------|-------------|
   | `project.buildDir` | `layout.buildDirectory` |
   | `project.file()` | `layout.projectDirectory.file()` |
   | `ConfigurableFileCollection.from(project.X)` | Provider-based |
   | `configurations.compile` | `configurations.implementation` |

   **Gradle 8 → 9**:
   | Deprecated | Replacement |
   |------------|-------------|
   | Various task properties | Lazy equivalents |
   | Old plugin DSL | Plugin marker artifacts |

2. **`commands/migrate-gradle.md`**:
   ```
   /migrate-gradle 9.0           # Migrate to Gradle 9.0
   /migrate-gradle 8.11 --auto   # Auto-apply safe fixes
   ```

3. **Workflow**:
   - Detect current version
   - Identify all deprecated/removed APIs
   - Generate migration plan
   - Apply fixes (interactive or auto)
   - Update wrapper
   - Run verification build

### Testing
- Test against `legacy-groovy` fixture
- Verify migration path correctness
- Test wrapper update

### Success Criteria
- Handles 7→8 and 8→9 migrations
- No false positives on valid code
- Wrapper updated correctly

---

## Phase 5: Performance Fixer

**Goal**: Auto-apply performance optimizations.

### Deliverables

1. **`tools/performance-fixer.java`** - Identifies and fixes:

   **gradle.properties optimizations**:
   - `org.gradle.parallel=true`
   - `org.gradle.caching=true`
   - `org.gradle.configuration-cache=true`
   - `org.gradle.vfs.watch=true`
   - JVM args tuning

   **Build script optimizations**:
   - `tasks.all {}` → `tasks.configureEach {}`
   - Missing `@CacheableTask` annotations
   - Inefficient dependency declarations

2. **`commands/optimize.md`**:
   ```
   /optimize                  # Analyze and suggest
   /optimize --apply          # Apply safe optimizations
   /optimize --benchmark      # Run before/after benchmark
   ```

### Testing
- Test on various project sizes
- Benchmark actual improvement
- Verify no regressions

### Success Criteria
- Measurable build time improvement
- No build breakage
- Clear before/after metrics

---

## Phase 6: Proactive Hooks

**Goal**: Automatic analysis without user action.

### Deliverables

1. **Session start hook** - When opening Gradle project:
   - Run quick health check
   - Show summary of issues if any
   - Suggest relevant commands

2. **Post-edit hook** - After modifying build files:
   - Quick validation
   - Warn about introduced issues
   - Suggest fixes

3. **Configuration**:
   ```yaml
   # .claude/hooks.yaml
   hooks:
     - event: SessionStart
       condition: "test -f build.gradle.kts"
       action: "Quick health check available. Run /doctor for details."

     - event: PostToolUse
       matcher:
         tool: Edit
         path: "**/build.gradle*"
       action: "jbang tools/quick-validate.java ${FILE}"
   ```

### Testing
- Test hook triggers
- Verify non-blocking execution
- Test condition matching

### Success Criteria
- Proactive without being annoying
- Fast execution (< 2 seconds)
- Useful warnings only

---

## Implementation Order

| Phase | Effort | Value | Dependencies |
|-------|--------|-------|--------------|
| 1. Config Cache Fixer | 2-3 days | HIGH | None |
| 2. Fix Applier Agent | 1-2 days | HIGH | Phase 1 |
| 3. Workflow Command | 0.5 day | HIGH | Phase 1, 2 |
| 4. Migration Fixer | 2-3 days | MEDIUM | Phase 1 pattern |
| 5. Performance Fixer | 2 days | MEDIUM | Phase 1 pattern |
| 6. Proactive Hooks | 1 day | MEDIUM | Any |

**Recommended**: Complete Phases 1-3 first for immediate high value.

---

## File Structure After Implementation

```
tools/
├── gradle-analyzer.java        # existing
├── cache-validator.java        # existing
├── task-analyzer.java          # existing
├── performance-profiler.java   # existing
├── build-health-check.java     # existing
├── config-cache-fixer.java     # Phase 1 ✓ DONE
├── migration-fixer.java        # Phase 4 ✓ DONE
└── performance-fixer.java      # Phase 5 (planned)

agents/
├── doctor.md                   # existing
├── migration.md                # existing
└── fix-applier.md              # Phase 2 ✓ DONE

commands/
├── doctor.md                   # existing
├── migrate.md                  # existing
├── fix-config-cache.md         # Phase 3 ✓ DONE
├── migrate-gradle.md           # Phase 4 ✓ DONE
└── optimize.md                 # Phase 5 (update existing)
```

---

## Current Status

- [x] **Phase 1**: Config Cache Fixer - COMPLETE
  - Created `tools/config-cache-fixer.java`
  - Detects 19 issues in config-cache-broken fixture
  - Outputs structured JSON with fixes
  - HIGH/MEDIUM/LOW confidence levels
- [x] **Phase 2**: Fix Applier Agent - COMPLETE
  - Created `agents/fix-applier.md`
  - Interactive/Auto/Dry-run modes
  - Before/after diffs with explanations
- [x] **Phase 3**: Workflow Command - COMPLETE
  - Created `commands/fix-config-cache.md`
  - Supports --auto and --dry-run flags
  - Full analyze → fix → verify workflow
- [x] **Phase 4**: Migration Fixer - COMPLETE
  - Created `tools/migration-fixer.java`
  - Detects 22 deprecations in legacy-groovy fixture
  - Supports 7→8 and 8→9 migration paths
  - Created `commands/migrate-gradle.md`
- [ ] **Phase 5**: Performance Fixer - NOT STARTED
- [ ] **Phase 6**: Proactive Hooks - NOT STARTED

---

## Getting Started

To begin Phase 1:

```bash
# Create the fixer tool
touch tools/config-cache-fixer.java

# Use config-cache-broken fixture for testing
ls test-fixtures/projects/config-cache-broken/

# Run existing validator for reference
jbang tools/cache-validator.java test-fixtures/projects/config-cache-broken/
```

The fixer should detect the same issues as the validator, but output structured fix plans instead of just reports.
