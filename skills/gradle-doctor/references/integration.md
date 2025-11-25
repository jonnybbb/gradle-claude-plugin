# Tool and Skill Integration

## Tool Invocation

### gradle-analyzer.java
```bash
jbang tools/gradle-analyzer.java <project-dir> [--json]
```

Outputs:
- Project structure (modules, subprojects)
- Gradle/Java versions
- Plugin list
- Task count

### cache-validator.java
```bash
jbang tools/cache-validator.java <project-dir> [--fix]
```

Checks:
- Build cache configuration
- Configuration cache compatibility
- Cache hit potential

### performance-profiler.java
```bash
jbang tools/performance-profiler.java <project-dir> [--report]
```

Outputs:
- Configuration vs execution time ratio
- Slowest tasks
- Parallelization opportunities

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
