---
name: gradle-migration-assistant
description: Guides Gradle version migrations from 6/7 to 8/9, identifies deprecated APIs, provides upgrade paths, and ensures compatibility. Claude uses this when you ask about upgrading gradle, migration issues, or deprecated API replacements.
---

# Gradle Migration Assistant Skill

This skill enables Claude to guide Gradle version migrations, identify compatibility issues, and provide upgrade paths from Gradle 6/7 to 8/9.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask to "upgrade gradle" or "migrate to gradle 8"
- Report "deprecated API" warnings
- Want to "update gradle wrapper"
- Ask about "gradle compatibility" or "breaking changes"
- Need "migration guide" for Gradle versions

## Migration Paths

### Gradle 6.x → 8.x
Major changes: Java 8+ requirement, configuration cache, deprecated APIs removed

### Gradle 7.x → 8.x
Moderate changes: Some deprecated APIs removed, improved defaults

### Gradle 8.x → 9.x
Incremental changes: Future API enhancements, continued deprecation cleanup

## Migration Workflow

### Step 1: Update Wrapper

```bash
# Check current version
./gradlew --version

# Update to Gradle 8.5
./gradlew wrapper --gradle-version=8.5 --distribution-type=bin

# Verify update
./gradlew --version
```

### Step 2: Run with Warnings

```bash
# Build with deprecation warnings
./gradlew build --warning-mode=all

# Check for issues
./gradlew build --warning-mode=fail
```

### Step 3: Fix Deprecated APIs

**Common Deprecations:**

**1. Project conventions → Extensions**
```kotlin
// ❌ Deprecated (Gradle 7)
project.convention.getPlugin(JavaPluginConvention::class.java)

// ✅ Modern (Gradle 8+)
project.extensions.getByType<JavaPluginExtension>()
```

**2. BasePluginConvention → BasePluginExtension**
```kotlin
// ❌ Deprecated
project.convention.getPlugin(BasePluginConvention::class.java).archivesBaseName = "myapp"

// ✅ Modern
project.extensions.getByType<BasePluginExtension>().archivesName.set("myapp")
```

**3. Maven → Maven Publish**
```kotlin
// ❌ Deprecated: maven plugin
plugins {
    maven
}

// ✅ Modern: maven-publish plugin
plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
```

**4. compile → implementation**
```kotlin
// ❌ Deprecated
dependencies {
    compile("com.google.guava:guava:32.1.3-jre")
}

// ✅ Modern
dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
}
```

**5. Test task → Test Suites**
```kotlin
// ❌ Old approach
tasks.test {
    useJUnitPlatform()
}

// ✅ Modern with test suites (Gradle 7.3+)
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.1")
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}
```

### Step 4: Update Build Scripts

**Kotlin DSL Updates:**
```kotlin
// ❌ Gradle 6/7
val myTask = tasks.create("myTask") {
    doLast {
        println("Task executed")
    }
}

// ✅ Gradle 8+
val myTask = tasks.register("myTask") {
    doLast {
        println("Task executed")
    }
}
```

