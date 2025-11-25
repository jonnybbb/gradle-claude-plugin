---
description: Scaffold a new Gradle plugin with best practices
argument-hint: <plugin-name> [plugin-id]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

Use the gradle-plugin-development skill to scaffold a new Gradle plugin.

Arguments provided: $ARGUMENTS

If no plugin name provided, ask the user for a plugin name.
If no plugin ID provided, derive it from the plugin name (e.g., com.example.plugin-name).

Include:
1. Plugin class with proper extension registration
2. Convention plugin pattern if appropriate
3. TestKit-based functional tests
4. Proper Gradle plugin marker artifact setup
5. README with usage instructions
