# JVM Arguments Reference

**Gradle Version**: 7.0+

## Overview

JVM arguments optimize Gradle daemon and build execution performance.

## Setting JVM Arguments

**gradle.properties:**
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

## Memory Settings

### Heap Size

```properties
# Initial heap
-Xms2g

# Maximum heap
-Xmx4g

# Small projects
org.gradle.jvmargs=-Xmx2g

# Medium projects (10-50 modules)
org.gradle.jvmargs=-Xmx4g

# Large projects (50+ modules)
org.gradle.jvmargs=-Xmx8g
```

### Metaspace

```properties
# Initial metaspace
-XX:MetaspaceSize=512m

# Maximum metaspace
-XX:MaxMetaspaceSize=1g
```

### Direct Memory

```properties
# For builds with lots of I/O
-XX:MaxDirectMemorySize=2g
```

## Garbage Collection

### Parallel GC (Default)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

Good for throughput.

### G1GC

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

Good for large heaps, lower pause times.

### ZGC (Java 15+)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseZGC
```

Ultra-low pause times.

### Shenandoah (Java 12+)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseShenandoahGC
```

Low pause times.

## Debugging

### Heap Dump on OOM

```properties
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp
```

### GC Logging

```properties
# Java 8
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:gc.log

# Java 9+
-Xlog:gc*:file=gc.log:time,level,tags
```

### Remote Debugging

```properties
org.gradle.jvmargs=-Xmx4g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

Then attach debugger to port 5005.

## Performance Tuning

### Fast Startup

```properties
org.gradle.jvmargs=-Xmx4g \
  -XX:+UseParallelGC \
  -XX:+AggressiveOpts \
  -XX:+UseFastAccessorMethods
```

### Compiler Optimization

```properties
# Enable tiered compilation
-XX:+TieredCompilation

# More compiler threads
-XX:CICompilerCount=4
```

### String Optimization

```properties
# String deduplication (G1GC only)
-XX:+UseStringDeduplication
```

## Common Configurations

### Minimal (2GB)

```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### Recommended (4GB)

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC
```

### Large Projects (8GB+)

```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### CI/CD

```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC
```

## Troubleshooting

### OutOfMemoryError: Java heap space

Increase `-Xmx`:
```properties
org.gradle.jvmargs=-Xmx6g
```

### OutOfMemoryError: Metaspace

Increase metaspace:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=2g
```

### Slow GC

Switch GC algorithm:
```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
```

### High Memory Usage

- Reduce max heap
- Increase worker count
- Enable parallel GC

## Monitoring

### View Current Settings

```bash
./gradlew --status

# Shows daemon JVM args
```

### JVM Metrics

```bash
# With build scan
./gradlew build --scan

# View JVM metrics in scan
```

## Platform-Specific

### Windows

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Linux/Mac

```properties
# gradle.properties (same)
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### M1/M2 Mac

```properties
# Optimized for ARM
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

## Related Documentation

- [Daemon Optimization](daemon-optimization.md): Daemon tuning
- [Performance Tuning](performance-tuning.md): Overall optimization

## Quick Reference

```properties
# Optimal for most projects
org.gradle.jvmargs=-Xmx4g \
  -XX:MaxMetaspaceSize=1g \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:+UseParallelGC

# Debug
org.gradle.jvmargs=-Xmx4g \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```
