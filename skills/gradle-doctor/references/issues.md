# Common Issues Detected

Issues identified during health analysis.

## Performance Issues

| Issue | Impact | Fix |
|-------|--------|-----|
| Long configuration phase (>20%) | Slow every build | Lazy task registration |
| No parallel execution | Underutilized CPU | Enable org.gradle.parallel |
| Build cache disabled | Slow clean builds | Enable org.gradle.caching |
| Configuration cache off | Slow incremental | Enable configuration-cache |
| Poor JVM arguments | Memory issues | Optimize -Xmx, GC settings |
| Eager task creation | Slow configuration | Use tasks.register() |
| Config-time resolution | Slow configuration | Use Provider API |

## Cache Issues

| Issue | Impact | Fix |
|-------|--------|-----|
| Task.project in execution | Cache incompatible | Capture during config |
| Non-serializable captures | Cache incompatible | Use Property API |
| System.getProperty() calls | Cache incompatible | Use providers.systemProperty() |
| Build listeners | Cache incompatible | Use BuildService |
| Mutable shared state | Cache incompatible | Use BuildService |

## Dependency Issues

| Issue | Impact | Fix |
|-------|--------|-----|
| Version conflicts | Runtime errors | Add constraints |
| Duplicate dependencies | Bloated classpath | Exclude duplicates |
| No version catalog | Inconsistent versions | Create libs.versions.toml |
| Missing constraints | Unpredictable versions | Add dependency constraints |
| Transitive bloat | Large artifacts | Review and exclude |

## Structure Issues

| Issue | Impact | Fix |
|-------|--------|-----|
| Monolithic build files | Hard to maintain | Split into plugins |
| No convention plugins | Code duplication | Create buildSrc conventions |
| Cross-project config | Slow configuration | Use typed dependencies |
| Poor task organization | Confusion | Use groups and descriptions |
| No buildSrc | Repeated logic | Extract to buildSrc |

## Priority Matrix

| Priority | Criteria |
|----------|----------|
| 🔴 HIGH | Breaks builds, security, or causes errors |
| 🟡 MEDIUM | Performance impact, maintenance burden |
| 🟢 LOW | Best practice, future-proofing |

## Effort Estimation

| Effort | Time | Examples |
|--------|------|----------|
| QUICK | <30 min | Enable property, simple fix |
| MODERATE | 1-4 hours | Refactor tasks, add plugins |
| SIGNIFICANT | 1+ days | Major restructuring |
