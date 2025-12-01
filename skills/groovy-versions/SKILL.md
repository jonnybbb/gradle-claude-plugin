---
name: groovy-versions
description: Comprehensive Groovy version reference for upgrades and feature adoption. Use when working with Groovy code that needs upgrading between versions (2.x→3.0, 3.0→4.0, 4.0→5.0), when adopting new Groovy language features, when converting legacy patterns to modern Groovy idioms, when configuring Gradle/Maven for specific Groovy versions, or when explaining Groovy feature evolution. Covers Parrot Parser, Lambda expressions, Method references, Switch expressions, Records, Sealed classes, GINQ queries, Extension methods, and all major features from Groovy 3.0 through 5.0.
---

# Groovy Versions Skill

Reference for Groovy language features, version upgrades, and modern idiom adoption.

## Quick Reference

| Version | Key Features | JDK Requirements |
|---------|--------------|------------------|
| 3.0     | Parrot Parser, Lambdas, Method refs, `do-while`, ARM try, `var` keyword, `!in`/`!instanceof`, `===`/`!==` | Build: JDK9+, Runtime: JDK8+ |
| 4.0     | Switch expressions, Records, Sealed classes, GINQ, TOML support, Enhanced ranges, `groovy-typecheckers` | Build: JDK16+, Runtime: JDK8+ |
| 5.0     | 350+ extension methods, JEP-512 scripts, `@OperatorRename`, Pattern matching instanceof, `==>` operator, JLine3 REPL | Build: JDK17+, Runtime: JDK11+ |

## Version-Specific References

Select the appropriate reference based on your starting version:

- **Groovy 2.x → 3.0**: See [references/groovy-2-to-3.md](references/groovy-2-to-3.md) - Parrot Parser, Lambdas, Method references
- **Groovy 3.0 → 4.0**: See [references/groovy-3-to-4.md](references/groovy-3-to-4.md) - Switch expressions, Records, Sealed classes, GINQ
- **Groovy 4.0 → 5.0**: See [references/groovy-4-to-5.md](references/groovy-4-to-5.md) - Extension methods, JEP-512, Pattern matching

## Build Tool Configuration

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("groovy")
}

dependencies {
    // Groovy 5.x (latest)
    implementation("org.apache.groovy:groovy:5.0.0")
    // Or groovy-all for all modules
    implementation("org.apache.groovy:groovy-all:5.0.0")
    
    // Groovy 4.x
    // implementation("org.apache.groovy:groovy:4.0.24")
    
    // Groovy 3.x (legacy groupId)
    // implementation("org.codehaus.groovy:groovy:3.0.22")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // or 17 for Groovy 4, 11+ for Groovy 5
    }
}
```

### Maven

```xml
<dependencies>
    <!-- Groovy 5.x (latest) -->
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.0</version>
    </dependency>
    
    <!-- Note: Groovy 4+ uses org.apache.groovy groupId -->
    <!-- Groovy 3.x uses org.codehaus.groovy groupId -->
</dependencies>
```

## Common Migration Patterns

### Closures → Lambda Expressions (3.0+)

```groovy
// Before (closure)
def nums = [1, 2, 3].collect { it * 2 }

// After (lambda) - Groovy 3.0+
def nums = [1, 2, 3].collect(n -> n * 2)
// Closures still work and are preferred for Groovy-specific features
```

### Method References (3.0+)

```groovy
// Before
def upper = strings.collect { it.toUpperCase() }

// After - Groovy 3.0+
def upper = strings.collect(String::toUpperCase)

// Constructor references
def create = Person::new
def person = create('John', 30)
```

### Switch Statements → Switch Expressions (4.0+)

```groovy
// Before
def result
switch(i) {
    case 0: result = 'zero'; break
    case 1: result = 'one'; break
    default: result = 'other'
}

// After - Groovy 4.0+
def result = switch(i) {
    case 0 -> 'zero'
    case 1 -> 'one'
    default -> 'other'
}
```

### Data Classes → Records (4.0+)

```groovy
// Before
@Immutable
class Person {
    String name
    int age
}

// After - Groovy 4.0+
record Person(String name, int age) {
    Person { // compact constructor for validation
        if (age < 0) throw new IllegalArgumentException()
    }
}
```

### SQL-like Queries with GINQ (4.0+)

```groovy
// Query collections like SQL
def result = GQ {
    from p in persons
    where p.age >= 18
    orderby p.name
    select p.name, p.age
}

// Joins
def joined = GQ {
    from o in orders
    join c in customers on o.customerId == c.id
    select c.name, o.total
}
```

### Modern Extension Methods (5.0+)

```groovy
// Zip collections
def pairs = [1, 2, 3].zip(['a', 'b', 'c'])
// [[1, 'a'], [2, 'b'], [3, 'c']]

// Repeat elements
def batman = ['na'].repeat().take(13).join('-') + '...Batman'

// Interleave
def mixed = [1, 2, 3].interleave(['a', 'b', 'c'])
// [1, 'a', 2, 'b', 3, 'c']

// Lazy iterator methods
def result = (1..100000).iterator()
    .collecting(n -> 1..n)
    .collecting(r -> r.sum())
    .findingAll{ it % 2 == 0 }
    .take(5).toList()
```

## Key Version Differences

### Maven Coordinates Change (4.0)

```groovy
// Groovy 3.x and earlier
org.codehaus.groovy:groovy:3.0.x

// Groovy 4.0+
org.apache.groovy:groovy:4.0.x
```

### Package Changes (4.0)

```groovy
// Groovy 3.x (deprecated)
import groovy.util.XmlSlurper
import groovy.util.XmlParser

// Groovy 4.0+ (required)
import groovy.xml.XmlSlurper
import groovy.xml.XmlParser
```

### Automatic Imports (5.0)

Groovy 5 adds `java.time.*` to automatic imports:

```groovy
// No import needed in Groovy 5
def today = LocalDate.now()
def time = LocalTime.of(14, 30)
```

## Migration Checklist

### 2.x → 3.0
- [ ] Update Groovy dependency to 3.0.x
- [ ] Consider adopting lambda expressions for Java interop
- [ ] Use method references where appropriate
- [ ] Migrate to `var` keyword if desired
- [ ] Update any code relying on old parser quirks

### 3.0 → 4.0
- [ ] Change groupId from `org.codehaus.groovy` to `org.apache.groovy`
- [ ] Update package imports (groovy.util.* → groovy.xml.*, etc.)
- [ ] Consider using switch expressions
- [ ] Evaluate record classes for DTOs
- [ ] Try GINQ for complex collection queries

### 4.0 → 5.0
- [ ] Update minimum JDK to 11+
- [ ] Leverage new extension methods for cleaner code
- [ ] Consider `@OperatorRename` for third-party library integration
- [ ] Use pattern matching instanceof if desired
- [ ] Explore lazy iterator methods for performance
