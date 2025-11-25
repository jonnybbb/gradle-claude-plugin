# Remote Build Cache Setup

Configure remote build cache for team sharing.

## HTTP Build Cache

### Basic Setup

```kotlin
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/cache/")
        isPush = System.getenv("CI") != null  // Push from CI only
        
        credentials {
            username = System.getenv("CACHE_USER") ?: ""
            password = System.getenv("CACHE_TOKEN") ?: ""
        }
    }
}
```

### Gradle Enterprise

```kotlin
plugins {
    id("com.gradle.enterprise") version "3.16"
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

## Push Strategy

### Push from CI Only

```kotlin
remote<HttpBuildCache> {
    url = uri("https://cache.example.com")
    
    // Only push from main branch
    val isMainBranch = System.getenv("CI_BRANCH") == "main"
    val isCI = System.getenv("CI") != null
    isPush = isCI && isMainBranch
}
```

## Self-Hosted Options

### Nginx + WebDAV

```nginx
server {
    listen 443 ssl;
    server_name cache.example.com;
    
    location /cache/ {
        dav_methods PUT DELETE;
        client_max_body_size 100M;
        root /var/cache/gradle;
    }
}
```

### Docker Build Cache Node

```bash
docker run -d \
  -p 5071:5071 \
  -v cache-data:/data \
  gradle/build-cache-node:latest
```

## CI/CD Examples

### GitHub Actions

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Cache
        env:
          CACHE_USER: ${{ secrets.CACHE_USER }}
          CACHE_TOKEN: ${{ secrets.CACHE_TOKEN }}
          CI: true
        run: ./gradlew build --build-cache
```

### GitLab CI

```yaml
build:
  script:
    - ./gradlew build --build-cache
  variables:
    CACHE_USER: $CACHE_USER
    CACHE_TOKEN: $CACHE_TOKEN
    CI: "true"
```

## Security

- Always use HTTPS
- Use authentication
- Isolate caches per project/team
- Rotate credentials regularly
