# Java 11 Baseline Features

Java 11 is commonly the starting point for upgrades. Key features introduced in Java 11 (from Java 8/9/10).

## Table of Contents
- [HTTP Client API (JEP 321)](#http-client-api-jep-321)
- [String Methods](#string-methods)
- [Files Methods](#files-methods)
- [Local var in Lambdas (JEP 323)](#local-var-in-lambdas-jep-323)
- [Single-File Source Programs (JEP 330)](#single-file-source-programs-jep-330)
- [Collection and Optional Enhancements](#collection-and-optional-enhancements)
- [Other Improvements](#other-improvements)

---

## HTTP Client API (JEP 321)

Modern HTTP client supporting HTTP/1.1, HTTP/2, and WebSocket.

```java
// Synchronous GET request
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/data"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());

// Asynchronous request
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println);

// POST request with body
HttpRequest postRequest = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("""
        {"name": "John", "email": "john@example.com"}
        """))
    .build();

// Configure client with timeout and HTTP/2
HttpClient configuredClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();
```

**Advantages over HttpURLConnection:**
- Native HTTP/2 support
- Asynchronous API with CompletableFuture
- WebSocket support
- Cleaner, more intuitive API

---

## String Methods

New utility methods for common string operations.

```java
// isBlank() - checks for empty or whitespace only
"".isBlank();           // true
"   ".isBlank();        // true
"hello".isBlank();      // false

// strip() - Unicode-aware trim (better than trim())
"  hello  ".strip();         // "hello"
"  hello  ".stripLeading();  // "hello  "
"  hello  ".stripTrailing(); // "  hello"

// lines() - stream of lines
String multiline = "line1\nline2\nline3";
multiline.lines()
    .filter(line -> !line.isBlank())
    .forEach(System.out::println);

// repeat() - repeat string n times
"ab".repeat(3);  // "ababab"
"-".repeat(50);  // "---...---" (50 dashes)
```

**strip() vs trim():**
- `strip()` handles Unicode whitespace characters
- `trim()` only handles ASCII whitespace (codepoint ≤ U+0020)

---

## Files Methods

Simplified file I/O operations.

```java
Path path = Path.of("example.txt");

// Write string directly to file
Files.writeString(path, "Hello, Java 11!");

// Read entire file as string
String content = Files.readString(path);

// With charset (defaults to UTF-8)
Files.writeString(path, content, StandardCharsets.UTF_8);

// Path.of() factory method (Java 11)
Path p1 = Path.of("dir", "subdir", "file.txt");
Path p2 = Path.of("/home/user/file.txt");
```

---

## Local var in Lambdas (JEP 323)

Use `var` in lambda parameters to enable annotations.

```java
// Without var - cannot add annotations
list.forEach(item -> process(item));

// With var - can add annotations
list.forEach((@NotNull var item) -> process(item));

// Multiple parameters
BiFunction<String, String, String> concat = 
    (var a, var b) -> a + b;

// Useful for annotation-driven frameworks
list.stream()
    .filter((@Nonnull var item) -> item.isValid())
    .collect(toList());
```

---

## Single-File Source Programs (JEP 330)

Run Java source files directly without explicit compilation.

```bash
# Before Java 11
javac HelloWorld.java
java HelloWorld

# Java 11+
java HelloWorld.java

# With arguments
java MyScript.java arg1 arg2

# Shebang support (Unix)
#!/usr/bin/java --source 11
public class Script {
    public static void main(String[] args) {
        System.out.println("Hello from script!");
    }
}
```

---

## Collection and Optional Enhancements

```java
// Collection.toArray() with IntFunction
List<String> list = List.of("a", "b", "c");
String[] array = list.toArray(String[]::new);

// Optional.isEmpty() - inverse of isPresent()
Optional<String> opt = Optional.empty();
if (opt.isEmpty()) {
    System.out.println("No value present");
}

// Predicate.not() - negate predicates
List<String> filtered = list.stream()
    .filter(Predicate.not(String::isBlank))
    .collect(toList());

// Same as:
// .filter(s -> !s.isBlank())
```

---

## Other Improvements

### Nest-Based Access Control (JEP 181)

Inner classes can access private members of enclosing classes without synthetic bridge methods.

```java
public class Outer {
    private String secret = "hidden";
    
    class Inner {
        void printSecret() {
            // Direct access, no synthetic accessor needed
            System.out.println(secret);
        }
    }
}
```

### Epsilon GC (JEP 318)

No-op garbage collector for performance testing:
```bash
java -XX:+UseEpsilonGC MyApp
```

### Java Flight Recorder (JEP 328)

Profiling and diagnostics tool now open-source:
```bash
java -XX:StartFlightRecording=filename=recording.jfr MyApp
```

### TLS 1.3 Support (JEP 332)

Modern TLS protocol support enabled by default.

---

## Migration from Java 8

When upgrading from Java 8 to 11:

1. **Module system** - Add `module-info.java` or use unnamed module
2. **Removed APIs** - Java EE modules removed (javax.xml.bind, etc.)
   ```xml
   <!-- Add dependencies for removed modules -->
   <dependency>
       <groupId>jakarta.xml.bind</groupId>
       <artifactId>jakarta.xml.bind-api</artifactId>
       <version>4.0.0</version>
   </dependency>
   ```
3. **Internal APIs** - Replace `sun.misc.Unsafe` usage where possible
4. **Removed tools** - `javah` removed (use `javac -h`)
