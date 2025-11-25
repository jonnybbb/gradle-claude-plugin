# JVM Tuning

Optimize JVM settings for Gradle daemon.

## Memory Settings

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

### Sizing Recommendations

| Project Size | Heap (-Xmx) | Metaspace |
|--------------|-------------|-----------|
| Small (<10 modules) | 2g | 512m |
| Medium (10-50) | 4g | 1g |
| Large (50+) | 8g+ | 2g |

## Garbage Collection

### Parallel GC (Default, recommended)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### G1GC (Large heaps)

```properties
org.gradle.jvmargs=-Xmx8g -XX:+UseG1GC
```

### ZGC (Java 15+, low latency)

```properties
org.gradle.jvmargs=-Xmx8g -XX:+UseZGC
```

## Common Configurations

### Minimal (Small projects)

```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseParallelGC
```

### Recommended (Most projects)

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC
```

### Large Projects

```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=2g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseG1GC
```

### CI/CD

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8
```

## Troubleshooting

### OutOfMemoryError: Java heap space

```properties
# Increase heap
org.gradle.jvmargs=-Xmx6g
```

### OutOfMemoryError: Metaspace

```properties
# Increase metaspace
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=2g
```

### Slow GC

```bash
# Enable GC logging
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g -Xlog:gc*"
```

### High Memory Usage

```bash
# Check current settings
./gradlew --status

# Stop daemons
./gradlew --stop
```

## Monitoring

```bash
# View current JVM args
./gradlew properties | grep jvmargs

# Build with GC details
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g -XX:+PrintGCDetails"
```
