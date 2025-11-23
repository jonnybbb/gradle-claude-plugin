---
description: Analyzes existing Gradle task implementations for proper input/output configuration, cacheability, incremental build support, and provides specific improvement recommendations.
---

# Review Gradle Task

Analyze an existing Gradle task implementation and provide detailed feedback on correctness, performance, and best practices.

## What This Command Does

1. **Analyzes Task Implementation**: Reviews task class structure and annotations
2. **Validates Inputs/Outputs**: Checks proper annotation usage
3. **Assesses Cacheability**: Determines if task can benefit from build cache
4. **Checks Incremental Support**: Evaluates incremental processing capability
5. **Reviews Task Dependencies**: Validates task ordering and dependencies
6. **Provides Recommendations**: Specific, actionable improvements

## Usage

```
/reviewTask <task-name>
```

Examples:
```
/reviewTask processResources
/reviewTask :app:generateBuildInfo
/reviewTask customDataProcessor
```

## Review Categories

### 1. ✅ Task Structure

**Checks:**
- Task class extends appropriate base
- Uses abstract properties (not concrete fields)
- Proper use of Provider API
- Task action method properly annotated

**Example Review:**
```
=== Task Structure Review ===

Task: :app:generateBuildInfo
Class: com.example.tasks.BuildInfoTask

✅ Extends DefaultTask
✅ Uses abstract properties
⚠️  Uses concrete field for internal state
❌ Missing @CacheableTask annotation

Code Review:
  Line 15: class BuildInfoTask : DefaultTask()
  ✅ Good: Proper base class

  Line 17-19: var timestamp: Long = 0
  ❌ Issue: Concrete field instead of abstract property
  💡 Fix: Use @get:Internal abstract val timestamp: Property<Long>

  Line 23: @TaskAction fun generate()
  ✅ Good: Proper action annotation
```

### 2. 📋 Input/Output Annotations

**Checks:**
- All inputs properly annotated
- All outputs properly annotated
- Appropriate annotation types used
- PathSensitivity set for file inputs
- Optional inputs marked correctly

**Example Review:**
```
=== Input/Output Review ===

Inputs:
  ✅ inputFile: @InputFile @PathSensitive(RELATIVE)
  ❌ configDir: File - Missing @InputDirectory annotation
  ⚠️  options: Map<String, String> - Missing @Input annotation
  ✅ enabled: @Input (boolean)

Outputs:
  ✅ outputFile: @OutputFile
  ❌ reportDir: DirectoryProperty - Missing @OutputDirectory annotation
  ⚠️  tempFile: File - Should be @LocalState (not cached)

Missing Annotations:
  Line 25: val configDir = File("config")
  💡 Add: @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE)

  Line 27: val options: Map<String, String> = emptyMap()
  💡 Add: @get:Input

  Line 35: abstract val reportDir: DirectoryProperty
  💡 Add: @get:OutputDirectory

Path Sensitivity Issues:
  Line 23: @get:InputFile (no path sensitivity)
  ⚠️  Default is ABSOLUTE - breaks cache portability
  💡 Fix: @get:PathSensitive(PathSensitivity.RELATIVE)
```

### 3. 💾 Cacheability Analysis

**Checks:**
- Task marked as cacheable
- No absolute paths in inputs
- Deterministic task logic
- No timestamps/random values in outputs
- Proper serialization

**Example Review:**
```
=== Cacheability Analysis ===

Current Status: NOT CACHEABLE ❌

Issues Preventing Caching:
  1. Missing @CacheableTask annotation
     Severity: HIGH
     Impact: Task never cached
     Fix: Add @CacheableTask to class declaration

  2. Absolute paths in inputs (Line 28)
     Severity: CRITICAL
     Impact: Cache not portable across machines
     Code: val config = File("/absolute/path/config.json")
     Fix: Use project.layout.projectDirectory.file("config.json")

  3. Non-deterministic output (Line 45)
     Severity: HIGH
     Impact: Cache always misses
     Code: output.writeText("Built at: ${System.currentTimeMillis()}")
     Fix: Exclude timestamp or use @get:Internal

  4. Missing path sensitivity (Line 23)
     Severity: MEDIUM
     Impact: Cache not portable
     Fix: Add @PathSensitive(PathSensitivity.RELATIVE)

Cacheability Score: 2/10

Potential Improvement:
  After fixes: 75% faster builds with cache hits
  Estimated time savings: 30s per build (current task time: 40s)
```

