---
description: Analyze a Gradle project for best practices compliance and provide recommendations
argument-hint: [project-path]
allowed-tools: Read, Glob, Grep, Bash
---

Use the gradle-best-practices skill to analyze the current Gradle project for best practices compliance.

Arguments provided: $ARGUMENTS

If no project path is provided, use the current working directory.

## Analysis Steps

1. **Locate Gradle Files**
   - Find settings.gradle(.kts) to determine project root
   - Identify all build.gradle(.kts) files
   - Check for gradle.properties files

2. **Check Each Best Practice**

   a. **Kotlin DSL Usage**
   - Look for `.gradle` files (Groovy) vs `.gradle.kts` files (Kotlin)
   - Report percentage of Kotlin DSL adoption
   - Note any Groovy files that should be migrated

   b. **Gradle Version**
   - Check `gradle/wrapper/gradle-wrapper.properties` for current version
   - Compare against latest available version
   - Warn if using unsupported or outdated minor version

   c. **Plugin Application**
   - Search for `apply plugin` or `apply(plugin =` patterns (legacy)
   - Verify plugins are applied via `plugins {}` block
   - Check for buildscript dependency declarations

   d. **Internal API Usage**
   - Search for imports from `org.gradle.*internal*` packages
   - Look for types ending in `Internal` or `Impl`
   - List all violations with file locations

   e. **gradle.properties Location**
   - Verify gradle.properties exists in root
   - Check for gradle.properties in subprojects (violation)
   - Review recommended properties are set

   f. **Root Project Naming**
   - Check settings.gradle.kts for `rootProject.name`
   - Warn if not explicitly set

3. **Generate Report**

   Provide a summary with:
   - Overall compliance score (percentage)
   - Issues found grouped by category
   - Specific file:line references for violations
   - Actionable recommendations for each issue

## Output Format

```
=== Gradle Best Practices Analysis ===

Project: <project-name>
Gradle Version: <current> (latest: <latest>)

Compliance Score: X/7 practices

✓ Using Kotlin DSL (or ✗ with details)
✓ Latest minor version (or ✗ with recommendation)
✓ plugins {} block usage (or ✗ with violations)
✓ No internal APIs (or ✗ with violations)
✓ Root gradle.properties (or ✗ with issues)
✓ Root project named (or ✗ with fix)
✓ No subproject gradle.properties (or ✗ with locations)

[Detailed findings and recommendations for each violation]
```
