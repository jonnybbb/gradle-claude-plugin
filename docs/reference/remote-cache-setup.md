# Remote Build Cache Setup

**Source**: https://docs.gradle.org/current/userguide/build_cache.html  
**Gradle Version**: 7.0+

## Overview

Remote build cache enables cache sharing across team members and CI/CD.

## HTTP Cache Server

### Basic Setup

```kotlin
// settings.gradle.kts
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = System.getenv("CI") != null
    }
}
```

### With Authentication

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = true
        
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```

## Gradle Enterprise Cache

```kotlin
plugins {
    id("com.gradle.enterprise") version "3.15"
}

gradleEnterprise {
    buildCache {
        remote(gradleEnterprise.buildCache) {
            isEnabled = true
            isPush = System.getenv("CI") != null
        }
    }
}
```

## Local + Remote Strategy

```kotlin
buildCache {
    local {
        isEnabled = true
        removeUnusedEntriesAfterDays = 7
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        // Pull everywhere, push from CI only
        isPush = System.getenv("CI") != null
        
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```

## CI/CD Configuration

### GitHub Actions

```yaml
name: Build

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Build with cache
        env:
          CACHE_USER: ${{ secrets.CACHE_USER }}
          CACHE_PASSWORD: ${{ secrets.CACHE_PASSWORD }}
          CI: true
        run: ./gradlew build --build-cache
```

### GitLab CI

```yaml
build:
  stage: build
  script:
    - ./gradlew build --build-cache
  variables:
    CACHE_USER: $CACHE_USER
    CACHE_PASSWORD: $CACHE_PASSWORD
    CI: "true"
  cache:
    paths:
      - .gradle/caches
```

## Self-Hosted Cache Server

### Using Build Cache Node

```bash
# Start cache server
java -jar build-cache-node.jar

# Configure clients
```

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("http://cache-server:5071")
        isPush = true
    }
}
```

### Using nginx + WebDAV

```nginx
location /cache {
    dav_methods PUT DELETE MKCOL COPY MOVE;
    dav_access user:rw group:rw all:r;
    
    create_full_put_path on;
    client_max_body_size 1G;
}
```

## Cache Backend Options

### S3-Compatible Storage

Custom build cache backend:

```kotlin
interface S3BuildCache : BuildCache {
    var bucket: String
    var region: String
}

class S3BuildCacheService : BuildCacheService {
    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        // Load from S3
    }
    
    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        // Store to S3
    }
}
```

## Security Considerations

### HTTPS Only

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")  // Always HTTPS
    }
}
```

### Credentials Management

```kotlin
buildCache {
    remote<HttpBuildCache> {
        credentials {
            username = providers.environmentVariable("CACHE_USER")
                .getOrElse("")
            password = providers.environmentVariable("CACHE_TOKEN")
                .getOrElse("")
        }
    }
}
```

### Cache Isolation

Separate caches for:
- Main branch vs feature branches
- Release vs development builds
- Different teams/projects

```kotlin
buildCache {
    remote<HttpBuildCache> {
        val branch = System.getenv("CI_BRANCH") ?: "unknown"
        url = uri("https://cache.example.com/$branch")
    }
}
```

## Monitoring

### Cache Metrics

```bash
# Enable build scan
./gradlew build --scan

# View metrics:
# - Cache hit rate
# - Time saved
# - Cache usage
```

### Logging

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        // Log all cache operations
        logger.lifecycle("Remote cache: ${url.get()}")
    }
}
```

## Troubleshooting

### Cache Not Working

```bash
# Check configuration
./gradlew help --info | grep -i cache

# Test connection
curl -v https://cache.example.com
```

### Slow Cache

- Check network latency
- Use closer cache server
- Enable local cache

### Cache Misses

- Check task cacheability
- Verify cache key consistency
- Review build scan

## Best Practices

1. **Pull everywhere, push from CI**
2. **Use HTTPS + authentication**
3. **Monitor cache hit rate**
4. **Set appropriate cache retention**
5. **Test before production rollout**

## Related Documentation

- [Build Cache](build-cache.md): Fundamentals
- [Cache Compatibility](cache-compatibility.md): Issues
- [Performance Tuning](performance-tuning.md): Optimization

## Quick Reference

```kotlin
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
        removeUnusedEntriesAfterDays = 7
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = System.getenv("CI") != null
        
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```
