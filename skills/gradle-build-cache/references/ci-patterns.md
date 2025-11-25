# CI/CD Build Cache Patterns

## GitHub Actions

```yaml
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v3
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}

- name: Build
  run: ./gradlew build --build-cache
```

## GitLab CI

```yaml
variables:
  GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle

cache:
  key: gradle-$CI_COMMIT_REF_SLUG
  paths:
    - .gradle/caches
    - .gradle/wrapper

build:
  script:
    - ./gradlew build --build-cache
```

## Jenkins

```groovy
pipeline {
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }
  stages {
    stage('Build') {
      steps {
        sh './gradlew build --build-cache'
      }
    }
  }
}
```

## Remote Cache Configuration

```kotlin
// settings.gradle.kts
buildCache {
    local { isEnabled = true }
    remote<HttpBuildCache> {
        url = uri(System.getenv("GRADLE_CACHE_URL") ?: "https://cache.example.com/")
        isPush = System.getenv("CI") != null
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```

## Best Practices

1. **Push only from CI main branch** - Prevent cache pollution
2. **Read-only for PRs** - Faster, safer
3. **Separate caches per major version** - Avoid incompatibility
4. **Monitor cache hit rate** - Track via build scans
