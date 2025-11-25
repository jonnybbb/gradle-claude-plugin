# Gradle Daemon Optimization

**Gradle Version**: 7.0+

## Overview

The Gradle daemon is a long-lived background process that improves build performance by avoiding startup overhead.

## Enable Daemon

**gradle.properties:**
```properties
org.gradle.daemon=true
org.gradle.daemon.idletimeout=3600000
```

## JVM Arguments

Optimize daemon JVM:

```properties
org.gradle.jvmargs=-Xmx4g \
  -XX:MaxMetaspaceSize=1g \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:+UseParallelGC \
  -XX:MaxGCPauseMillis=200
```

### Memory Sizing

**Small projects (<10 modules):**
```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

**Medium projects (10-50 modules):**
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

**Large projects (50+ modules):**
```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=2g
```

## Daemon Management

```bash
# Status
./gradlew --status

# Stop all daemons
./gradlew --stop

# Run without daemon (debugging)
./gradlew build --no-daemon
```

## Performance Impact

**First build (cold):**
- No daemon: 60s (includes JVM startup)
- With daemon: 45s

**Subsequent builds:**
- No daemon: 60s (JVM startup each time)
- With daemon: 35s (no startup)

## Best Practices

1. **Always enable daemon**
2. **Set appropriate heap size**
3. **Use --stop after major changes**
4. **Monitor with --status**
5. **One daemon per Gradle version**

## Troubleshooting

### OutOfMemoryError

Increase heap:
```properties
org.gradle.jvmargs=-Xmx6g
```

### Slow GC

Use better GC:
```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
```

## Quick Reference

```properties
# Optimal daemon config
org.gradle.daemon=true
org.gradle.daemon.idletimeout=3600000
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC
```
