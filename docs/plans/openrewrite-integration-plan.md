# OpenRewrite Integration Plan

## Executive Summary

Integrate OpenRewrite as an opt-in execution engine for migrations and refactoring, combining this plugin's analysis and guidance capabilities with OpenRewrite's deterministic, large-scale transformation capabilities.

## Current State

| Component | Capability |
|-----------|-----------|
| `/fix-config-cache` | Claude-driven text transformations |
| `/upgrade` | Claude-driven API migrations |
| `/doctor` | Analysis and recommendations |
| JBang tools | Gradle Tooling API analysis |

**Limitation**: Claude-driven transformations work file-by-file, may produce inconsistent formatting, and aren't reproducible across runs.

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Commands                             │
│  /fix-config-cache  /upgrade  /optimize  /openrewrite       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Analysis Layer                             │
│  • Issue detection (existing JBang tools)                    │
│  • Recipe mapping engine (NEW)                               │
│  • Custom recipe generator (NEW)                             │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│   Claude Execution       │  │  OpenRewrite Execution    │
│   (default)              │  │  (opt-in)                 │
│                          │  │                           │
│  • Interactive           │  │  • Deterministic          │
│  • Context-aware         │  │  • Preserves formatting   │
│  • Handles edge cases    │  │  • Reproducible           │
│  • Small-scale           │  │  • Large-scale            │
└──────────────────────────┘  └──────────────────────────┘
```

## Opt-in Mechanisms

### 1. Per-Command Flag

```bash
# Use OpenRewrite for this execution
/fix-config-cache --engine=openrewrite
/upgrade 9.0 --engine=openrewrite

# Dry-run with OpenRewrite
/fix-config-cache --engine=openrewrite --dry-run
```

### 2. Project Settings

```yaml
# .claude/gradle-plugin.local.md
---
openrewrite:
  enabled: true
  defaultEngine: false      # true = prefer OpenRewrite by default
  cliVersion: "latest"      # or pin: "2.21.0"
  additionalRecipes: []     # extra recipe coordinates
  recipeOptions: {}         # recipe-specific options
---
```

### 3. Dedicated Command

```bash
# Direct OpenRewrite operations
/openrewrite run org.openrewrite.gradle.MigrateToGradle8
/openrewrite dry-run org.openrewrite.gradle.MigrateToGradle8
/openrewrite list gradle          # List Gradle-related recipes
/openrewrite suggest              # Suggest recipes based on analysis
```

## Implementation Phases

### Phase 1: Recipe Recommendation (Low Effort)

**Goal**: Map detected issues to OpenRewrite recipes without execution.

**New Components**:
- `skills/gradle-openrewrite/SKILL.md` - OpenRewrite integration guide
- `skills/gradle-openrewrite/references/recipe-catalog.md` - Curated recipe list

**Behavior Change**:
```
/doctor output:
  ⚠ Found 47 uses of tasks.create() (should use tasks.register())
    → Fix with Claude: /fix-config-cache --auto
    → Fix with OpenRewrite: org.openrewrite.gradle.MigrateToGradle8
```

**Recipe Mapping Table**:

| Detected Issue | OpenRewrite Recipe |
|---------------|-------------------|
| `tasks.create()` | `org.openrewrite.gradle.search.FindGradleProject` + manual |
| `$buildDir` usage | `org.openrewrite.gradle.UpdateGradleWrapper` (partial) |
| Groovy DSL | `org.openrewrite.kotlin.gradle.MigrateToKotlinDsl` |
| Old Gradle version | `org.openrewrite.gradle.UpdateGradleWrapper` |
| Missing version catalog | `org.openrewrite.gradle.MigrateToVersionCatalog` |
| Deprecated APIs | `org.openrewrite.gradle.MigrateToGradle8` |

### Phase 2: OpenRewrite CLI Integration (Medium Effort)

**Goal**: Execute OpenRewrite recipes directly from plugin commands.

**New Components**:
- `tools/openrewrite_runner.java` - JBang tool to run rewrite-cli
- Modified commands to support `--engine=openrewrite` flag

**JBang Tool Design**:

```java
//DEPS info.picocli:picocli:4.7.5
//DEPS org.openrewrite:rewrite-core:LATEST
// Uses rewrite-cli under the hood

public class OpenRewriteRunner {
    // Download and cache rewrite-cli if needed
    // Execute specified recipes
    // Parse results and return structured output
    // Support dry-run mode
}
```

**Alternative: Init Script Approach**:

Generate a temporary Gradle init script that applies OpenRewrite:

```kotlin
// Generated: /tmp/openrewrite-init.gradle.kts
initscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.openrewrite:plugin:latest.release")
    }
}

