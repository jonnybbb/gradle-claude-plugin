# Gradle Daemon Optimization

## Heap Size Recommendations

By project size:
- **Small** (<10 modules): 2GB (`-Xmx2g`)
- **Medium** (10-30 modules): 4GB (`-Xmx4g`)
- **Large** (30-100 modules): 8GB (`-Xmx8g`)
- **Very Large** (100+ modules): 12GB+ (`-Xmx12g`)

## Configuration

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true
org.gradle.daemon.idletimeout=3600000
```

## Daemon Management

```bash
# Check daemon status
gradle --status

# Stop all daemons
gradle --stop

# View daemon logs
cat ~/.gradle/daemon/*/daemon-*.out.log
```

## Health Issues

**Symptoms:**
- Slow builds
- OutOfMemoryError
- Daemon crashes

**Solutions:**
1. Increase heap size
2. Stop and restart daemon
3. Check for memory leaks in build logic
4. Enable heap dumps for analysis
