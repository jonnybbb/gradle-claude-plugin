---
name: gradle-doctor
description: This skill should be used when the user asks to "check Gradle build health", "run build diagnostics", "analyze my Gradle project", "what's wrong with my build", "assess build quality", or runs the /doctor command, reports multiple build issues, wants overall build optimization, or needs initial project assessment.
---

# Gradle Doctor

## Overview

Comprehensive health analysis using specialized tools and subagents. Provides actionable recommendations for performance, caching, dependencies, and structure.

For analysis workflow details, see [references/workflow.md](references/workflow.md).
For common issues detected, see [references/issues.md](references/issues.md).

## Commands

### `/doctor [options]`

```bash
# Basic health check
/doctor

# Quick scan (essential checks only)
/doctor --quick

# Deep analysis with auto-fix
/doctor --deep --fix

# Generate detailed report
/doctor --report
```

## Quick Start

Run `/doctor` to get:
- Overall health score
- Performance analysis
- Cache configuration check
- Dependency conflict detection
- Structure review
- Prioritized recommendations

## Tool Integration

| Tool | Purpose |
|------|---------|
| gradle-analyzer.java | Project metadata extraction |
| cache-validator.java | Cache configuration validation |
| performance-profiler.java | Build performance analysis |

## Health Categories

| Category | Checks |
|----------|--------|
| Performance | Configuration time, task execution, parallelization |
| Caching | Build cache, configuration cache, compatibility |
| Dependencies | Conflicts, duplicates, version management |
| Structure | Organization, conventions, patterns |

## Interpreting Results

| Status | Meaning |
|--------|---------|
| ✅ Healthy | No action needed |
| ⚠️ Needs Attention | Optimization opportunities |
| ❌ Critical | Immediate action required |

## Auto-Fix Safety

- Only applies safe, reversible changes
- Creates backups before changes
- Suggests manual review for complex issues
- Provides rollback instructions

## Related Skills

Delegates to specialized skills:
- **gradle-performance**: Detailed performance analysis
- **gradle-config-cache**: Configuration cache troubleshooting
- **gradle-build-cache**: Build cache optimization
- **gradle-dependencies**: Dependency management
- **gradle-structure**: Build organization

## Related Files

- [references/workflow.md](references/workflow.md) - Analysis phases
- [references/issues.md](references/issues.md) - Common issues
- [references/integration.md](references/integration.md) - Tool/skill integration