allprojects {
    apply(plugin = org.openrewrite.gradle.RewritePlugin::class.java)

    rewrite {
        activeRecipe("org.openrewrite.gradle.MigrateToGradle8")
    }
}
```

Then execute:
```bash
./gradlew rewriteRun --init-script /tmp/openrewrite-init.gradle.kts
```

**Pros/Cons**:

| Approach | Pros | Cons |
|----------|------|------|
| rewrite-cli via JBang | No Gradle modification, standalone | Separate tool, version sync |
| Init script injection | Uses project's Gradle, native | Temp files, may conflict |
| Direct API usage | Full control | Complex, heavy dependencies |

**Recommendation**: Init script approach for Gradle projects (most compatible).

---

## Deep Dive: Init Script vs Gradle Tooling API

### Approach A: Init Script Injection

**How it works**: Generate a temporary init script that applies the OpenRewrite Gradle plugin, then invoke Gradle with `--init-script`.

```kotlin
// Generated: /tmp/openrewrite-init-{uuid}.gradle.kts
initscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.openrewrite:plugin:latest.release")
    }
}

allprojects {
    apply(plugin = org.openrewrite.gradle.RewritePlugin::class.java)

    configure<org.openrewrite.gradle.RewriteExtension> {
        activeRecipe("org.openrewrite.gradle.MigrateToGradle8")
        // Additional configuration...
    }
}
```

**Execution**:
```bash
./gradlew rewriteRun --init-script /tmp/openrewrite-init-{uuid}.gradle.kts
```

**Advantages**:
| Aspect | Benefit |
|--------|---------|
| No build modification | User's build.gradle remains untouched |
| Uses project's Gradle | Respects wrapper version, daemon, settings |
| Full Gradle context | Access to all project configurations, dependencies |
| Native caching | Leverages Gradle's dependency cache |
| Multi-project aware | Properly handles composite/included builds |
| Familiar output | Standard Gradle console output |

**Disadvantages**:
| Aspect | Drawback |
|--------|----------|
| Requires Gradle execution | Full Gradle startup overhead |
| Init script limitations | Some configurations harder to inject |
| Potential conflicts | May conflict with existing plugins |
| Temp file management | Need to clean up generated scripts |
| Version coupling | Init script syntax varies by Gradle version |

**Implementation Complexity**: Medium
- Generate init script dynamically based on requested recipes
- Handle Gradle version differences (Groovy vs Kotlin DSL init scripts)
- Parse Gradle output for results
- Clean up temp files

---

### Approach B: Gradle Tooling API with OpenRewrite

**How it works**: Use Gradle Tooling API to programmatically configure and execute an OpenRewrite build action.

```java
//DEPS org.gradle:gradle-tooling-api:8.14
//DEPS org.openrewrite:rewrite-core:8.x.x
//DEPS org.openrewrite:rewrite-gradle:8.x.x

public class OpenRewriteToolingApiRunner {
    public void runRecipe(Path projectDir, String recipeName) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile())
                .connect()) {

            // Option 1: Execute rewriteRun task if plugin already applied
            connection.newBuild()
                .forTasks("rewriteRun")
                .withArguments("-Drewrite.activeRecipe=" + recipeName)
                .run();

            // Option 2: Use BuildAction to inject configuration
            connection.action(new OpenRewriteBuildAction(recipeName))
                .run();
        }
    }
}