### 4. 📈 Incremental Build Support

**Checks:**
- Supports InputChanges parameter
- Handles ADDED/MODIFIED/REMOVED changes
- Implements full rebuild fallback
- Proper change detection

**Example Review:**
```
=== Incremental Build Support ===

Current Status: NOT INCREMENTAL ❌

Task Action:
  @TaskAction
  fun process() {
    // Processes all files every time
    inputFiles.forEach { processFile(it) }
  }

💡 Recommendation: Add Incremental Support

Suggested Implementation:
  @TaskAction
  fun process(inputChanges: InputChanges) {
    if (!inputChanges.isIncremental) {
      // Full rebuild
      project.delete(outputDir)
      inputFiles.forEach { processFile(it) }
    } else {
      // Incremental processing
      inputChanges.getFileChanges(inputFiles).forEach { change ->
        when (change.changeType) {
          ChangeType.ADDED, ChangeType.MODIFIED -> {
            processFile(change.file)
          }
          ChangeType.REMOVED -> {
            deleteOutputFor(change.file)
          }
        }
      }
    }
  }

Potential Improvement:
  Incremental builds: 90% faster (only process changed files)
  Estimated time savings: 36s per incremental build
```

### 5. 🔗 Task Dependencies

**Checks:**
- Proper use of dependsOn
- Correct use of mustRunAfter/shouldRunAfter
- Avoid direct task references
- Provider-based dependencies

**Example Review:**
```
=== Task Dependencies Review ===

Dependencies Found:
  1. Line 67: dependsOn("compileJava")
     ✅ Good: String-based reference

  2. Line 68: dependsOn(tasks.getByName("processResources"))
     ⚠️  Warning: Eager task realization
     💡 Fix: dependsOn(tasks.named("processResources"))

  3. Line 72: mustRunAfter(project(":lib").tasks.named("build"))
     ❌ Issue: Cross-project task dependency
     💡 Fix: Use outputs as inputs instead

Ordering Issues:
  - Task may run before required inputs are ready
  - Cross-project reference creates coupling

Recommendations:
  1. Replace eager task realization with lazy references
  2. Use outputs as inputs instead of cross-project dependencies
  3. Consider task configuration avoidance patterns
```

### 6. ⚡ Performance Analysis

**Checks:**
- Task execution time
- Unnecessary work detection
- Parallel execution compatibility
- Resource usage

**Example Review:**
```
=== Performance Analysis ===

Execution Metrics (last 10 builds):
  Average time: 42s
  Min time: 38s
  Max time: 58s
  Median time: 40s

Performance Issues:
  1. Sequential file processing
     Impact: HIGH (42s total)
     💡 Fix: Use Worker API for parallel processing
     Estimated improvement: 30s → 10s (3-4x faster)

  2. Unnecessary file reads
     Impact: MEDIUM
     Code: File repeatedly read in loop
     💡 Fix: Cache file contents

  3. No caching enabled
     Impact: HIGH
     💡 Fix: Make task cacheable (see cacheability section)
     Estimated improvement: 42s → 0s (with cache hit)

Optimization Potential:
  Current: 42s
  With parallelization: 12s (71% faster)
  With caching: 0s on cache hit (100% faster)
  Combined potential savings: 42s per build
```

## Complete Task Review Example

