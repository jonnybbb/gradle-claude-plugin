---
description: Migrate Gradle build to a newer version
argument-hint: [target-version]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

Use the gradle-migration skill to analyze the current Gradle project and migrate to the specified target version.

Arguments provided: $ARGUMENTS

If no target version specified, detect the current version and recommend migrating to the latest stable Gradle version (8.11 as of late 2024).

Steps:
1. Detect current Gradle version
2. Identify breaking changes between versions
3. Check deprecated API usage
4. Provide migration steps with code changes
5. Verify configuration cache compatibility
