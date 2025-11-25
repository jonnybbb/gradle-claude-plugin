# Build Scan Analysis

**Source**: https://scans.gradle.com  
**Gradle Version**: 7.0+

## Overview

Build scans provide detailed insights into build performance, dependencies, and failures.

## Generating Scans

```bash
# Generate scan
./gradlew build --scan

# Always generate
gradle.properties:
org.gradle.caching=true
```

## Key Sections

### Timeline

- Task execution order
- Parallel execution visualization
- Critical path analysis
- Task duration

### Performance

- Configuration time
- Execution time
- Dependency resolution time
- Task execution breakdown

### Build Cache

- Cache hit rate
- Cache misses
- Time saved
- Cache operations

### Dependencies

- Dependency tree
- Conflict resolution
- Version selection
- Repository access

### Tests

- Test execution
- Test failures
- Test duration
- Test output

## Using Scans for Debugging

### Performance Issues

1. Open Timeline view
2. Find slowest tasks
3. Analyze critical path
4. Check for parallelization opportunities

### Cache Issues

1. Open Build Cache view
2. Check hit rate
3. Identify non-cacheable tasks
4. Review cache operations

### Dependency Issues

1. Open Dependencies view
2. Search for dependency
3. See version conflicts
4. View resolution reason

## Comparing Scans

```bash
# Generate baseline
./gradlew build --scan

# Make changes

# Generate comparison
./gradlew build --scan
```

Compare URLs to see performance differences.

## Best Practices

1. **Generate regularly** for baseline
2. **Share with team** for collaboration
3. **Use for CI/CD** analysis
4. **Compare before/after** changes

## Related Documentation

- [Debugging Builds](debugging-builds.md): Debugging techniques
- [Performance Tuning](performance-tuning.md): Optimization

## Quick Reference

```bash
# Generate scan
./gradlew build --scan

# Key views:
# - Timeline: Task execution
# - Performance: Bottlenecks
# - Build Cache: Cache effectiveness
# - Dependencies: Resolution
# - Tests: Test results
```