// Custom BuildAction for more control
class OpenRewriteBuildAction implements BuildAction<RewriteResult> {
    @Override
    public RewriteResult execute(BuildController controller) {
        // Access Gradle model
        // Configure OpenRewrite programmatically
        // Execute and return results
    }
}
```

**Advantages**:
| Aspect | Benefit |
|--------|---------|
| Programmatic control | Full Java API access |
| Structured results | Can return typed objects, not just console output |
| No temp files | Everything in memory |
| IDE integration ready | Same API used by IntelliJ, Eclipse |
| Progress monitoring | BuildAction supports progress listeners |
| Error handling | Proper exceptions instead of parsing output |

**Disadvantages**:
| Aspect | Drawback |
|--------|----------|
| API complexity | Tooling API has learning curve |
| Build action limitations | BuildActions run in Gradle daemon, limited classpath |
| OpenRewrite classpath | Getting OpenRewrite onto Gradle's classpath is tricky |
| Version matrix | Must handle Tooling API ↔ Gradle ↔ OpenRewrite compatibility |
| Heavier dependencies | JBang script becomes larger |

**Implementation Complexity**: High
- Manage complex classpath requirements
- Handle Tooling API version compatibility
- Implement custom BuildAction (if needed)
- More error handling code

---

### Approach C: Hybrid - Tooling API for Orchestration, Init Script for Execution

**How it works**: Use Tooling API to analyze project and orchestrate, but delegate actual OpenRewrite execution to init script.

```java
public class HybridOpenRewriteRunner {
    public RewriteResult run(Path projectDir, String recipe) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile())
                .connect()) {

            // 1. Use Tooling API to get project model
            GradleProject model = connection.getModel(GradleProject.class);
            String gradleVersion = connection.getModel(BuildEnvironment.class)
                .getGradle().getGradleVersion();

            // 2. Generate appropriate init script for this Gradle version
            Path initScript = generateInitScript(gradleVersion, recipe);

            // 3. Execute via Tooling API with init script
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            connection.newBuild()
                .forTasks("rewriteRun")
                .withArguments("--init-script", initScript.toString())
                .setStandardOutput(output)
                .run();

            // 4. Parse results
            return parseRewriteOutput(output.toString());
        }
    }
}
```

**Advantages**:
- Best of both worlds
- Tooling API for analysis/orchestration (typed, structured)
- Init script for execution (proven, compatible)
- Clean separation of concerns

**Disadvantages**:
- Two mechanisms to maintain
- Still has temp file for init script

---

### Comparison Matrix

| Criteria | Init Script Only | Tooling API Only | Hybrid |
|----------|-----------------|------------------|--------|
| **Implementation effort** | Low-Medium | High | Medium |
| **Gradle version compat** | Good (with conditionals) | Medium (API changes) | Good |
| **OpenRewrite compat** | Excellent | Tricky classpath | Excellent |
| **Structured output** | Parse console | Native objects | Native objects |
| **Multi-project support** | Automatic | Manual handling | Automatic |
| **Temp files** | Yes | No | Yes |
| **Progress reporting** | Parse output | Native listeners | Native listeners |
| **Error handling** | Parse stderr | Exceptions | Exceptions |
| **Maintenance burden** | Low | High | Medium |
| **CI/CD friendliness** | Excellent | Good | Excellent |

---

### Recommendation: Hybrid Approach

**Rationale**:

1. **Tooling API for analysis**: We already use Tooling API in existing JBang tools. Continue using it for:
   - Detecting Gradle version
   - Understanding project structure
   - Progress monitoring
   - Structured error handling

2. **Init script for OpenRewrite execution**: OpenRewrite's Gradle plugin is battle-tested and handles:
   - Recipe dependency resolution
   - Multi-project builds
   - Incremental processing
   - Result reporting

3. **Best compatibility**: Init scripts work across Gradle 6.x-9.x with minimal adjustments, while Tooling API provides the orchestration layer.

**Implementation Sketch**:

```java
//DEPS org.gradle:gradle-tooling-api:8.14

public class OpenRewriteRunner {

    public Result runRecipe(Path project, String recipe, boolean dryRun) {
        try (ProjectConnection conn = connect(project)) {
            // 1. Get Gradle version for init script generation
            String gradleVersion = getGradleVersion(conn);

            // 2. Generate version-appropriate init script
            Path initScript = generateInitScript(gradleVersion, recipe);

            // 3. Execute with progress monitoring
            Result result = new Result();
            conn.newBuild()
                .forTasks(dryRun ? "rewriteDryRun" : "rewriteRun")
                .withArguments("--init-script", initScript.toString())
                .addProgressListener(event -> result.trackProgress(event))
                .setStandardOutput(result.getOutputStream())
                .setStandardError(result.getErrorStream())
                .run();

            // 4. Parse and structure results
            return result.parse();

        } finally {
            // 5. Cleanup temp files
            cleanupInitScript(initScript);
        }
    }

    private Path generateInitScript(String gradleVersion, String recipe) {
        // Generate Groovy init script for Gradle < 8.0
        // Generate Kotlin init script for Gradle >= 8.0
        // Include recipe-specific configuration
    }
}
```

---

### Init Script Templates

**For Gradle 6.x-7.x (Groovy)**:
```groovy
// openrewrite-init.gradle
initscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'org.openrewrite:plugin:latest.release'
    }
}

allprojects {
    apply plugin: org.openrewrite.gradle.RewritePlugin

    rewrite {
        activeRecipe '${RECIPE_NAME}'
        failOnDryRunResults = ${FAIL_ON_DRY_RUN}
    }
}
```

**For Gradle 8.x+ (Kotlin)**:
```kotlin
// openrewrite-init.gradle.kts
initscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.openrewrite:plugin:latest.release")
    }
}

