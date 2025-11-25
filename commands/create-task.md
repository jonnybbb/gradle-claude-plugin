---
description: Create a new custom Gradle task with proper caching and incremental support
argument-hint: <task-name> [task-type: simple|incremental|worker-api]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

Use the gradle-task-development skill to create a new custom Gradle task.

Arguments provided: $ARGUMENTS

If no task name provided, ask the user for a task name.

Task types:
- simple: Basic task with inputs/outputs (default)
- incremental: Task with incremental processing support
- worker-api: Parallel task using Worker API

Ensure the task:
1. Has proper input/output annotations for caching
2. Follows task avoidance API patterns
3. Is configuration-cache compatible
4. Includes appropriate documentation
