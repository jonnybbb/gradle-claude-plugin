// =============================================================================
// Example: Incremental Task Implementation
// =============================================================================
// Demonstrates how to create tasks that only process changed files,
// dramatically improving build performance.
// =============================================================================

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

// =============================================================================
// Basic Incremental Task
// =============================================================================

/**
 * Processes source files incrementally - only changed files are reprocessed.
 * 
 * Key annotations:
 * - @Incremental: Marks input for change tracking
 * - @PathSensitive: Defines how file paths affect caching
 */
abstract class ProcessSourcesTask : DefaultTask() {
    
    // Input directory - marked incremental for change tracking
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    // Output directory
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    // Configuration option (non-file input)
    @get:Input
    abstract val prefix: Property<String>
    
    @TaskAction
    fun process(inputChanges: InputChanges) {
        // Check if this is incremental or full rebuild
        val incremental = inputChanges.isIncremental
        logger.lifecycle("Processing ${if (incremental) "incrementally" else "from scratch"}")
        
        if (!incremental) {
            // Full rebuild - clear outputs and process everything
            outputDir.get().asFile.deleteRecursively()
            outputDir.get().asFile.mkdirs()
        }
        
        // Process only changed files
        inputChanges.getFileChanges(sourceDir).forEach { change ->
            val targetFile = outputDir.file(change.file.name).get().asFile
            
            when (change.changeType) {
                ChangeType.REMOVED -> {
                    logger.lifecycle("Removing: ${change.file.name}")
                    targetFile.delete()
                }
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    logger.lifecycle("Processing: ${change.file.name}")
                    processFile(change.file, targetFile)
                }
            }
        }
    }
    
    private fun processFile(source: File, target: File) {
        // Example: add prefix to each line
        val content = source.readText()
        val processed = content.lines()
            .joinToString("\n") { "${prefix.get()}$it" }
        target.parentFile.mkdirs()
        target.writeText(processed)
    }
}

// =============================================================================
// Register the Task
// =============================================================================

tasks.register<ProcessSourcesTask>("processSources") {
    sourceDir.set(layout.projectDirectory.dir("src/main/resources"))
    outputDir.set(layout.buildDirectory.dir("processed"))
    prefix.set("[PROCESSED] ")
}

// =============================================================================
// Incremental Task with Multiple Input Types
// =============================================================================

/**
 * More complex incremental task with multiple input sources.
 */
abstract class TransformFilesTask : DefaultTask() {
    
    // Primary source - incremental
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    // Template file - any change triggers full rebuild
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val templateFile: RegularFileProperty
    
    // Configuration - any change triggers full rebuild
    @get:Input
    abstract val transformMode: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun transform(inputChanges: InputChanges) {
        // Template or config changes = full rebuild
        if (!inputChanges.isIncremental) {
            logger.lifecycle("Full rebuild required")
            outputDir.get().asFile.deleteRecursively()
        }
        
        val template = templateFile.get().asFile.readText()
        
        inputChanges.getFileChanges(sourceDir).forEach { change ->
            if (change.changeType != ChangeType.REMOVED) {
                if (change.file.isFile) {
                    transformFile(change.file, template)
                }
            }
        }
    }
    
    private fun transformFile(source: File, template: String) {
        val output = outputDir.file(source.nameWithoutExtension + ".out").get().asFile
        val content = template.replace("{{content}}", source.readText())
        output.parentFile.mkdirs()
        output.writeText(content)
    }
}

// =============================================================================
// Runtime Classpath Normalization
// =============================================================================

/**
 * Task that processes classpath but ignores irrelevant changes.
 */
abstract class AnalyzeClasspathTask : DefaultTask() {
    
    // Classpath input - normalized to ignore timestamps, ordering
    @get:Classpath  // Equivalent to @InputFiles + runtime normalization
    abstract val classpath: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val report: RegularFileProperty
    
    @TaskAction
    fun analyze() {
        val classes = classpath.files.flatMap { file ->
            if (file.isDirectory) {
                file.walkTopDown().filter { it.extension == "class" }.toList()
            } else {
                listOf(file)
            }
        }
        
        report.get().asFile.writeText(
            "Analyzed ${classes.size} class files"
        )
    }
}

// =============================================================================
// Task with Optional Inputs
// =============================================================================

abstract class FlexibleProcessTask : DefaultTask() {
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty
    
    // Optional input - task runs even if not set
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFile: RegularFileProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() {
        val config = if (configFile.isPresent) {
            configFile.get().asFile.readText()
        } else {
            "default config"
        }
        
        logger.lifecycle("Using config: $config")
        // Process files...
    }
}

// =============================================================================
// Best Practices Summary
// =============================================================================
//
// 1. Always use @Incremental on primary input directories
// 2. Use appropriate @PathSensitive annotation:
//    - RELATIVE: File content and relative path matter
//    - NAME_ONLY: Only filename matters
//    - NONE: Only content matters
//    - ABSOLUTE: Full path matters (rare)
//
// 3. Use @Classpath for compile/runtime classpaths
// 4. Use @CompileClasspath for compile-only classpaths  
// 5. Check inputChanges.isIncremental for full vs incremental
// 6. Handle all ChangeTypes: ADDED, MODIFIED, REMOVED
// 7. Use @Optional for inputs that may not exist
//
// Benefits:
// - Only process changed files
// - Better build cache hits
// - Faster incremental builds
// - Configuration cache compatible
// =============================================================================