allprojects {
    apply(plugin = org.openrewrite.gradle.RewritePlugin::class.java)

    configure<org.openrewrite.gradle.RewriteExtension> {
        activeRecipe("${RECIPE_NAME}")
        setFailOnDryRunResults(${FAIL_ON_DRY_RUN})
    }
}
```

---

### Edge Cases to Handle

| Scenario | Solution |
|----------|----------|
| Composite builds | Init script applied to all included builds automatically |
| Included builds | May need `--include-build` handling |
| Configuration cache | OpenRewrite plugin has known CC issues - may need `--no-configuration-cache` |
| Offline mode | Pre-cache OpenRewrite dependencies or bundle them |
| Custom repositories | Inject user's repository configuration into init script |
| Conflicting plugins | Check if OpenRewrite already applied, skip injection |

### Phase 3: Smart Recipe Generation (Medium-High Effort)

**Goal**: Generate custom recipes for project-specific issues not covered by standard recipes.

**Use Cases**:
- Custom deprecated API migrations
- Project-specific patterns
- Proprietary framework migrations

**Workflow**:
1. Plugin analyzes project, finds issues without standard recipes
2. Generates `rewrite.yml` with custom declarative recipes
3. Executes alongside standard recipes

**Example Generated Recipe**:

```yaml
# Generated: .rewrite/custom-migrations.yml
type: specs.openrewrite.org/v1beta/recipe
name: com.myproject.MigrateCustomApis
displayName: Migrate Custom APIs
description: Auto-generated recipe for project-specific migrations
recipeList:
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: "com.myproject.OldApi doThing()"
      newMethodName: "doThingV2"
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: "com.myproject.DeprecatedService"
      newFullyQualifiedTypeName: "com.myproject.NewService"
```

### Phase 4: Unified Migration Agent (High Effort)

**Goal**: Intelligent agent that orchestrates complex migrations using both engines.

**New Component**: `agents/migration-orchestrator.md`

**Capabilities**:
- Analyze project complexity and choose appropriate engine
- Split work: OpenRewrite for bulk, Claude for edge cases
- Verify transformations compile and tests pass
- Rollback on failure
- Generate migration report

**Example Flow**:

```
User: /upgrade 9.0 --full

Agent:
1. Analyze project (47 modules, 1,234 build files)
2. Detect: 89 eager task patterns, 23 deprecated APIs, 12 custom issues
3. Plan:
   - Phase A: Run OpenRewrite MigrateToGradle8 (covers 78 patterns)
   - Phase B: Run OpenRewrite UpdateGradleWrapper
   - Phase C: Claude fixes for 12 custom issues
   - Phase D: Verify build compiles
4. Execute with progress reporting
5. Generate migration report
```

## New Plugin Components

### Skills

```
skills/
└── gradle-openrewrite/
    ├── SKILL.md                    # Integration guide
    └── references/
        ├── recipe-catalog.md       # Curated recipes for Gradle
        ├── custom-recipes.md       # Writing custom recipes
        └── troubleshooting.md      # Common issues
```

### Commands

```
commands/
├── openrewrite.md                  # /openrewrite command
└── (modified existing commands with --engine flag)
```

### Tools

```
tools/
└── openrewrite_runner.java         # JBang CLI wrapper
```

### Agents

```
agents/
└── migration-orchestrator.md       # Intelligent migration agent
```

## Configuration Schema

```yaml
# .claude/gradle-plugin.local.md
---
openrewrite:
  # Enable OpenRewrite integration
  enabled: true

  # Use OpenRewrite as default engine (otherwise Claude)
  defaultEngine: false

  # CLI version to use
  cliVersion: "latest"  # or "2.21.0"

  # Additional recipe dependencies
  additionalRecipes:
    - "org.openrewrite.recipe:rewrite-spring:LATEST"
    - "com.mycompany:custom-recipes:1.0.0"

  # Recipe-specific options
  recipeOptions:
    "org.openrewrite.gradle.UpdateGradleWrapper":
      version: "8.14"

  # Recipes to exclude
  excludeRecipes:
    - "org.openrewrite.gradle.SomeProblematicRecipe"

  # Execution options
  execution:
    dryRunByDefault: true    # Always preview first
    failOnError: false       # Continue on recipe errors
    generateReport: true     # Create HTML report
