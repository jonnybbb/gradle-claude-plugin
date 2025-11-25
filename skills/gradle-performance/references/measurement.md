# Performance Measurement

## Before/After Comparison

### Establish Baseline
```bash
# Run 3 times, take average
./gradlew clean
time ./gradlew build --no-build-cache

# Record metrics
./gradlew build --profile
# Check build/reports/profile/
```

### Key Metrics to Track

| Metric | Command | Target |
|--------|---------|--------|
| Total build time | time ./gradlew build | <2min |
| Configuration time | --profile report | <5% of total |
| Incremental build | time ./gradlew build | <10s |
| No-op build | time ./gradlew build | <1s |

### Build Scan Comparison

```bash
# Baseline scan
./gradlew build --scan

# After optimization
./gradlew build --scan

# Compare scans at gradle.com
```

## Benchmarking Script

```bash
#!/bin/bash
RUNS=3
echo "Benchmarking $RUNS runs..."

for i in $(seq 1 $RUNS); do
  ./gradlew clean > /dev/null
  time ./gradlew build --no-build-cache 2>&1 | grep real
done
```

## Performance Regression Detection

```properties
# gradle.properties - fail on slow builds
org.gradle.configuration-cache.problems=fail
```

Monitor in CI:
- Track build times over commits
- Alert on >20% regression
- Use build scan API for automation
