# Dependency Constraints

**Source**: https://docs.gradle.org/current/userguide/dependency_constraints.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Dependency constraints influence version selection without adding direct dependencies.

## Basic Constraints

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre")
    }
}
```

Constraint applies only if guava is already a transitive dependency.

## Constraint Types

### Version Constraint

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                strictly("[28.0-jre, 32.0-jre[")
                prefer("31.1-jre")
                reject("30.0-jre")
            }
        }
    }
}
```

### Reasoning

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre") {
            because("Security fix for CVE-2023-XXXXX")
        }
    }
}
```

### Platform Constraints

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.slf4j:slf4j-api:2.0.9")
        api("org.slf4j:slf4j-simple:2.0.9")
        runtime("com.h2database:h2:2.2.224")
    }
}
```

## Using Platforms

### Import Platform

```kotlin
dependencies {
    // Import BOM/platform
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Dependencies without versions
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
}
```

### Enforce Platform

```kotlin
dependencies {
    // Enforce platform versions (override transitives)
    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    implementation("org.springframework:spring-core")
}
```

## Rich Version Constraints

### Strictly

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                strictly("[28.0-jre, 32.0-jre[")
            }
        }
    }
}
```

Build fails if version outside range is requested.

### Prefer

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                prefer("31.1-jre")
            }
        }
    }
}
```

Soft preference, can be overridden.

### Require

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                require("31.1-jre")
            }
        }
    }
}
```

Minimum version requirement.

### Reject

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                reject("30.0-jre", "30.1-jre")
            }
        }
    }
}
```

Explicitly reject specific versions.

## Combining Constraints

```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                strictly("[28.0-jre, 32.0-jre[")
                prefer("31.1-jre")
                reject("30.0-jre")
            }
            because("Security and compatibility requirements")
        }
    }
}
```

## Platform Projects

### Creating Platform

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // Define constraints
    constraints {
        api("com.google.guava:guava:31.1-jre")
        api("org.slf4j:slf4j-api:2.0.9")
    }
    
    // Import other platforms
    api(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
}
```

### Publishing Platform

```kotlin
plugins {
    `java-platform`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
        }
    }
}
```

## Dependency Alignment

### Version Alignment

```kotlin
dependencies {
    constraints {
        // Align all Jackson versions
        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
        implementation("com.fasterxml.jackson.core:jackson-core:2.15.0")
        implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.0")
    }
}
```

### Using Component Metadata

```kotlin
dependencies {
    components {
        all {
            if (id.group == "com.fasterxml.jackson.core") {
                allVariants {
                    attributes {
                        attribute(
                            Attribute.of("org.gradle.status", String::class.java),
                            "release"
                        )
                    }
                }
            }
        }
    }
}
```

## Use Cases

### Security Updates

```kotlin
dependencies {
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind") {
            version {
                strictly("[2.15.0,)")
            }
            because("Security fix for CVE-2023-XXXXX")
        }
    }
}
```

### Feature Requirements

```kotlin
dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib") {
            version {
                require("1.9.0")
            }
            because("Need sealed interface support")
        }
    }
}
```

### Breaking Changes

```kotlin
dependencies {
    constraints {
        implementation("com.example:library") {
            version {
                strictly("[1.0, 2.0[")
            }
            because("Version 2.0 has breaking API changes")
        }
    }
}
```

## Best Practices

1. **Use platforms** for related dependencies
2. **Document reasons** with `because`
3. **Prefer over force** for flexibility
4. **Test constraints** with dependency insight
5. **Version carefully** - too strict breaks builds

## Troubleshooting

### Constraint Not Applied

```bash
# Check if dependency present
./gradlew dependencies | grep library

# Constraints only apply to present dependencies
```

### Conflict with Constraint

```bash
# See resolution reason
./gradlew dependencyInsight --dependency library

# Shows constraint vs actual version
```

## Related Documentation

- [Dependency Management](dependency-management.md): Basics
- [Conflict Resolution](conflict-resolution.md): Resolving conflicts
- [Version Catalogs](version-catalogs.md): Centralized versions

## Quick Reference

```kotlin
dependencies {
    // Platform
    implementation(platform("group:platform:version"))
    
    // Constraints
    constraints {
        implementation("group:artifact:version") {
            version {
                strictly("range")
                prefer("version")
                reject("version")
            }
            because("reason")
        }
    }
}

// Platform project
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("group:artifact:version")
    }
}
```