```
===============================================
    TASK REVIEW REPORT
===============================================

Task: :app:generateBuildInfo
Class: com.example.tasks.BuildInfoTask
Location: buildSrc/src/main/kotlin/BuildInfoTask.kt

Overall Score: 4.2/10

SUMMARY:
  ✅ 3 checks passed
  ⚠️  5 warnings
  ❌ 4 critical issues

CRITICAL ISSUES:
  1. NOT CACHEABLE - Missing @CacheableTask
     Impact: Task re-runs on every build
     Time cost: 42s per build
     Fix: Add @CacheableTask annotation

  2. ABSOLUTE PATHS - Breaks cache portability
     Line 28: val config = File("/absolute/path/config.json")
     Fix: Use relative path with layout.projectDirectory

  3. NO INPUT ANNOTATIONS - Config dir not tracked
     Line 25: val configDir = File("config")
     Fix: Add @get:InputDirectory annotation

  4. CROSS-PROJECT DEPENDENCY - Creates tight coupling
     Line 72: dependsOn(project(":lib").tasks.named("build"))
     Fix: Use outputs as inputs

WARNINGS:
  1. No incremental build support (potential 90% speedup)
  2. Sequential processing (potential 70% speedup)
  3. Missing path sensitivity on inputs
  4. Eager task realization in dependencies
  5. Non-deterministic timestamp in output

RECOMMENDATIONS (Prioritized):

HIGH PRIORITY (Do First):
  1. Make task cacheable
     □ Add @CacheableTask annotation
     □ Fix absolute paths to relative
     □ Add missing input/output annotations
     □ Set path sensitivity to RELATIVE
     Estimated impact: 100% faster with cache hits

  2. Fix critical annotations
     □ Add @InputDirectory to configDir
     □ Add @PathSensitive to all file inputs
     □ Mark timestamp as @Internal
     Estimated impact: Correct build behavior

MEDIUM PRIORITY:
  3. Add incremental build support
     □ Add InputChanges parameter
     □ Implement change detection
     □ Handle ADDED/MODIFIED/REMOVED
     Estimated impact: 90% faster incremental builds

  4. Add parallel processing
     □ Use Worker API for file processing
     □ Parallelize independent operations
     Estimated impact: 70% faster execution

LOW PRIORITY:
  5. Improve task dependencies
     □ Use lazy task references
     □ Remove cross-project dependencies
     □ Use outputs as inputs

  6. Add better logging
     □ Log processed file count
     □ Add progress indicators
     □ Include timing information

SUGGESTED FIXES:

Fix 1: Add Cacheability
──────────────────────────────────────────────
// Before
class BuildInfoTask : DefaultTask() {

// After
@CacheableTask
abstract class BuildInfoTask : DefaultTask() {


Fix 2: Fix Absolute Path
──────────────────────────────────────────────
// Before
val config = File("/absolute/path/config.json")

// After
@get:InputFile
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val config: RegularFileProperty


Fix 3: Add Input Annotations
──────────────────────────────────────────────
// Before
val configDir = File("config")

// After
@get:InputDirectory
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val configDir: DirectoryProperty


Fix 4: Add Incremental Support
──────────────────────────────────────────────
@TaskAction
fun generate(inputChanges: InputChanges) {
  if (!inputChanges.isIncremental) {
    // Full rebuild
    processAllFiles()
  } else {
    // Process only changes
    inputChanges.getFileChanges(inputFiles).forEach { change ->
      when (change.changeType) {
        ChangeType.ADDED, ChangeType.MODIFIED -> process(change.file)
        ChangeType.REMOVED -> deleteOutput(change.file)
      }
    }
  }
}


IMPACT ANALYSIS:

If all recommendations are applied:
  Current build time: 42s
  Optimized build time: 5s (with cache miss)
  Cached build time: 0s (with cache hit)

  Total improvement potential: 88% faster (uncached)
                               100% faster (cached)

  Annual time savings (1000 builds/year):
    Developer time: 10.3 hours
    CI/CD time: 11.7 hours
    Cost savings: ~$500/year per developer

===============================================
Report saved to: build/reports/task-review/generateBuildInfo.txt
HTML report: build/reports/task-review/generateBuildInfo.html
===============================================

Would you like to:
  1. Apply automatic fixes (where possible)
  2. Generate corrected task implementation
  3. See detailed code examples
  4. Review another task

Enter choice (1-4):
```

## Auto-Fix Capability

For simple issues, the command can generate fixed code:

```
/reviewTask generateBuildInfo --auto-fix

Analyzing task...
Found 4 critical issues and 5 warnings.

Generating fixes...

✅ Generated fixed implementation: buildSrc/src/main/kotlin/BuildInfoTask.kt.fixed
✅ Generated migration guide: build/reports/task-review/migration-guide.md

Review the fixed implementation and apply if satisfactory:
  mv buildSrc/src/main/kotlin/BuildInfoTask.kt.fixed buildSrc/src/main/kotlin/BuildInfoTask.kt

Or use interactive mode:
  /reviewTask generateBuildInfo --interactive
```

## Related

- `/createTask` - Create new tasks with best practices
- `/doctor` - Comprehensive build health check
- See `gradle-task-development` skill for task development guidance
- See `gradle-cache-optimization` skill for caching best practices
