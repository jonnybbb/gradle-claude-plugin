---
description: Deep-dive analysis of specific build issues (flaky-tests, outcome-mismatch, failure-patterns)
allowed-tools: Task, AskUserQuestion
arguments:
  - name: topic
    description: "The topic to diagnose: flaky-tests, outcome-mismatch, or failure-patterns"
    required: false
---

# Diagnose - Deep-Dive Build Analysis

You are running the diagnose command. This command performs in-depth analysis of specific build issues using Develocity data.

## Available Topics

| Topic | Description | Agent |
|-------|-------------|-------|
| `flaky-tests` | Analyze flaky tests, identify patterns, suggest fixes | `flaky-test-analyzer` |
| `outcome-mismatch` | Investigate why same code has different outcomes (CI vs local, same commit) | `outcome-mismatch-analyzer` |
| `failure-patterns` | Identify recurring failures and their root causes | `failure-pattern-analyzer` |

## Routing Logic

### If no topic provided:

Show available topics and ask user to choose:

```
/diagnose - Deep-Dive Build Analysis

Available topics:

  flaky-tests       Analyze flaky tests, identify patterns, suggest fixes
  outcome-mismatch  Investigate different outcomes for same code (CI vs local)
  failure-patterns  Identify recurring failures and root causes

Usage: /diagnose <topic>

Examples:
  /diagnose flaky-tests
  /diagnose outcome-mismatch
  /diagnose failure-patterns

For a general overview of build health, use /build-insights instead.
```

### If topic = "flaky-tests":

Spawn the `flaky-test-analyzer` agent using the Task tool:

```
Use the Task tool with:
- subagent_type: "gradle:flaky-test-analyzer"
- prompt: "Analyze flaky tests for this project. Query Develocity for flaky test data, identify patterns, examine test source code if helpful, and provide actionable recommendations to fix the flakiness."
```

### If topic = "outcome-mismatch":

Spawn the `outcome-mismatch-analyzer` agent using the Task tool:

```
Use the Task tool with:
- subagent_type: "gradle:outcome-mismatch-analyzer"
- prompt: "Investigate why builds have different outcomes. Compare CI vs LOCAL builds, analyze environment differences, identify missing variables or configuration mismatches, and suggest how to align environments."
```

### If topic = "failure-patterns":

Spawn the `failure-pattern-analyzer` agent using the Task tool:

```
Use the Task tool with:
- subagent_type: "gradle:failure-pattern-analyzer"
- prompt: "Analyze failure patterns across builds. Query Develocity for failure groups, identify recurring issues, categorize by type (compilation, test, OOM, etc.), and prioritize fixes based on frequency and impact."
```

### If topic is unrecognized:

```
Unknown topic: "<topic>"

Available topics:
  - flaky-tests
  - outcome-mismatch
  - failure-patterns

Usage: /diagnose <topic>
```

## Prerequisites

All diagnose topics require the Develocity MCP server to be configured. If the spawned agent reports Develocity is unavailable, inform the user:

```
Develocity MCP server not configured.

The /diagnose command requires Develocity for build analysis.
Configure the Develocity MCP server in your Claude Code settings.

For local build script analysis, use /doctor instead.
```

## Related Commands

- `/build-insights` - General overview of build health (run this first)
- `/doctor` - Local build script health check (no Develocity needed)
