# Groovy 3.0 → 4.0 Upgrade Reference

Groovy 4.0 introduces switch expressions, records, sealed classes, GINQ queries, and removes legacy code.

## Table of Contents
- [Maven Coordinate Change](#maven-coordinate-change)
- [Legacy Package Removal](#legacy-package-removal)
- [Switch Expressions](#switch-expressions)
- [Sealed Types](#sealed-types)
- [Records](#records)
- [GINQ (Groovy-Integrated Query)](#ginq-groovy-integrated-query)
- [Built-in Type Checkers](#built-in-type-checkers)
- [Built-in Macro Methods](#built-in-macro-methods)
- [TOML Support](#toml-support)
- [Enhanced Ranges](#enhanced-ranges)
- [GString Performance](#gstring-performance)
- [POJO Annotation](#pojo-annotation)
- [Groovy Contracts](#groovy-contracts)
- [Legacy Consolidation](#legacy-consolidation)
- [Breaking Changes](#breaking-changes)

---

## Maven Coordinate Change

**Critical**: GroupId changed from `org.codehaus.groovy` to `org.apache.groovy`.

```xml
<!-- Groovy 3.x -->
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>3.0.x</version>
</dependency>

<!-- Groovy 4.0+ -->
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>4.0.x</version>
</dependency>
```

```kotlin
// Gradle Kotlin DSL
// Groovy 3.x
implementation("org.codehaus.groovy:groovy:3.0.x")

// Groovy 4.0+
implementation("org.apache.groovy:groovy:4.0.x")
```

---

## Legacy Package Removal

Classes deprecated in Groovy 3 (duplicate packages for JPMS) are now removed. **Update imports:**

```groovy
// Old (removed)           // New (required)
groovy.util.XmlSlurper     groovy.xml.XmlSlurper
groovy.util.XmlParser      groovy.xml.XmlParser
groovy.util.AntBuilder     groovy.ant.AntBuilder
groovy.util.GroovyTestCase groovy.test.GroovyTestCase
```

---

## Switch Expressions

Switch can now be used as an expression with arrow syntax.

```groovy
// Traditional switch statement
def result
switch(i) {
    case 0: result = 'zero'; break
    case 1: result = 'one'; break
    default: throw new IllegalStateException()
}

// Switch expression - Groovy 4.0+
def result = switch(i) {
    case 0 -> 'zero'
    case 1 -> 'one'
    default -> throw new IllegalStateException()
}

// Block with yield for complex logic
def result = switch(i) {
    case 0 -> { def a = 'ze'; def b = 'ro'; a + b }
    case 1 -> 'one'
    default -> 'other'
}

// Traditional colon form with yield
def result = switch(i) {
    case 0:
        def a = 'ze'
        def b = 'ro'
        yield a + b
    case 1:
        yield 'one'
    default:
        throw new IllegalStateException()
}
```

**Groovy-specific case expressions work in switch expressions:**

```groovy
def items = [10, -1, 5, null, 41, 3.5f, 'foo']
def result = items.collect { a ->
    switch(a) {
        case null -> 'null'
        case 5 -> 'five'
        case 0..15 -> 'range'
        case [37, 41, 43] -> 'prime'
        case Float -> 'float'
        case { it instanceof Number && it % 2 == 0 } -> 'even'
        case ~/../ -> 'two chars'
        default -> 'none'
    }
}
```

---

## Sealed Types

Restrict which classes can extend/implement a type.

```groovy
// Using sealed keyword
sealed abstract class Weather { }
class Rainy extends Weather { Integer rainfall }
class Sunny extends Weather { Integer temp }
class Cloudy extends Weather { Integer uvIndex }

// Using @Sealed annotation
import groovy.transform.*

@Sealed interface Tree<T> {}
@Singleton final class Empty implements Tree {
    String toString() { 'Empty' }
}
@Canonical final class Node<T> implements Tree<T> {
    T value
    Tree<T> left, right
}

Tree<Integer> tree = new Node<>(42, new Node<>(0, Empty.instance, Empty.instance), Empty.instance)
```

**Modifiers for permitted classes:**
- `final` - no further subclasses
- `sealed` - further restricted subclasses
- `non-sealed` - open for extension

---

## Records

Immutable data classes with auto-generated methods (JDK16+ native, earlier JDKs emulated).

```groovy
// Simple record definition
record Cyclist(String firstName, String lastName) { }

// Equivalent longer form
@groovy.transform.RecordType
class Cyclist {
    String firstName
    String lastName
}

// Usage
def richie = new Cyclist('Richie', 'Porte')

// Record characteristics:
// - Implicitly final
// - Private final fields with accessor methods (firstName(), lastName())
// - Default constructor
// - Auto-generated toString(), equals(), hashCode()

// With validation (compact constructor)
record Person(String name, int age) {
    Person {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative")
    }
}
```

**Records vs @Immutable:**
- Records are closer to Java records
- `@Immutable` provides more Groovy-specific features
- Use `@RecordOptions` for native vs emulated control

---

## GINQ (Groovy-Integrated Query)

SQL-like syntax for querying collections.

```groovy
// Basic query
def adults = GQ {
    from p in persons
    where p.age >= 18
    select p.name
}

// With ordering and limit
def top3 = GQ {
    from p in persons
    orderby p.age in desc
    limit 3
    select p.name, p.age
}

// Join
def result = GQ {
    from p in prices
    join c in vitaminC on c.name == p.name
    orderby c.conc / p.price in desc
    limit 2
    select p.name
}

// Group by with having
def byGender = GQ {
    from p in persons
    groupby p.gender
    having p.gender == 'Male'
    select p.gender, max(p.age)
}

// JSON processing example
def json = new JsonSlurper().parseText(jsonString)
assert GQ {
    from p in json.prices
    join c in json.vitC on c.name == p.name
    orderby c.conc / p.price in desc
    limit 2
    select p.name
}.toList() == ['Kakuda plum', 'Kiwifruit']
```

---

## Built-in Type Checkers

Optional module `groovy-typecheckers` provides compile-time checking.

```groovy
import groovy.transform.TypeChecked

// Regex checker - catches bad patterns at compile time
@TypeChecked(extensions = 'groovy.typecheckers.RegexChecker')
def whenIs2020Over() {
    def newYearsEve = '2020-12-31'
    def matcher = newYearsEve =~ /(\d{4})-(\d{1,2})-(\d{1,2}/  // Compile error!
}
// Error: Bad regex: Unclosed group near index 26
```

---

## Built-in Macro Methods

Optional module `groovy-macro-library` provides debugging utilities.

```groovy
def num = 42
def list = [1, 2, 3]
def range = 0..5
def string = 'foo'

// SV - String with Variables (name=value pairs)
println SV(num, list, range, string)
// Output: num=42, list=[1, 2, 3], range=[0, 1, 2, 3, 4, 5], string=foo

// SVI - with inspect() instead of toString()
println SVI(range)
// Output: range=0..5

// SVD - with dump()
println SVD(range)
// Output: range=<groovy.lang.IntRange@14 from=0 to=5 ...>

// NV - NamedValue object
def r = NV(range)
assert r.name == 'range' && r.val == 0..5

// NVL - List of NamedValue
def nsl = NVL(num, string)
assert nsl*.name == ['num', 'string']
```

---

## TOML Support

New `groovy-toml` module for TOML file processing.

```groovy
// Building TOML
def builder = new TomlBuilder()
builder.records {
    car {
        name 'HSV Maloo'
        make 'Holden'
        year 2006
        country 'Australia'
    }
}

// Parsing TOML
def ts = new TomlSlurper()
def toml = ts.parseText(builder.toString())
assert 'HSV Maloo' == toml.records.car.name
assert 2006 == toml.records.car.year
```

---

## Enhanced Ranges

Ranges can now be open on both sides.

```groovy
// Inclusive (existing)
assert (3..5).toList() == [3, 4, 5]

// Exclusive on right (existing)
assert (4..<7).toList() == [4, 5, 6]

// Exclusive on left (new)
assert (3<..6).toList() == [4, 5, 6]

// Exclusive on both (new)
assert (0<..<4).toList() == [1, 2, 3]
```

---

## GString Performance

GString `toString()` values are now automatically cached when safe.

```groovy
def now = java.time.LocalDateTime.now()
def gs = "integer: ${1}, double: ${1.2d}, string: ${'x'}, date: ${now}"

// Much faster in Groovy 4 (0.1s vs 10s for 10M iterations in Groovy 3)
for (int i = 0; i < 10000000; i++) {
    gs.toString()
}
```

---

## POJO Annotation

Generate plain Java objects without Groovy runtime dependencies.

```groovy
@CompileStatic
@POJO
@Canonical(includeNames = true)
class Point {
    Integer x, y
}

// Generated class doesn't have getMetaClass(), invokeMethod(), etc.
// Can be used with Java frameworks without Groovy jars
```

---

## Groovy Contracts

Design-by-contract support with `groovy-contracts` module.

```groovy
import groovy.contracts.*

@Invariant({ speed() >= 0 })
class Rocket {
    int speed = 0
    boolean started = true

    @Requires({ isStarted() })
    @Ensures({ old.speed < speed })
    def accelerate(inc) { speed += inc }

    def isStarted() { started }
    def speed() { speed }
}
```

---

## Legacy Consolidation

### Old Parser Removed
The old Antlr2-based parser is removed. Use Groovy 3.x if needed.

### Classic Bytecode Generation Removed
Only invoke dynamic ("indy") bytecode is now generated. One set of jars (previously there were "normal" and "-indy" variants).

---

## Breaking Changes

- **Maven coordinates**: `org.codehaus.groovy` → `org.apache.groovy`
- **Package removals**: Use new package locations (see above)
- **groovy-all changes**: `groovy-yaml` included, `groovy-testng` removed
- **intersect() semantics**: Now aligns with other languages
- **JavaBean property naming**: Edge cases now follow spec
- **ASTTest retention**: Changed from `RUNTIME` to `SOURCE`
- **Boolean property access**: More closely follows JavaBean spec
- **Floating point zero**: -0.0 now correctly different from 0.0

---

## Migration Checklist

- [ ] Update groupId to `org.apache.groovy`
- [ ] Update all deprecated package imports
- [ ] Review groovy-all module changes
- [ ] Test switch statements (default must be last)
- [ ] Consider switch expressions for cleaner code
- [ ] Evaluate records for DTOs and value objects
- [ ] Try GINQ for complex collection queries
- [ ] Update any code relying on removed legacy features