**Java Toolchain:**
```kotlin
// ❌ Old: sourceCompatibility
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// ✅ Modern: toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

### Step 5: Plugin Updates

**Check Plugin Compatibility:**
```bash
# Verify plugins are compatible with Gradle 8
./gradlew buildEnvironment
```

**Update Plugin Versions:**
```kotlin
plugins {
    id("org.springframework.boot") version "3.1.5"  // Gradle 8+ compatible
    id("com.github.johnrengelman.shadow") version "8.1.1"  // Gradle 8+ compatible
    kotlin("jvm") version "1.9.20"  // Gradle 8+ compatible
}
```

## Breaking Changes by Version

### Gradle 8.0 Breaking Changes

1. **Removed APIs:**
   - `Project.getConvention()`
   - `Project.convention.getPlugin()`
   - `compile`/`runtime` configurations
   - `jar.baseName` → `jar.archiveBaseName`

2. **Behavior Changes:**
   - Configuration cache enabled by default
   - Stricter task validation
   - Updated default Java target versions

3. **Plugin Changes:**
   - Kotlin DSL 1.9+ required
   - Updated Android Gradle Plugin requirement
   - Maven plugin removed (use maven-publish)

### Gradle 7.0 Breaking Changes

1. **Java Version:** Requires Java 8+
2. **Removed:** Many deprecated APIs from Gradle 5/6
3. **Configuration Cache:** Introduced (experimental)

## Migration Checklist

### Pre-Migration
- [ ] Document current Gradle version
- [ ] Run full build successfully
- [ ] Commit all changes to version control
- [ ] Review Gradle release notes

### During Migration
- [ ] Update wrapper to target version
- [ ] Run build with `--warning-mode=all`
- [ ] Fix deprecation warnings
- [ ] Update plugin versions
- [ ] Update dependency configurations
- [ ] Migrate conventions to extensions
- [ ] Test with configuration cache
- [ ] Update CI/CD scripts

### Post-Migration
- [ ] Full build succeeds
- [ ] All tests pass
- [ ] No deprecation warnings
- [ ] Configuration cache works
- [ ] Build cache functional
- [ ] Documentation updated

## Common Migration Issues

### Issue 1: Configuration Cache Incompatibility

**Problem:** Plugin not compatible with configuration cache

**Solution:**
```bash
# Disable temporarily
gradle build -Dorg.gradle.configuration-cache=false

# Or in gradle.properties
org.gradle.configuration-cache=false

# Fix plugin compatibility issues or wait for updates
```

### Issue 2: Task Convention Access

**Problem:** `Cannot access convention of task`

**Solution:**
```kotlin
// ❌ Old
val jarTask = tasks.getByName("jar") as Jar
jarTask.baseName = "myapp"

// ✅ New
tasks.named<Jar>("jar") {
    archiveBaseName.set("myapp")
}
```

### Issue 3: BuildConfig Generation

**Problem:** Android BuildConfig removed

**Solution:**
```kotlin
// Add BuildConfig plugin
plugins {
    id("com.github.gmazzo.buildconfig") version "4.1.2"
}

buildConfig {
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}
```

## Migration Scripts

**scripts/migration-checker.jbang:**
```bash
#!/usr/bin/env jbang
//DEPS org.gradle:gradle-tooling-api:8.5
//JAVA 17+

// Analyze project for Gradle 8 compatibility
// Checks: deprecated APIs, plugin versions, configuration
```

## Version-Specific Guidance

### Migrating to Gradle 8.5

**Recommended Steps:**
1. Update from Gradle 7.6 (latest 7.x)
2. Fix all deprecation warnings first
3. Update plugins to latest versions
4. Test configuration cache compatibility
5. Update to 8.5

**Key Features in 8.5:**
- Improved configuration cache
- Better Kotlin DSL performance
- Enhanced dependency verification
- Java 21 support

### Migrating to Gradle 9.0 (Future)

**Preparation:**
- Fix all Gradle 8.x deprecations
- Adopt new APIs early
- Monitor Gradle 9 release notes
- Test with Gradle 9 RCs

## Best Practices

1. **Incremental Migration:** Don't skip versions (6→7→8, not 6→8)
2. **Fix Warnings:** Address deprecations before upgrading
3. **Test Thoroughly:** Run full test suite after migration
4. **Update Plugins:** Ensure plugins are compatible
5. **Use Latest Patch:** Migrate to latest patch version (8.5, not 8.0)
6. **Configuration Cache:** Test compatibility early
7. **Document Changes:** Keep migration notes for team
8. **CI/CD Updates:** Update build server Gradle versions

## Resources

- [Gradle 8 Upgrade Guide](https://docs.gradle.org/current/userguide/upgrading_version_8.html)
- [Gradle 7 Upgrade Guide](https://docs.gradle.org/current/userguide/upgrading_version_7.html)
- [Gradle Release Notes](https://docs.gradle.org/current/release-notes.html)
- [Kotlin DSL Migration](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
