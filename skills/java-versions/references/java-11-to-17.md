# Java 11 → 17 Upgrade Reference

47 JEPs covering major language enhancements and API improvements.

## Table of Contents
- [Records (JEP 395)](#records-jep-395)
- [Sealed Classes (JEP 409)](#sealed-classes-jep-409)
- [Pattern Matching for instanceof (JEP 394)](#pattern-matching-for-instanceof-jep-394)
- [Switch Expressions (JEP 361)](#switch-expressions-jep-361)
- [Text Blocks (JEP 378)](#text-blocks-jep-378)
- [Helpful NullPointerExceptions (JEP 358)](#helpful-nullpointerexceptions-jep-358)
- [Enhanced Random Generators (JEP 356)](#enhanced-random-generators-jep-356)
- [Garbage Collectors](#garbage-collectors)
- [Deprecations and Removals](#deprecations-and-removals)

---

## Records (JEP 395)

Immutable data classes with auto-generated methods.

```java
// Before: verbose data class
public class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() { return name; }
    public int age() { return age; }

    @Override
    public boolean equals(Object obj) { /* boilerplate */ }
    @Override
    public int hashCode() { /* boilerplate */ }
    @Override
    public String toString() { /* boilerplate */ }
}

// After: record
public record Person(String name, int age) {
    // Compact constructor for validation
    public Person {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
    }

    // Custom methods allowed
    public boolean isAdult() {
        return age >= 18;
    }
}
```

**When to use records:**
- DTOs and value objects
- API response/request objects
- Configuration objects
- Compound map keys

**Limitations:**
- Cannot extend other classes (implicitly extend Record)
- Fields are final (immutable)
- Cannot declare instance fields

---

## Sealed Classes (JEP 409)

Restrict which classes can extend/implement a type.

```java
public sealed class Shape
    permits Circle, Rectangle, Triangle {
    public abstract double area();
}

public final class Circle extends Shape {
    private final double radius;
    public Circle(double radius) { this.radius = radius; }
    @Override public double area() { return Math.PI * radius * radius; }
}

public final class Rectangle extends Shape {
    private final double width, height;
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    @Override public double area() { return width * height; }
}

// non-sealed allows further inheritance
public non-sealed class Triangle extends Shape {
    private final double base, height;
    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }
    @Override public double area() { return 0.5 * base * height; }
}
```

**Modifiers for permitted classes:**
- `final` - no further subclasses
- `sealed` - further restricted subclasses
- `non-sealed` - open for extension

---

## Pattern Matching for instanceof (JEP 394)

Eliminate redundant casts after instanceof checks.

```java
// Before
public String process(Object obj) {
    if (obj instanceof String) {
        String str = (String) obj;
        return str.toUpperCase();
    } else if (obj instanceof Integer) {
        Integer num = (Integer) obj;
        return "Number: " + num;
    } else if (obj instanceof List<?>) {
        List<?> list = (List<?>) obj;
        return "List with " + list.size() + " elements";
    }
    return "Unknown type";
}

// After
public String process(Object obj) {
    if (obj instanceof String str) {
        return str.toUpperCase();
    } else if (obj instanceof Integer num) {
        return "Number: " + num;
    } else if (obj instanceof List<?> list) {
        return "List with " + list.size() + " elements";
    }
    return "Unknown type";
}
```

**Pattern variable scope:**
- Available in the true branch
- Also available in combined conditions: `if (obj instanceof String s && s.length() > 5)`

---

## Switch Expressions (JEP 361)

Modern switch with arrow syntax and expression form.

```java
// Before: switch statement
public String getDayType(DayOfWeek day) {
    String result;
    switch (day) {
        case MONDAY:
        case TUESDAY:
        case WEDNESDAY:
        case THURSDAY:
        case FRIDAY:
            result = "Workday";
            break;
        case SATURDAY:
        case SUNDAY:
            result = "Weekend";
            break;
        default:
            throw new IllegalArgumentException("Unknown day: " + day);
    }
    return result;
}

// After: switch expression
public String getDayType(DayOfWeek day) {
    return switch (day) {
        case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Workday";
        case SATURDAY, SUNDAY -> "Weekend";
    };
}

// With yield for complex logic
public int calculateScore(Grade grade) {
    return switch (grade) {
        case A -> 100;
        case B -> 85;
        case C -> 70;
        case D -> {
            System.out.println("Consider improvement");
            yield 55;
        }
        case F -> {
            System.out.println("Needs retake");
            yield 0;
        }
    };
}
```

**Key differences:**
- Arrow syntax `->` (no fall-through)
- Multiple case labels: `case A, B, C ->`
- Expression returns value
- `yield` for block expressions

---

## Text Blocks (JEP 378)

Multi-line string literals with preserved formatting.

```java
// Before
String html = "<html>\n" +
              "  <body>\n" +
              "    <h1>Hello World</h1>\n" +
              "  </body>\n" +
              "</html>";

String sql = "SELECT p.id, p.name, p.email, " +
             "       a.street, a.city " +
             "FROM person p " +
             "JOIN address a ON p.address_id = a.id " +
             "WHERE p.active = true";

// After
String html = """
              <html>
                <body>
                  <h1>Hello World</h1>
                </body>
              </html>
              """;

String sql = """
             SELECT p.id, p.name, p.email,
                    a.street, a.city
             FROM person p
             JOIN address a ON p.address_id = a.id
             WHERE p.active = true
             """;

// With formatted() for interpolation
String json = """
              {
                "name": "%s",
                "age": %d
              }
              """.formatted(name, age);
```

**Formatting rules:**
- Opening `"""` must be followed by newline
- Incidental whitespace (leading spaces) is stripped
- Trailing whitespace preserved with `\s`
- Escape newlines with `\` at end of line

---

## Helpful NullPointerExceptions (JEP 358)

Detailed NPE messages showing exactly what was null.

```java
// Old message
// Exception in thread 'main' java.lang.NullPointerException

// New message (enabled by default in Java 17)
// Cannot invoke 'String.length()' because the return value of 'Person.getName()' is null

public void process(Map<String, List<Person>> groups) {
    // NPE will pinpoint exactly which part is null
    int length = groups.get("admins")
                      .get(0)
                      .getName()
                      .length();
}
```

---

## Enhanced Random Generators (JEP 356)

New random number generator interfaces and algorithms.

```java
// Old
Random random = new Random();
int value = random.nextInt(100);

// New: specific algorithms
RandomGenerator generator = RandomGeneratorFactory
    .of("Xoshiro256PlusPlus")
    .create(System.nanoTime());

// Splittable for parallel processing
RandomGenerator.SplittableGenerator splittable =
    RandomGeneratorFactory.<RandomGenerator.SplittableGenerator>of("L64X128MixRandom")
        .create();

splittable.splits(4)
    .parallel()
    .mapToInt(rng -> rng.nextInt(1000))
    .forEach(System.out::println);

// Stream of random values
generator.ints(10, 1, 101)
    .forEach(System.out::println);
```

---

## Garbage Collectors

### ZGC (JEP 377)
Low-latency GC, suitable for large heaps:
```bash
-XX:+UseZGC
```

### Shenandoah (JEP 379)
Consistent pause times:
```bash
-XX:+UseShenandoahGC
```

---

## Deprecations and Removals

### Security Manager (JEP 411) - Deprecated
```java
// Remove Security Manager dependencies
// Use application-level security, containers, or process isolation instead
```

### Applet API (JEP 398) - Deprecated
Migrate to standalone applications or web technologies.

### Nashorn JavaScript Engine (JEP 372) - Removed
Alternatives:
- GraalVM JavaScript engine
- External JavaScript execution via ProcessBuilder
```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
```

---

## Migration Checklist

- [ ] Update build configuration to Java 17
- [ ] Convert data classes to records where appropriate
- [ ] Apply pattern matching to instanceof checks
- [ ] Modernize switch statements to expressions
- [ ] Replace string concatenation with text blocks for multi-line strings
- [ ] Remove Nashorn dependencies
- [ ] Remove Security Manager usage
- [ ] Consider ZGC or Shenandoah for large heap applications
