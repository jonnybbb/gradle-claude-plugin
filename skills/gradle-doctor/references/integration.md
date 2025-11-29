# Tool and Skill Integration

## Tool Invocation

### build-health-check.java
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/build-health-check.java <project-dir> [--json]
```

Outputs:
- Overall health score
- Category breakdown (Performance, Caching, Structure, Task Quality, Dependencies)
- Top recommendations

### cache-validator.java
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/cache-validator.java <project-dir> [--json] [--fix]
```

Checks:
- Build cache configuration
- Configuration cache compatibility
- Cache hit potential

### performance-fixer.java
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/performance-fixer.java <project-dir> [--json] [--auto]
```

Outputs:
- Performance optimizations with impact ratings
- Auto-fixable issues
- Expected improvement estimates

### task-analyzer.java
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/task-analyzer.java <project-dir> [--json]
```

Outputs:
- Task registration patterns (eager vs lazy)
- Configuration cache compatibility issues
- Lazy registration score

## Skill Delegation

| Issue Category | Delegate To |
|----------------|-------------|
| Slow configuration | gradle-performance |
| Cache problems | gradle-config-cache, gradle-build-cache |
| Dependency conflicts | gradle-dependencies |
| Structure issues | gradle-structure |
| Deprecation warnings | gradle-migration |
| Build failures | gradle-troubleshooting |

## Aggregation Pattern

Doctor orchestrates by:
1. Running tools for data collection
2. Analyzing results across categories
3. Delegating deep analysis to specialized skills
4. Synthesizing prioritized recommendations
