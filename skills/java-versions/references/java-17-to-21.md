# Java 17 → 21 Upgrade Reference

22 JEPs focusing on concurrency, pattern matching enhancements, and API improvements.

## Table of Contents
- [Virtual Threads (JEP 444)](#virtual-threads-jep-444)
- [Pattern Matching for switch (JEP 441)](#pattern-matching-for-switch-jep-441)
- [Record Patterns (JEP 440)](#record-patterns-jep-440)
- [Sequenced Collections (JEP 431)](#sequenced-collections-jep-431)
- [String Templates (JEP 430, Preview)](#string-templates-jep-430-preview)
- [Scoped Values (JEP 446, Preview)](#scoped-values-jep-446-preview)
- [Unnamed Patterns (JEP 443, Preview)](#unnamed-patterns-jep-443-preview)
- [UTF-8 by Default (JEP 400)](#utf-8-by-default-jep-400)
- [Deprecations](#deprecations)

---

## Virtual Threads (JEP 444)

Lightweight threads for high-throughput concurrent applications.

```java
// Before: platform thread pool (limited scalability)
ExecutorService executor = Executors.newFixedThreadPool(100);
executor.submit(() -> {
    httpClient.send(request); // blocking I/O
});

// After: virtual threads (scales to millions)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        httpClient.send(request); // blocking is fine!
    });
}

// Direct creation
Thread.ofVirtual().start(() -> {
    System.out.println("Running in virtual thread");
});

// Named virtual threads
Thread vThread = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> doWork());
```

**When to use virtual threads:**
- I/O-bound operations (HTTP calls, database queries, file I/O)
- High-concurrency server applications
- Tasks that spend time waiting

**When NOT to use:**
- CPU-intensive computations
- Tasks requiring native code or JNI
- Code holding locks for extended periods

**Configuration:**
```bash
# Debug virtual thread scheduling
-Djdk.virtualThreadScheduler.parallelism=N
-Djdk.virtualThreadScheduler.maxPoolSize=N
```

---

## Pattern Matching for switch (JEP 441)

Enhanced switch with type patterns and guards.

```java
// Type patterns in switch
public String process(Object obj) {
    return switch (obj) {
        case String s -> "String: " + s;
        case Integer i -> "Integer: " + i;
        case null -> "null value";
        default -> "Unknown: " + obj.getClass().getSimpleName();
    };
}

// Guarded patterns with 'when'
public String categorize(Object obj) {
    return switch (obj) {
        case String s when s.isEmpty() -> "Empty string";
        case String s when s.length() > 100 -> "Long string";
        case String s -> "Normal string: " + s;
        case Integer i when i < 0 -> "Negative: " + i;
        case Integer i when i == 0 -> "Zero";
        case Integer i -> "Positive: " + i;
        case null -> "null";
        default -> "Other";
    };
}

// Combining with sealed classes (exhaustive)
public double calculateArea(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle t -> 0.5 * t.base() * t.height();
        // No default needed if Shape is sealed with these permits
    };
}
```

**Pattern dominance rules:**
- More specific patterns must come before general ones
- `String s when s.isEmpty()` must precede `String s`

---

## Record Patterns (JEP 440)

Destructure records directly in patterns.

```java
public record Point(int x, int y) {}
public record ColoredPoint(Point point, Color color) {}
public record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}

// Destructuring in instanceof
if (obj instanceof Point(var x, var y)) {
    System.out.println("Point at " + x + ", " + y);
}

// Destructuring in switch
public String describe(Object obj) {
    return switch (obj) {
        case Point(var x, var y) -> 
            "Point at (" + x + ", " + y + ")";
        case ColoredPoint(Point(var x, var y), var color) -> 
            "Colored point at (" + x + ", " + y + ") in " + color;
        default -> "Unknown";
    };
}

// Nested patterns
public String analyzeRectangle(Rectangle rect) {
    return switch (rect) {
        case Rectangle(
            ColoredPoint(Point(var x1, var y1), var c1),
            ColoredPoint(Point(var x2, var y2), var c2)
        ) when c1.equals(c2) -> "Monochrome rectangle";
        case Rectangle r -> "Multi-colored rectangle";
    };
}
```

---

## Sequenced Collections (JEP 431)

New interfaces for collections with defined encounter order.

```java
// New interfaces in hierarchy
// SequencedCollection extends Collection
// SequencedSet extends Set, SequencedCollection
// SequencedMap extends Map

// New methods on List, Deque, LinkedHashSet, etc.
List<String> list = List.of("first", "middle", "last");
String first = list.getFirst();    // "first"
String last = list.getLast();      // "last"
List<String> reversed = list.reversed();  // ["last", "middle", "first"]

// LinkedHashSet
SequencedSet<String> set = new LinkedHashSet<>();
set.addFirst("alpha");
set.addLast("omega");
String firstElement = set.getFirst();
set.reversed().forEach(System.out::println);

// LinkedHashMap
SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.putFirst("one", 1);
map.putLast("three", 3);
Map.Entry<String, Integer> firstEntry = map.firstEntry();
map.reversed().forEach((k, v) -> System.out.println(k + "=" + v));
```

**New methods:**
| Method | SequencedCollection | SequencedSet | SequencedMap |
|--------|---------------------|--------------|--------------|
| getFirst() | ✓ | ✓ | firstEntry() |
| getLast() | ✓ | ✓ | lastEntry() |
| addFirst(e) | ✓ | ✓ | putFirst(k,v) |
| addLast(e) | ✓ | ✓ | putLast(k,v) |
| removeFirst() | ✓ | ✓ | pollFirstEntry() |
| removeLast() | ✓ | ✓ | pollLastEntry() |
| reversed() | ✓ | ✓ | ✓ |

---

## String Templates (JEP 430, Preview)

Safe string interpolation (requires --enable-preview).

```java
// Basic interpolation
String name = "World";
int count = 42;
String message = STR."Hello, \{name}! You have \{count} messages.";

// Expressions in templates
String result = STR."The sum is \{2 + 2}";
String upper = STR."Name: \{name.toUpperCase()}";

// Multi-line
String json = STR."""
    {
      "name": "\{name}",
      "count": \{count}
    }
    """;

// Custom template processors (for SQL, HTML, etc.)
// PreparedStatement stmt = SQL."SELECT * FROM users WHERE id = \{userId}";
```

---

## Scoped Values (JEP 446, Preview)

Modern alternative to ThreadLocal, optimized for virtual threads.

```java
// Define scoped value
private static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

// Set and use
public void handleRequest(String userId) {
    ScopedValue.where(USER_ID, userId)
        .run(() -> {
            processRequest();  // USER_ID available throughout
        });
}

// Access in nested calls
public void processRequest() {
    String userId = USER_ID.get();
    // Use userId...
}

// With return value
public String handleWithResult(String userId) {
    return ScopedValue.where(USER_ID, userId)
        .call(() -> {
            return processAndReturn();
        });
}
```

**Advantages over ThreadLocal:**
- Immutable within scope
- Automatically inherited by child threads
- Lower overhead with virtual threads
- Clear lifecycle (scope-bound)

---

## Unnamed Patterns (JEP 443, Preview)

Use `_` to ignore unused pattern components.

```java
// Ignore record components
switch (point) {
    case Point(var x, _) -> "X coordinate: " + x;  // ignore y
    case ColoredPoint(Point(_, var y), _) -> "Y: " + y;  // ignore x and color
}

// Exception handling
try {
    riskyOperation();
} catch (IOException | SQLException _) {
    // Don't need exception details
    handleError();
}

// Lambda parameters (already in Java 9+)
map.forEach((_, value) -> System.out.println(value));
```

---

## UTF-8 by Default (JEP 400)

UTF-8 is now the default charset on all platforms.

```java
// Before: explicit charset often needed
Files.readString(path, StandardCharsets.UTF_8);
Files.writeString(path, content, StandardCharsets.UTF_8);

// After: UTF-8 by default
Files.readString(path);
Files.writeString(path, content);

// Check default charset
System.out.println(Charset.defaultCharset());  // UTF-8
```

---

## Deprecations

### Finalization (JEP 421) - Deprecated
```java
// Deprecated
@Override
protected void finalize() throws Throwable {
    cleanup();
}

// Alternative: Cleaner API
private static final Cleaner CLEANER = Cleaner.create();

public MyResource() {
    CLEANER.register(this, new CleanupAction(resource));
}

private record CleanupAction(Resource resource) implements Runnable {
    public void run() { resource.release(); }
}
```

### Dynamic Agent Loading (JEP 451)
Warning issued when loading agents dynamically:
```bash
# Suppress warning if needed
-XX:+EnableDynamicAgentLoading
```

---

## Generational ZGC (JEP 439)

Improved ZGC with generational collection:
```bash
-XX:+UseZGC -XX:+ZGenerational
```

---

## Migration Checklist

- [ ] Update build configuration to Java 21
- [ ] Replace thread pools with virtual thread executors for I/O-bound work
- [ ] Convert instanceof chains to pattern matching switch
- [ ] Use record patterns for data extraction
- [ ] Replace getFirst/getLast workarounds with SequencedCollection methods
- [ ] Remove explicit UTF-8 charset specifications
- [ ] Replace finalize() with Cleaner API
- [ ] Consider Generational ZGC for improved GC performance
