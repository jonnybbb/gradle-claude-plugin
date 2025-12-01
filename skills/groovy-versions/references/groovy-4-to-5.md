# Groovy 4.0 → 5.0 Upgrade Reference

Groovy 5.0 adds 350+ extension methods, JEP-512 script support, pattern matching, and modernized tooling.

## Table of Contents
- [JDK Requirements](#jdk-requirements)
- [Extension Method Additions](#extension-method-additions)
- [Lazy Iterator Methods](#lazy-iterator-methods)
- [Array Extension Methods](#array-extension-methods)
- [Additional Scripting Variations](#additional-scripting-variations)
- [Pattern Matching for instanceof](#pattern-matching-for-instanceof)
- [Underscore as Placeholder](#underscore-as-placeholder)
- [OperatorRename Transform](#operatorrename-transform)
- [Logical Implication Operator](#logical-implication-operator)
- [Automatic java.time Import](#automatic-javatime-import)
- [Index Variable in Loops](#index-variable-in-loops)
- [Enhanced groovysh REPL](#enhanced-groovysh-repl)
- [Type Checking Enhancements](#type-checking-enhancements)
- [Native Interface Methods](#native-interface-methods)
- [Breaking Changes](#breaking-changes)

---

## JDK Requirements

- **Build**: JDK 17+
- **Runtime**: JDK 11+ (minimum)
- **Tested**: JDK 11 through 25
- Many JDK 17-25 features available on earlier JDKs

---

## Extension Method Additions

350+ new extension methods added for productivity gains.

### Collection Extensions

```groovy
// repeat - repeat elements
def words = ['row'].repeat(3) + ['your', 'boat']
assert words.join(' ') == 'row row row your boat'

// Infinite repeat
def batman = ['na'].repeat().take(13).join('-') + '...Batman'
assert batman == 'na-na-na-na-na-na-na-na-na-na-na-na-na...Batman'

// zip - combine collections pairwise
def pairs = [1, 2, 3].zip(['a', 'b', 'c'])
assert pairs.toString() == '[[1, a], [2, b], [3, c]]'

// zipAll - handle different sizes with defaults
def one = ['🍎'].repeat(4)
def two = ['🍏'].repeat(3)
assert one.zipAll(two, '_', '🍌')*.join() == ['🍎🍏', '🍎🍏', '🍎🍏', '🍎🍌']

// interleave - alternate elements
def gs = ['🅶'].repeat(5)
def hs = ['💚'].repeat(3)
assert gs.interleave(hs).join() == '🅶💚🅶💚🅶💚'
assert hs.interleave(gs, true).join() == '💚🅶💚🅶💚🅶🅶🅶'  // append remainder

// flattenMany - flatMap equivalent
var items = ["1", "2", "foo", "3", "bar"]
var toInt = s -> s.number ? Optional.of(s.toInteger()) : Optional.empty()
assert items.flattenMany(toInt) == [1, 2, 3]

// collectEntries with separate key/value functions
def languages = ['Groovy', 'Java', 'Kotlin']
assert languages.collectEntries(String::toLowerCase, String::size) ==
    [groovy:6, java:4, kotlin:6]

// withCollectedKeys/Values
assert languages.withCollectedKeys(s -> s.take(1)) ==
    [G:'Groovy', J:'Java', K:'Kotlin']
assert languages.withCollectedValues(s -> s.size()) ==
    [Groovy:6, Java:4, Kotlin:6]

// Set operators
var a = [1, 2, 3] as Set
var b = [2, 3, 4] as Set
assert (a | b) == [1, 2, 3, 4] as Set  // union
assert (a & b) == [2, 3] as Set        // intersect
assert (a ^ b) == [1, 4] as Set        // symmetric difference

// injectAll - scan with intermediate results
def letters = 'a'..'d'
assert letters.injectAll('', String::plus) == ['a', 'ab', 'abc', 'abcd']

// partitionPoint - for sorted/partitioned collections
def nums = [0, 1, 1, 3, 4, 4, 4, 5, 7, 9]
def lower = nums.partitionPoint{ it < 4 }   // 4
def upper = nums.partitionPoint{ it <= 4 }  // 7

// subList with range
def fruit = ['🍏', '🍎', '🍉', '🍋', '🍇']
def apples = fruit.subList(0..<2)
apples.clear()
assert fruit == ['🍉', '🍋', '🍇']

// drain - poll all from queue in priority order
def letters = new PriorityQueue(String.CASE_INSENSITIVE_ORDER)
letters.addAll(['Z', 'y', 'X', 'a', 'B', 'c'])
assert letters.drain() == ['a', 'B', 'c', 'X', 'y', 'Z']
```

### Checked Collections

```groovy
// Fail early on type errors
List<String> names = ['john', 'pete'].asChecked(String)
names << 'mary'  // ok
names << 35      // boom! fails immediately
```

### String Extensions

```groovy
// next/previous for strings
assert (1..3).collect('💙'::next) == ['💚', '💛', '💜']

// codePoints
assert '❤️'.codePoints.size() == 2

// StringBuilder length property
assert new StringBuilder('FooBar').tap{ length -= 3 }.toString() == 'Foo'
```

### Stream Extensions

```groovy
// Index operators for streams
def nums = (10..20).stream()
assert nums[5] == 15

def letters = ('a'..'d').stream()
assert letters[1..2] == 'b'..'c'
```

### BitSet Extensions

```groovy
def fortyTwo = BitSet.valueOf(42)
assert BitSet.valueOf(84) == fortyTwo << 1
assert BitSet.valueOf(21) == fortyTwo >> 1
```

---

## Lazy Iterator Methods

New lazy variants of collection methods for memory efficiency.

| Eager (returns List) | Lazy (returns Iterator) |
|---------------------|------------------------|
| `collect` | `collecting` |
| `collectEntries` | `collectingEntries` |
| `collectMany` | `collectingMany` |
| `findAll` | `findingAll` |
| `findResults` | `findingResults` |

```groovy
// Eager - uses hundreds of MB, takes minutes
assert (0..1000000)
    .collect(n -> 1..n)
    .collect(r -> r.sum())
    .collate(2)
    .collect{ a, b -> a * b }
    .findAll{ it % 2 }
    .take(3) == [3, 315, 2475]

// Lazy - uses KB, takes milliseconds
assert (1..100000).iterator()
    .collecting(n -> 1..n)
    .collecting(r -> r.sum())
    .collate(2)
    .collecting{ a, b -> a * b }
    .findingAll{ it % 2 }
    .take(3)
    .toList() == [3, 315, 2475]

// Mix with streams
assert (1..100000).iterator()
    .collecting(n -> 1..n)
    .stream()
    .map(r -> r.sum())
    .iterator()
    .collate(2)
    .collecting{ a, b -> a * b }
    .stream()
    .filter{ it % 2 }
    .limit(3)
    .toList() == [3, 315, 2475]
```

---

## Array Extension Methods

220+ new/enhanced extension methods for arrays with improved performance (up to 10× faster).

```groovy
int[] nums = -3..2
assert nums.any{ it > 1 }
assert nums.every(n -> n < 4)
assert nums.join(' ') == '-3 -2 -1 0 1 2'
assert nums.head() == -3
assert nums.tail() == -2..2
assert nums.max() == 2
assert nums.max{ it.abs() } == -3
assert nums.reverse() == 2..-3
assert nums.chop(3, 3) == [[-3, -2, -1], [0, 1, 2]]

String[] letters = 'a'..'d'
assert letters.last() == 'd'
assert letters.withIndex()*.join() == ['a0', 'b1', 'c2', 'd3']
assert letters.withCollectedValues(String::toUpperCase) ==
    ['a':'A', 'b':'B', 'c':'C', 'd':'D']

// Multidimensional arrays
int[][] matrix = [[1, 2], [10, 20], [100, 200]]
assert matrix.transpose() == [[1, 10, 100], [2, 20, 200]]
assert matrix.flatten() == [1, 2, 10, 20, 100, 200]
```

---

## Additional Scripting Variations

Support for JEP-512 style scripts (compact source notation).

```groovy
// Traditional Groovy script
println 'Hello, World!'

// JEP-512 style instance main (Groovy 5+)
def main() {
    println 'Hello, World!'
}

// JEP-512 style with arguments
def main(args) {
    println "Args: $args"
}

// Static main (promoted to public static void main)
@CompileStatic
static main(args) {
    println 'Groovy world!'
}

// Instance run method (extends Script class)
def run() {
    println 'Hello, World!'
}

// With fields and methods (no @Field needed)
def main() {
    assert upper(foo) + lower(bar) == 'FOObar'
}

def upper(s) { s.toUpperCase() }
def lower = String::toLowerCase
def (foo, bar) = ['Foo', 'Bar']
```

---

## Pattern Matching for instanceof

JEP-394 syntax supported for Java compatibility.

```groovy
// Traditional Groovy (still works via duck typing)
if (obj instanceof String) {
    println obj.toUpperCase()  // no cast needed
}

// JEP-394 syntax (Groovy 5+)
if (obj instanceof String s) {
    println s.toUpperCase()
}
```

---

## Underscore as Placeholder

Use `_` for unused parameters (JEP-302 style).

```groovy
// Unused in multi-assignment
var (_, y, m, _, _, d) = Calendar.instance
println "Today is $y-${m+1}-$d"

// Unused lambda parameters
def c = (_, _, a, b) -> a + b
assert c(1000, 100, 10, 1) == 11

// Unused closure parameters
def f = { a, _, _, b -> a + b }
assert f(1000, 100, 10, 1) == 1001
```

---

## OperatorRename Transform

Map Groovy operators to different method names for third-party libraries.

```groovy
import groovy.transform.OperatorRename

// Apache Commons Numbers Fraction
@OperatorRename(plus='add')
def testAddOfTwoFractions() {
    var half = Fraction.of(1, 2)
    var third = Fraction.of(1, 3)
    assert half + third == Fraction.of(5, 6)  // calls add()
}

// Matrix libraries
@OperatorRename(multiply='mult')  // Ejml
@OperatorRename(plus='add')       // Commons Math
@OperatorRename(minus='sub')      // Nd4j
```

---

## Logical Implication Operator

New `==>` operator for logical implication (equivalent to `!A || B`).

```groovy
assert false ==> false
assert false ==> true
assert true ==> true
assert !(true ==> false)

// Useful in preconditions, postconditions, invariants
nums.indices.every { it in lower..<upper ==> nums[it] == 4 }
```

---

## Automatic java.time Import

`java.time.*` is now automatically imported.

```groovy
// No import needed in Groovy 5
def today = LocalDate.now()
def time = LocalTime.of(14, 30)
def dateTime = LocalDateTime.now()
def duration = Duration.ofHours(2)
```

---

## Index Variable in Loops

Declare index variable directly in for loop.

```groovy
var list = []
var hearts = ['💙', '💚', '💛', '💜']
for (int idx, var heart in hearts) {
    idx.times { list << heart }
}
assert list.join() == '💚💛💛💜💜💜'
```

---

## Enhanced groovysh REPL

Rebuilt on JLine3 with modern features.

**Features:**
- Cross-platform line editing, history, completion
- ANSI colors and syntax highlighting
- Rich command set

**Key Commands:**
- `/grab` - Add Maven dependencies
- `/prnt` - Pretty print objects
- `/inspect` - Browse object info
- `/slurp` - Quick data ingestion
- `/nano` - Edit files in terminal
- `/help` - Full command list

---

## Type Checking Enhancements

### Format String Checker

```groovy
import groovy.transform.TypeChecked

@TypeChecked(extensions = 'groovy.typecheckers.FormatStringChecker')
def format() {
    // Compile-time error if format string is invalid
    String.format('%4.2f %02X %B', Math.PI, 15, true)
}
```

### Enhanced Named Parameter Checking

`@MapConstructor` and `copyWith` now generate `@NamedParam` annotations automatically.

```groovy
@Immutable(copyWith = true)
class DoctorWho {
    String first, last
    Integer number
}

// Compile-time error for unknown parameters
def dr = new DoctorWho(first: 'Tom', last: 'Baker', born: 1934)  // Error!

// Type checking on copyWith
def dr6 = dr4.copyWith(number: 'six')  // Error: expected Integer
```

---

## Native Interface Methods

Default, private, and static methods in interfaces now native (not trait-based).

```groovy
interface Calculator {
    int calculate(int a, int b)
    
    default int add(int a, int b) { a + b }
    
    private int validate(int x) { Math.abs(x) }
    
    static Calculator create() { (a, b) -> a + b }
}
```

---

## Breaking Changes

- **JDK 11+ required** at runtime
- **java.time auto-import**: May conflict with classes named `Year`, `Month`, `Duration` in default package
- **findIndexValues return type**: Changed from `List` to `Iterator`
- **chop behavior**: Stops when collection exhausted (use boolean for old behavior)
- **Anonymous inner class visibility**: Now package-private (was public)
- **Duplicate imports**: Now error (was last-wins)
- **Static main scripts**: No longer extend `Script` class
- **remainder operator**: `%` now uses `remainder()` method (was `mod()`)
- **$getLookup method**: Removed (JPMS workaround no longer needed)

---

## Migration Checklist

- [ ] Update JDK to 11+ minimum
- [ ] Check for classes named `Year`, `Month`, `Duration` in default package
- [ ] Review code using `findIndexValues` (now returns Iterator)
- [ ] Check `chop` usage for changed behavior
- [ ] Update duplicate imports to use explicit imports
- [ ] Convert static main scripts if binding access needed
- [ ] Update operator overloading from `mod()` to `remainder()` if applicable
- [ ] Leverage new extension methods for cleaner code
- [ ] Consider `@OperatorRename` for third-party library integration
- [ ] Explore lazy iterator methods for large data processing
