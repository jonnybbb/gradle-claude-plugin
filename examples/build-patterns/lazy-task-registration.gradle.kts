// =============================================================================
// Example: Lazy Task Registration Patterns
// =============================================================================
// Demonstrates the difference between eager and lazy task APIs.
// Lazy registration improves configuration time significantly.
// =============================================================================

plugins {
    java
}

// =============================================================================
// ❌ BAD: Eager task creation (AVOID)
// =============================================================================

// These tasks are configured immediately, even if never executed

/*
// Don't do this:
tasks.create("eagerTask1") {
    println("Configuring eagerTask1") // Runs during configuration!
    doLast { println("Running eagerTask1") }
}

// Don't do this either:
task("eagerTask2") {
    println("Configuring eagerTask2") // Runs during configuration!
    doLast { println("Running eagerTask2") }
}

// Avoid getByName for configuration:
tasks.getByName("jar") {
    println("Configuring jar eagerly") // Forces jar task creation
}
*/

// =============================================================================
// ✅ GOOD: Lazy task registration
// =============================================================================

// Task is only configured when actually needed
tasks.register("lazyTask1") {
    println("Configuring lazyTask1") // Only runs if task is executed
    doLast { println("Running lazyTask1") }
}

// Typed task registration
tasks.register<Copy>("lazyCopy") {
    from("src")
    into(layout.buildDirectory.dir("copied"))
}

// Named reference for existing tasks (lazy)
tasks.named("jar") {
    // Configuration deferred until jar is actually needed
}

// =============================================================================
// ✅ GOOD: Lazy typed task registration
// =============================================================================

// Define custom task class
abstract class GenerateReportTask : DefaultTask() {
    
    @get:Input
    abstract val reportName: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generate() {
        outputFile.get().asFile.writeText("Report: ${reportName.get()}")
    }
}

// Register lazily
tasks.register<GenerateReportTask>("generateReport") {
    reportName.set("Monthly Summary")
    outputFile.set(layout.buildDirectory.file("reports/summary.txt"))
}

// =============================================================================
// ✅ GOOD: Lazy iteration with configureEach
// =============================================================================

// Configure all tasks of a type lazily
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

// Configure all Test tasks lazily
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

// =============================================================================
// ✅ GOOD: Lazy task dependencies
// =============================================================================

// Use TaskProvider for dependencies
val generateSources = tasks.register("generateSources") {
    val outputDir = layout.buildDirectory.dir("generated")
    outputs.dir(outputDir)
    doLast {
        outputDir.get().asFile.mkdirs()
        // Generate sources...
    }
}

// Reference lazily
tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateSources)
    source(generateSources.map { layout.buildDirectory.dir("generated").get() })
}

// =============================================================================
// ✅ GOOD: Avoiding task realization
// =============================================================================

// This does NOT realize any tasks
val taskNames = tasks.names
println("Available tasks: ${taskNames.size}")

// This WOULD realize tasks (avoid in configuration):
// val allTasks = tasks.toList() // Forces all tasks to be created

// =============================================================================
// ✅ GOOD: Conditional task registration
// =============================================================================

// Only register if condition is met
val enableProfiling = providers.gradleProperty("profiling.enabled")
    .map { it.toBoolean() }
    .orElse(false)

if (enableProfiling.get()) {
    tasks.register("profileBuild") {
        doLast { println("Profiling enabled") }
    }
}

// Or defer the condition check:
tasks.register("maybeProfile") {
    onlyIf { enableProfiling.get() }
    doLast { println("Running profile") }
}

// =============================================================================
// ✅ GOOD: Task registration with dependencies
// =============================================================================

val prepareData = tasks.register("prepareData") {
    val output = layout.buildDirectory.file("data/prepared.json")
    outputs.file(output)
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("{}")
        }
    }
}

val processData = tasks.register("processData") {
    // Lazy dependency - prepareData won't be configured unless processData runs
    dependsOn(prepareData)
    
    // Wire outputs to inputs
    inputs.file(prepareData.flatMap { 
        layout.buildDirectory.file("data/prepared.json") 
    })
    
    doLast { println("Processing...") }
}

// =============================================================================
// Summary: Task API Comparison
// =============================================================================
//
// | Eager (AVOID)          | Lazy (PREFERRED)           |
// |------------------------|----------------------------|
// | tasks.create("x")      | tasks.register("x")        |
// | tasks.getByName("x")   | tasks.named("x")           |
// | tasks.all { }          | tasks.configureEach { }    |
// | tasks.withType().all{} | tasks.withType().configureEach{} |
// | task("x") { }          | tasks.register("x") { }    |
//
// Benefits of lazy registration:
// - Faster configuration phase
// - Lower memory usage
// - Configuration cache compatible
// - Only configure what's needed
// =============================================================================
