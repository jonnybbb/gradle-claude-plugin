# Groovy 2.x → 3.0 Upgrade Reference

Groovy 3.0 introduces the Parrot Parser with major syntax enhancements and Java compatibility features.

## Table of Contents
- [Parrot Parser](#parrot-parser)
- [Lambda Expressions](#lambda-expressions)
- [Method References](#method-references)
- [do-while Loop](#do-while-loop)
- [Enhanced for Loop](#enhanced-for-loop)
- [Java-style Array Initialization](#java-style-array-initialization)
- [try-with-resources (ARM)](#try-with-resources-arm)
- [New Operators](#new-operators)
- [var Keyword](#var-keyword)
- [Interface Default Methods](#interface-default-methods)
- [Safe Indexing](#safe-indexing)
- [Split Package Changes](#split-package-changes)

---

## Parrot Parser

Groovy 3.0 includes a new ANTLR4-based parser (Parrot) enabling new syntax features while maintaining backward compatibility.

**Configuration options:**
- `groovy.antlr4=false` - Disable new parser (not recommended)
- `groovy.attach.groovydoc=true` - Attach groovydoc to AST nodes
- `groovy.attach.runtime.groovydoc=true` - Embed groovydoc in bytecode

---

## Lambda Expressions

Java 8+ lambda syntax is now supported alongside Groovy closures.

```groovy
// Java-style lambdas
(1..10).forEach(e -> { println e })

def result = (1..10).stream()
    .filter(e -> e % 2 == 0)
    .map(e -> e * 2)
    .toList()

// Variants
def add = (int x, int y) -> { def z = y; return x + z }
def sub = (int x, int y) -> x - y           // no braces for single expression
def mult = (x, y) -> x * y                  // parameter types optional
def isEven = n -> n % 2 == 0               // no parens for single param
def answer = () -> 42                       // no-arg lambda

// Groovy-specific: default parameter values
def addWithDefault = (int x, int y = 100) -> x + y
assert addWithDefault(1) == 101
```

**When to use lambdas vs closures:**
- Use lambdas for Java interop and @CompileStatic scenarios
- Use closures for Groovy-specific features (delegate, owner, curry, etc.)

---

## Method References

Java 8+ method reference syntax using double colon (`::`) is supported.

```groovy
// Static method reference
assert ['1', '2', '3'] == Stream.of(1, 2, 3)
    .map(String::valueOf)
    .toList()

// Instance method reference
assert ['A', 'B', 'C'] == ['a', 'b', 'c'].stream()
    .map(String::toUpperCase)
    .toList()

// Instance variable method reference
def sizeAlphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'::length
assert sizeAlphabet() == 26

// Constructor reference
def r = Random::new
assert r().nextInt(10) in 0..9

// Array constructor reference
assert [1, 2, 3].stream().toArray(Integer[]::new).class.name == '[Ljava.lang.Integer;'
```

---

## do-while Loop

Classic Java do-while loop is now supported.

```groovy
def count = 5
def fact = 1
do {
    fact *= count--
} while (count > 1)
assert fact == 120
```

---

## Enhanced for Loop

Java-style for loop with comma-separated expressions.

```groovy
def facts = []
def count = 5
for (int fact = 1, i = 1; i <= count; i++, fact *= i) {
    facts << fact
}
assert facts == [1, 2, 6, 24, 120]

// Multi-assignment in for loop
def baNums = []
for (def (String u, int v) = ['bar', 42]; v < 45; u++, v++) {
    baNums << "$u $v"
}
assert baNums == ['bar 42', 'bas 43', 'bat 44']
```

---

## Java-style Array Initialization

Curly brace array initialization is now supported after array type declarations.

```groovy
def primes = new int[] {2, 3, 5, 7, 11}
assert primes.size() == 5 && primes.sum() == 28

def pets = new String[] {'cat', 'dog'}
assert pets.size() == 2 && pets.sum() == 'catdog'

// Traditional Groovy alternative still works
String[] groovyBooks = ['Groovy in Action', 'Making Java Groovy']
```

---

## try-with-resources (ARM)

Automatic Resource Management from Java 7 is now supported.

```groovy
def wrestle(s) {
    try (
        FromResource from = new FromResource(s)
        ToResource to = new ToResource()
    ) {
        to << from
        return to.toString()
    }
}

// Java 9+ enhanced form
def wrestle2(s) {
    FromResource from = new FromResource(s)
    try (from; ToResource to = new ToResource()) {
        to << from
        return to.toString()
    }
}
```

---

## New Operators

### Identity Operators (`===`, `!==`)

```groovy
@EqualsAndHashCode
class Creature { String type }

def cat = new Creature(type: 'cat')
def copyCat = cat
def lion = new Creature(type: 'cat')

assert cat == lion      // logical equality
assert cat === copyCat  // identity (same as is())
assert cat !== lion     // not identical
```

### Elvis Assignment (`?=`)

```groovy
@ToString
class Element {
    String name
    int atomicNumber
}

def he = new Element(name: 'Helium')
he.atomicNumber ?= 2  // assigns only if atomicNumber is null/false
assert he.atomicNumber == 2
```

### Negated Operators (`!in`, `!instanceof`)

```groovy
assert 45 !instanceof Date
assert 4 !in [1, 3, 5, 7]
```

---

## var Keyword

Java 10-style `var` type placeholder (available on JDK 8+).

```groovy
var two = 2
IntFunction<Integer> twice = (final var x) -> x * two  // Java 11 syntax
assert [1, 2, 3].collect{ twice.apply(it) } == [2, 4, 6]
```

---

## Interface Default Methods

Java-style default methods in interfaces.

```groovy
interface Greetable {
    String target()
    
    default String salutation() {
        'Greetings'
    }
    
    default String greet() {
        "${salutation()}, ${target()}"
    }
}

class Greetee implements Greetable {
    String name
    @Override String target() { name }
}

def daniel = new Greetee(name: 'Daniel')
assert daniel.greet() == 'Greetings, Daniel'
```

---

## Safe Indexing

Null-safe index operator `?[]`.

```groovy
String[] array = ['a', 'b']
assert 'b' == array?[1]
array?[1] = 'c'
assert 'c' == array?[1]

array = null
assert null == array?[1]  // returns null, no NPE
array?[1] = 'c'           // quietly ignored

def map = [name: 'Groovy']
assert 'Groovy' == map?['name']
map = null
assert null == map?['name']
```

---

## Split Package Changes

For JPMS compliance, some classes moved to new packages. Groovy 3 provides deprecated copies in old locations.

| Old Package | New Package | Classes |
|-------------|-------------|---------|
| `groovy.util` | `groovy.xml` | `XmlSlurper`, `XmlParser` |
| `groovy.util` | `groovy.ant` | `AntBuilder` |
| `groovy.util` | `groovy.test` | `GroovyTestCase` |

**Migration example:**
```groovy
// Groovy 2.x (deprecated in 3.0)
import groovy.util.XmlSlurper

// Groovy 3.0+ (recommended)
import groovy.xml.XmlSlurper
```

---

## Breaking Changes

- Switch default branch must be last if present
- Iterating over String is now consistent between static and dynamic Groovy
- `ImportCustomizer` applied once per module (not per class)
- Picocli no longer bundled - may need explicit `@Grab`

---

## Migration Checklist

- [ ] Update Groovy dependency to 3.0.x
- [ ] Test with new Parrot parser
- [ ] Consider adopting lambda expressions
- [ ] Use method references where beneficial
- [ ] Migrate deprecated package imports
- [ ] Review switch statement default placement
- [ ] Add explicit picocli dependency if needed
