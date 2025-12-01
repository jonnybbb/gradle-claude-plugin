# Java 21 → 25 Upgrade Reference

18 JEPs covering constructor enhancements, pattern matching expansion, and finalized features.

## Table of Contents
- [Flexible Constructor Bodies (JEP 513)](#flexible-constructor-bodies-jep-513)
- [Primitive Types in Patterns (JEP 507, Preview)](#primitive-types-in-patterns-jep-507-preview)
- [Scoped Values (JEP 506)](#scoped-values-jep-506)
- [Compact Source Files (JEP 512)](#compact-source-files-jep-512)
- [Module Import Declarations (JEP 511)](#module-import-declarations-jep-511)
- [Structured Concurrency (JEP 505, Preview)](#structured-concurrency-jep-505-preview)
- [Compact Object Headers (JEP 519)](#compact-object-headers-jep-519)
- [AOT Method Profiling (JEP 515)](#aot-method-profiling-jep-515)
- [Additional JEPs](#additional-jeps)

---

## Flexible Constructor Bodies (JEP 513)

Execute code before calling super() or this().

```java
// Before: validation after super() or in static method
public class Customer extends Person {
    private final UUID customerId;
    
    public Customer(String email) {
        super(validate(email));  // awkward static helper
        this.customerId = UUID.randomUUID();
    }
    
    private static String validate(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email");
        }
        return email;
    }
}

// After: validation before super()
public class Customer extends Person {
    private final UUID customerId;
    
    public Customer(String email) {
        // Prologue: runs before super()
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.customerId = UUID.randomUUID();  // can initialize fields
        
        super(email);  // now call super
        
        // Epilogue: runs after super()
    }
}
```

**Constructor phases:**
1. **Prologue** - Code before super()/this()
   - Can validate arguments
   - Can initialize fields of current class
   - Cannot access `this` as an object
   - Cannot call instance methods
2. **Constructor invocation** - super() or this()
3. **Epilogue** - Code after (existing behavior)

**What's allowed in prologue:**
```java
public class Example extends Base {
    private final int value;
    
    public Example(int input) {
        // ✓ Local variables
        int computed = input * 2;
        
        // ✓ Argument validation
        if (input < 0) throw new IllegalArgumentException();
        
        // ✓ Field initialization
        this.value = computed;
        
        // ✓ Static method calls
        log("Creating with " + input);
        
        // ✗ Cannot use 'this' as object
        // this.doSomething();  // ERROR
        
        // ✗ Cannot call instance methods
        // toString();  // ERROR
        
        super(computed);
    }
}
```

---

## Primitive Types in Patterns (JEP 507, Preview)

Pattern matching extended to all primitive types.

```java
// Pattern matching with primitives (requires --enable-preview)
public String categorize(Object value) {
    return switch (value) {
        case byte b   -> "Byte: " + b;
        case short s  -> "Short: " + s;
        case int i    -> "Int: " + i;
        case long l   -> "Long: " + l;
        case float f  -> "Float: " + f;
        case double d -> "Double: " + d;
        case boolean b -> b ? "True" : "False";
        case char c   -> "Char: " + c;
        default       -> "Other: " + value;
    };
}

// Safe narrowing conversions
public String narrowPrimitive(int value) {
    return switch (value) {
        case byte b when b >= 0   -> "Positive byte: " + b;
        case short s when s < 0   -> "Negative short: " + s;
        case int i                -> "Larger int: " + i;
    };
}

// With instanceof
if (number instanceof int i && i > 0) {
    System.out.println("Positive integer: " + i);
}

// Exhaustiveness with primitives
public String booleanSwitch(boolean flag) {
    return switch (flag) {
        case true -> "Yes";
        case false -> "No";
    };  // exhaustive, no default needed
}
```

**Dominance rules for primitives:**
- Wider types dominate narrower: `long` after `int`
- `double` dominates all numeric types

---

## Scoped Values (JEP 506)

Now finalized (was preview in Java 21).

```java
// Define at class level
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
private static final ScopedValue<Transaction> CURRENT_TX = ScopedValue.newInstance();

// Bind and run
public void handleRequest(User user) {
    ScopedValue.where(CURRENT_USER, user)
        .run(() -> {
            processRequest();
        });
}

// Multiple bindings
public void handleTransaction(User user, Transaction tx) {
    ScopedValue.where(CURRENT_USER, user)
        .where(CURRENT_TX, tx)
        .run(() -> {
            executeTransaction();
        });
}

// With return value
public Result process(User user) {
    return ScopedValue.where(CURRENT_USER, user)
        .call(() -> {
            return computeResult();
        });
}

// Access anywhere in call chain
public void nestedMethod() {
    User user = CURRENT_USER.get();
    if (CURRENT_TX.isBound()) {
        Transaction tx = CURRENT_TX.get();
    }
}

// Rebinding in nested scope
public void outer() {
    ScopedValue.where(CURRENT_USER, adminUser)
        .run(() -> {
            // Here CURRENT_USER is adminUser
            inner();
        });
}

public void inner() {
    ScopedValue.where(CURRENT_USER, guestUser)
        .run(() -> {
            // Here CURRENT_USER is guestUser (rebound)
            doWork();
        });
}
```

---

## Compact Source Files (JEP 512)

Simplified source files for small programs.

```java
// Before: full ceremony
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}

// After: compact form (no class declaration needed)
void main() {
    println("Hello, World!");
}

// With the new IO class methods
void main() {
    String name = readln("What's your name? ");
    println("Hello, " + name + "!");
}
```

**IO class (java.lang.IO):**
```java
// Simple I/O methods
void main() {
    print("Enter value: ");           // no newline
    println("Hello");                 // with newline
    String line = readln();           // read line
    String prompted = readln("? ");   // read with prompt
}
```

**Benefits:**
- Faster prototyping
- Easier learning curve for beginners
- Code grows naturally as complexity increases

---

## Module Import Declarations (JEP 511)

Import all public types from a module.

```java
// Before: many individual imports
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
// ... many more

// After: module import
import module java.base;

// Now all public types from java.base are available
List<String> list = new ArrayList<>();
Map<String, Integer> map = new HashMap<>();
Stream<String> stream = list.stream();
Path path = Path.of("file.txt");  // java.nio.file.Path
```

**Usage with multiple modules:**
```java
import module java.base;
import module java.sql;
import module java.logging;

void main() {
    Connection conn = DriverManager.getConnection(url);
    Logger logger = Logger.getLogger("app");
}
```

---

## Structured Concurrency (JEP 505, Preview)

Treat concurrent tasks as a single unit of work.

```java
// Structured concurrency with ShutdownOnFailure
public UserData fetchUserData(String userId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<User> userTask = scope.fork(() -> fetchUser(userId));
        Subtask<List<Order>> ordersTask = scope.fork(() -> fetchOrders(userId));
        Subtask<Preferences> prefsTask = scope.fork(() -> fetchPreferences(userId));
        
        scope.join();            // Wait for all
        scope.throwIfFailed();   // Propagate first failure
        
        return new UserData(
            userTask.get(),
            ordersTask.get(),
            prefsTask.get()
        );
    }
}

// ShutdownOnSuccess - return first successful result
public String fetchFromAnyMirror(String resource) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
        for (String mirror : mirrors) {
            scope.fork(() -> fetchFrom(mirror, resource));
        }
        
        scope.join();
        return scope.result();  // First successful result
    }
}
```

**Key concepts:**
- Tasks are children of a scope
- If scope exits, all tasks are cancelled
- Exceptions properly propagated
- Works naturally with virtual threads

---

## Compact Object Headers (JEP 519)

Reduces object header from 12 to 8 bytes.

```bash
# Enable compact headers (experimental in 25)
-XX:+UseCompactObjectHeaders
```

**Benefits:**
- Reduced memory footprint
- Better cache utilization
- Especially significant for many small objects

---

## AOT Method Profiling (JEP 515)

Warm up faster using profile data from previous runs.

```bash
# Record profiling data
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf MyApp

# Use recorded profiles for faster startup
java -XX:AOTMode=replay -XX:AOTConfiguration=app.aotconf MyApp
```

**Benefits:**
- Reduced warm-up time
- Faster peak performance
- Useful for short-lived applications (serverless, CLI)

---

## Stream Gatherers (JEP 473/485)

Custom intermediate stream operations - finalized in Java 24/25.

```java
// Built-in gatherers
import java.util.stream.Gatherers;

// windowFixed - split into fixed-size batches
List<List<Integer>> batches = Stream.of(1, 2, 3, 4, 5, 6, 7)
    .gather(Gatherers.windowFixed(3))
    .toList();
// [[1, 2, 3], [4, 5, 6], [7]]

// windowSliding - overlapping windows
List<List<Integer>> sliding = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowSliding(3))
    .toList();
// [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

// fold - reduce to single value (accessible mid-stream)
int sum = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.fold(() -> 0, Integer::sum))
    .findFirst()
    .orElse(0);
// 15

// scan - running accumulation (emit every step)
List<Integer> runningSum = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.scan(() -> 0, Integer::sum))
    .toList();
// [1, 3, 6, 10, 15]

// mapConcurrent - parallel mapping with limit
List<String> results = urls.stream()
    .gather(Gatherers.mapConcurrent(4, this::fetchUrl))
    .toList();
```

**Custom Gatherer Example:**
```java
// Distinct by custom key
Gatherer<String, Set<Integer>, String> distinctByLength = 
    Gatherer.of(
        HashSet::new,  // initializer
        (state, element, downstream) -> {
            if (state.add(element.length())) {
                downstream.push(element);
            }
            return true;
        }
    );

List<String> uniqueLengths = Stream.of("a", "bb", "c", "ddd", "ee")
    .gather(distinctByLength)
    .toList();
// ["a", "bb", "ddd"] - one string per unique length
```

**Gatherer vs Collector:**
- `Collector` = terminal operation (end of pipeline)
- `Gatherer` = intermediate operation (middle of pipeline)

---

## Additional JEPs

### Key Derivation Function API (JEP 510)
```java
KDF hkdf = KDF.getInstance("HKDF-SHA256");
SecretKey derived = hkdf.deriveKey("AES", 
    new HKDFParameterSpec.Extract(salt, inputKey));
```

### PEM Encoding (JEP 470, Preview)
```java
// Encode keys/certificates to PEM format
String pem = PEMEncoder.encode(certificate);
Certificate cert = PEMDecoder.decode(pemString);
```

### JFR Method Timing (JEP 520)
```java
// Enable method timing events
-XX:StartFlightRecording=method-timing=true
```

### Generational Shenandoah (JEP 521)
```bash
-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational
```

---

## Migration Checklist

- [ ] Update build configuration to Java 25
- [ ] Refactor constructors with complex validation to use flexible constructor bodies
- [ ] Replace ThreadLocal with ScopedValue where appropriate
- [ ] Consider module imports for cleaner import sections
- [ ] Explore Structured Concurrency for parallel operations
- [ ] Enable Compact Object Headers for memory-sensitive applications
- [ ] Use AOT profiling for applications needing fast startup
- [ ] Update JavaDoc to Markdown format (JEP 467)
- [ ] Review sun.misc.Unsafe usage - deprecated methods should be replaced

## Preview Features (require --enable-preview)

- Primitive Types in Patterns (JEP 507)
- Structured Concurrency (JEP 505)
- PEM Encodings (JEP 470)
- Stable Values (JEP 502)
