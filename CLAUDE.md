# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Claude Code plugin** providing Gradle expertise through skills, commands, agents, and JBang tools. It helps users with Gradle performance tuning, build optimization, task/plugin development, troubleshooting, and version migration (Gradle 6-9+).

## Plugin Structure

This follows Claude Code plugin conventions:
- `.claude-plugin/plugin.json` - Main plugin manifest
- `skills/*/SKILL.md` - Auto-discovered skills (10 total)
- `commands/*.md` - Auto-discovered slash commands
- `agents/*.md` - Claude Code markdown agents (doctor, migration)
- `tools/*.java` - JBang tools using Gradle Tooling API
- `docs/reference/` - Reference documentation (skills link to these)

## Key Commands

### JBang Tools
```bash
# Analyze Gradle project structure
jbang tools/gradle-analyzer.java /path/to/project [--json]

# Validate cache configuration
jbang tools/cache-validator.java /path/to/project [--fix]

# Profile build performance
jbang tools/performance-profiler.java /path/to/project [--report]

# Analyze task implementations
jbang tools/task-analyzer.java /path/to/project [--json]

# Health check
jbang tools/build-health-check.java /path/to/project
```

### Testing
```bash
# Setup test fixtures
cd tests/fixtures
./scripts/setup-fixtures.sh

# Run tool tests against fixtures
./scripts/test-tools.sh

# Manual tool test
jbang tools/build-health-check.java tests/fixtures/projects/simple-java
```

## Architecture Layers

```
Presentation (skills/SKILL.md) → User-facing skill definitions
Orchestration (agents/*.md)    → Multi-step workflow coordination
Tools (tools/*.java)           → JBang scripts using Gradle Tooling API
Documentation (docs/reference) → Curated Gradle knowledge
```

Skills reference documentation files using relative paths to minimize context window usage. When editing a skill, check its `references/` subdirectory for related docs.

## Tool Development

JBang tools use the Gradle Tooling API and require:
- Java 25+
- `//DEPS org.gradle:gradle-tooling-api:9.2.1`
- Support both human-readable and `--json` output
- Handle connection failures gracefully

## Test Fixtures

Located in `tests/fixtures/projects/`:
- `simple-java` - Healthy baseline (Gradle 9.2.1, Java 25, all best practices)
- `config-cache-broken` - Intentional issues for detection testing
- `legacy-groovy` - Gradle 7.6.4 for migration testing (Java 11)
- `multi-module` - 4-module project for scale testing
- `spring-boot` - Framework compatibility testing

Expected outputs in `tests/fixtures/expected-outputs/*.json`.

## Requirements

- Java 25+ (for tools and tests)
- JBang 0.134.0+
- Gradle 6.0+ (for analyzed projects)
