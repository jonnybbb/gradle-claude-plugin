// =============================================================================
// Example: Configuration Cache Compatible Build
// =============================================================================
// This build.gradle.kts demonstrates patterns that work with Gradle's
// configuration cache (--configuration-cache flag).
//
// Key principles:
// 1. No Project references at execution time
// 2. Use Provider API for deferred values
// 3. Inject services instead of using project methods
// =============================================================================

plugins {
    java
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

// =============================================================================
// ✅ GOOD: Provider-based property access
// =============================================================================

// Use providers for system properties
val dbUrl = providers.systemProperty("db.url")
    .orElse("jdbc:h2:mem:default")

// Use providers for environment variables  
val environment = providers.environmentVariable("APP_ENV")
    .orElse("development")

// Use providers for Gradle properties
val debugEnabled = providers.gradleProperty("debug.enabled")
    .map { it.toBoolean() }
    .orElse(false)

// =============================================================================
// ✅ GOOD: Lazy task registration
// =============================================================================

// Register tasks lazily - configuration only runs when task is needed
tasks.register("generateConfig") {
    val outputFile = layout.buildDirectory.file("config/app.properties")
    outputs.file(outputFile)
    
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
                db.url=${dbUrl.get()}
                environment=${environment.get()}
                debug=${debugEnabled.get()}
            """.trimIndent())
        }
    }
}

// =============================================================================
// ✅ GOOD: Typed task with Provider inputs
// =============================================================================

abstract class ProcessDataTask : DefaultTask() {
    
    @get:Input
    abstract val inputData: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    // ✅ Inject services instead of using project.exec()
    @get:Inject
    abstract val execOperations: ExecOperations
    
    @TaskAction
    fun process() {
        val data = inputData.get()
        outputFile.get().asFile.writeText("Processed: $data")
        
        // ✅ Use injected service
        execOperations.exec {
            commandLine("echo", "Processing complete")
        }
    }
}

tasks.register<ProcessDataTask>("processData") {
    inputData.set("sample-input")
    outputFile.set(layout.buildDirectory.file("output/data.txt"))
}

// =============================================================================
// ✅ GOOD: File operations with injected services
// =============================================================================

abstract class CopyDocsTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val destDir: DirectoryProperty
    
    // ✅ Inject FileSystemOperations instead of project.copy()
    @get:Inject
    abstract val fsOps: FileSystemOperations
    
    @TaskAction
    fun copyDocs() {
        fsOps.copy {
            from(sourceDir)
            into(destDir)
            include("**/*.md")
        }
    }
}

tasks.register<CopyDocsTask>("copyDocs") {
    sourceDir.set(layout.projectDirectory.dir("docs"))
    destDir.set(layout.buildDirectory.dir("docs"))
}

// =============================================================================
// ✅ GOOD: Archive operations with injected services
// =============================================================================

abstract class CreateArchiveTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    @get:OutputFile
    abstract val archiveFile: RegularFileProperty
    
    @get:Inject
    abstract val archiveOps: ArchiveOperations
    
    @TaskAction
    fun createArchive() {
        archiveOps.zipTo(archiveFile.get().asFile) {
            from(sourceDir)
        }
    }
}

// =============================================================================
// ✅ GOOD: Lazy task dependencies
// =============================================================================

tasks.named("processResources") {
    dependsOn("generateConfig")
}

// =============================================================================
// ✅ GOOD: Conditional configuration with providers
// =============================================================================

tasks.register("conditionalTask") {
    // Use provider for conditional logic
    onlyIf { debugEnabled.get() }
    
    doLast {
        println("Running in debug mode")
    }
}

// =============================================================================
// ✅ GOOD: Build directory access via layout
// =============================================================================

tasks.register("showPaths") {
    // ✅ Use layout API instead of buildDir
    val buildDir = layout.buildDirectory
    val projectDir = layout.projectDirectory
    
    doLast {
        println("Build dir: ${buildDir.get()}")
        println("Project dir: $projectDir")
    }
}

// =============================================================================
// Application configuration
// =============================================================================

application {
    mainClass.set("com.example.Main")
    
    // ✅ Use provider for application args
    applicationDefaultJvmArgs = listOf(
        "-Ddb.url=${dbUrl.get()}",
        "-Dapp.env=${environment.get()}"
    )
}

// =============================================================================
// Test task configuration
// =============================================================================

tasks.test {
    useJUnitPlatform()
    
    // ✅ System properties via providers
    systemProperty("test.db.url", dbUrl.get())
}
