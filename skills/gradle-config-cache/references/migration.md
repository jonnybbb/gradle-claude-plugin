# Configuration Cache Migration Strategy

Step-by-step guide for migrating builds to full configuration cache support.

## Migration Phases

### Phase 1: Assessment

1. **Enable with warnings**:
```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

2. **Run full build**:
```bash
./gradlew build
```

3. **Count and categorize problems**:
- Task.project access
- System property access
- Non-serializable state
- Build listeners
- Third-party plugin issues

### Phase 2: Quick Wins

Fix easy issues first (typically 60-80% of problems):

1. **Property captures** - Most common, easiest fix
2. **Provider API usage** - Replace System.getProperty/getenv
3. **Typed tasks** - Convert ad-hoc tasks with project access

### Phase 3: Complex Fixes

1. **Custom task classes** - Add service injection
2. **BuildService migration** - Replace shared state and listeners
3. **Third-party plugins** - Update or find alternatives

### Phase 4: Validation

1. **Enable strict mode**:
```properties
org.gradle.configuration-cache.problems=fail
```

2. **Test all scenarios**:
```bash
./gradlew clean build
./gradlew test
./gradlew assemble
```

3. **Verify cache hits**:
```bash
# First run (stores cache)
./gradlew build

# Second run (should be instant)
./gradlew build
# Look for "Reusing configuration cache"
```

## Task Compatibility Checklist

Before a task is configuration cache compatible:

- [ ] No `project` access in `@TaskAction`
- [ ] No `project` access in `doLast`/`doFirst`
- [ ] No `System.getProperty()` at execution time
- [ ] No `System.getenv()` at execution time
- [ ] No file reading without `@InputFile`
- [ ] All inputs use Property API
- [ ] All services injected (fs, exec, archive)
- [ ] No mutable shared state
- [ ] No build listeners

## CI/CD Integration

### GitHub Actions

```yaml
jobs:
  build:
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Configuration Cache
        run: ./gradlew build --configuration-cache
        
      - name: Upload cache report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: config-cache-report
          path: build/reports/configuration-cache/
```

### GitLab CI

```yaml
build:
  script:
    - ./gradlew build --configuration-cache
  artifacts:
    when: on_failure
    paths:
      - build/reports/configuration-cache/
```

## Measuring Progress

Track migration progress:

```bash
# Count problems
./gradlew build --configuration-cache 2>&1 | grep -c "problem"

# Before: 47 problems
# After quick wins: 12 problems
# After complex fixes: 0 problems
```

## Rollback Plan

If issues arise in production:

```properties
# Disable temporarily
org.gradle.configuration-cache=false
```

Or per-build:
```bash
./gradlew build --no-configuration-cache
```

## Timeline Expectations

| Project Size | Assessment | Quick Wins | Complex | Total |
|--------------|------------|------------|---------|-------|
| Small (<10 modules) | 1 hour | 2 hours | 2 hours | 5 hours |
| Medium (10-50) | 2 hours | 4 hours | 8 hours | 14 hours |
| Large (50+) | 4 hours | 8 hours | 16+ hours | 28+ hours |

## Success Criteria

Migration complete when:
- [ ] `--configuration-cache` passes without warnings
- [ ] "Reusing configuration cache" appears on second run
- [ ] All CI/CD pipelines use configuration cache
- [ ] Build time reduced by 30%+ on incremental builds