---
```

## Recipe Mapping Database

Create a mapping file for automatic recipe suggestions:

```yaml
# skills/gradle-openrewrite/references/recipe-mappings.yml
mappings:
  - detection:
      pattern: "tasks\\.create\\("
      type: "config-cache-issue"
    recipes:
      - name: "org.openrewrite.gradle.MigrateToGradle8"
        confidence: 0.8
        note: "Covers most task registration patterns"

  - detection:
      pattern: "\\$buildDir"
      type: "deprecated-api"
    recipes:
      - name: "org.openrewrite.gradle.MigrateBuildToGradle8"
        confidence: 0.9

  - detection:
      pattern: "apply plugin:"
      type: "best-practice"
    recipes:
      - name: "org.openrewrite.gradle.plugins.MigrateToPluginsBlock"
        confidence: 0.95

  - detection:
      gradleVersion: "<8.0"
      type: "version-upgrade"
    recipes:
      - name: "org.openrewrite.gradle.UpdateGradleWrapper"
        options:
          version: "8.14"
```

## User Experience Examples

### Example 1: Opt-in via Flag

```bash
$ /fix-config-cache --dry-run

Found 47 configuration cache issues:
  • 32 × tasks.create() → tasks.register()
  • 12 × $buildDir → layout.buildDirectory
  • 3 × System.getProperty() → providers.systemProperty()

Options:
  1. /fix-config-cache --auto              (Claude - interactive)
  2. /fix-config-cache --engine=openrewrite (OpenRewrite - deterministic)

$ /fix-config-cache --engine=openrewrite

Running OpenRewrite recipe: org.openrewrite.gradle.MigrateToGradle8
✓ Transformed 44 files
✓ Fixed 47 issues
✓ Formatting preserved

3 issues require manual review (see report)
```

### Example 2: Direct OpenRewrite Command

```bash
$ /openrewrite suggest

Based on project analysis, recommended recipes:

1. org.openrewrite.gradle.MigrateToGradle8
   Fixes: task registration, deprecated APIs
   Estimated changes: 47 files

2. org.openrewrite.gradle.MigrateToVersionCatalog
   Fixes: dependency declarations
   Estimated changes: 12 files

3. org.openrewrite.kotlin.gradle.MigrateToKotlinDsl
   Fixes: Groovy → Kotlin DSL
   Estimated changes: 23 files

Run: /openrewrite run <recipe-name>
Preview: /openrewrite dry-run <recipe-name>
```

### Example 3: Intelligent Engine Selection

```bash
$ /upgrade 9.0

Analyzing project...
• 3 modules, 8 build files
• 12 issues detected

Recommendation: Use Claude engine (small project, interactive fixes)

Proceed? [Y/n]

---

$ /upgrade 9.0

Analyzing project...
• 127 modules, 894 build files
• 2,341 issues detected

Recommendation: Use OpenRewrite engine (large project, bulk transformation)

Proceed with OpenRewrite? [Y/n]
```

## Implementation Roadmap

| Phase | Effort | Status | Deliverables |
|-------|--------|--------|--------------|
| Phase 1 | Low | ✅ Complete | Skill + recipe recommendations in output |
| Phase 2 | Medium | ✅ Complete | CLI integration, --engine flag |
| Phase 3 | Medium-High | ✅ Complete | Custom recipe generation |
| Phase 4 | High | ✅ Complete | Migration orchestrator agent |

### Phase 4 Implementation Details

Delivered components:
- `tools/openrewrite_runner.java` - Added `--analyze` flag for project complexity assessment
- `agents/migration-orchestrator.md` - Intelligent migration orchestration agent
- `commands/migrate.md` - `/migrate` command for comprehensive migrations

Key features:
- Project complexity assessment (SMALL/MEDIUM/LARGE)
- Engine selection logic (OPENREWRITE_PRIMARY/CLAUDE_PRIMARY/HYBRID)
- Migration plan generation with steps and time estimates
- Issue categorization by type and coverage

## Success Metrics

1. **Adoption**: % of migrations using OpenRewrite engine
2. **Accuracy**: Transformation success rate (compiles after)
3. **Efficiency**: Time saved vs Claude-only approach on large projects
4. **Coverage**: % of detected issues with recipe mappings

## Open Questions

1. **License considerations**: OpenRewrite is Apache 2.0, but some recipes may have different licenses
2. **Moderne CLI**: Should we support Moderne CLI (commercial) for enterprise features?
3. **Recipe versioning**: How to handle recipe version compatibility with Gradle versions?
4. **Offline support**: Should we bundle common recipes or always fetch?

## References

- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [OpenRewrite Gradle Recipes](https://docs.openrewrite.org/recipes/gradle)
- [rewrite-gradle-plugin](https://github.com/openrewrite/rewrite-gradle-plugin)
- [Moderne CLI](https://docs.moderne.io/moderne-cli/getting-started/cli-intro)
