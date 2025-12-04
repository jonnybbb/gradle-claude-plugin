---
name: java-versions
description: This skill should be used when the user asks to "upgrade Java version", "migrate Java 8 to 11", "migrate Java 11 to 17", "migrate Java 17 to 21", "migrate Java 21 to 25", "use Java records", "use virtual threads", "use pattern matching", "configure Java toolchain", or mentions switch expressions, sealed classes, text blocks, Stream Gatherers, Scoped Values, flexible constructor bodies, or specific JEP numbers.
---

# Java Versions Skill

Reference for Java language features, version upgrades, and modern idiom adoption.

## Quick Reference

| Version | Key Features |
|---------|--------------|
| 11 LTS  | HTTP Client API, String methods (isBlank, strip, lines), Files.readString/writeString, var in lambdas |
| 17 LTS  | Records, Sealed Classes, Pattern Matching for instanceof, Switch Expressions, Text Blocks |
| 21 LTS  | Virtual Threads, Pattern Matching for switch, Record Patterns, Sequenced Collections |
| 25 LTS  | Flexible Constructor Bodies, Stream Gatherers, Scoped Values, Module Import Declarations |

## Version-Specific References

Select the appropriate reference based on the starting version:

- **Java 11 Baseline**: See [references/java-11-baseline.md](references/java-11-baseline.md) - HTTP Client, String/Files methods, var in lambdas
- **Java 11 → 17**: See [references/java-11-to-17.md](references/java-11-to-17.md) - 47 JEPs including Records, Sealed Classes, Pattern Matching
- **Java 17 → 21**: See [references/java-17-to-21.md](references/java-17-to-21.md) - 22 JEPs including Virtual Threads, Record Patterns  
- **Java 21 → 25**: See [references/java-21-to-25.md](references/java-21-to-25.md) - 18 JEPs including Flexible Constructor Bodies, Stream Gatherers

## Build Tool Configuration

### Gradle (Kotlin DSL)

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25) // or 21, 17
    }
}

tasks.withType<JavaCompile> {
    options.release.set(25)
    // For preview features:
    // options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    // For preview features:
    // jvmArgs("--enable-preview")
}
```

### Maven

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>25</release>
                <!-- For preview features: -->
                <!-- <compilerArgs><arg>--enable-preview</arg></compilerArgs> -->
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Common Migration Patterns

### Data Classes → Records

```java
// Before (any version)
public class Person {
    private final String name;
    private final int age;
    public Person(String name, int age) { this.name = name; this.age = age; }
    public String name() { return name; }
    public int age() { return age; }
    // equals, hashCode, toString...
}

// After (Java 16+)
public record Person(String name, int age) {
    public Person { // compact constructor for validation
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
    }
}
```

### instanceof Chains → Pattern Matching Switch

```java
// Before
if (obj instanceof String s) { return s.toUpperCase(); }
else if (obj instanceof Integer i) { return i * 2; }
else { return obj; }

// After (Java 21+)
return switch (obj) {
    case String s -> s.toUpperCase();
    case Integer i -> i * 2;
    default -> obj;
};
```

### Platform Threads → Virtual Threads

```java
// Before
ExecutorService executor = Executors.newFixedThreadPool(100);

// After (Java 21+)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

### ThreadLocal → Scoped Values

```java
// Before
private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
CURRENT_USER.set(user);
// ...
User user = CURRENT_USER.get();

// After (Java 25)
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
ScopedValue.where(CURRENT_USER, user).run(() -> {
    User u = CURRENT_USER.get();
});
```

### HTTP Client (Java 11+)

```java
// Modern HTTP requests
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/data"))
    .header("Accept", "application/json")
    .GET()
    .build();

// Sync
HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());

// Async
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println);
```

### Stream Gatherers (Java 24+)

```java
// Fixed-size batches
List<List<Integer>> batches = Stream.of(1, 2, 3, 4, 5, 6)
    .gather(Gatherers.windowFixed(2))
    .toList();
// [[1, 2], [3, 4], [5, 6]]

// Running totals
List<Integer> running = Stream.of(1, 2, 3, 4)
    .gather(Gatherers.scan(() -> 0, Integer::sum))
    .toList();
// [1, 3, 6, 10]
```

### Flexible Constructor Bodies (Java 25)

```java
// Validate before super()
public class Customer extends Person {
    public Customer(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email");
        }
        super(email);  // validation happens first
    }
}
```

## Preview Features

Enable preview features only when needed:

```bash
# Compile
javac --enable-preview --release 25 MyClass.java

# Run  
java --enable-preview MyClass
```

Current preview features in Java 25:
- Primitive Types in Patterns (JEP 507) - 3rd preview
- Structured Concurrency (JEP 505) - 5th preview
- Stable Values (JEP 502)
- PEM Encodings (JEP 470)
