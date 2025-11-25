// =============================================================================
// Example: Multi-Project Build - Root build.gradle.kts
// =============================================================================
// The root build file configures settings shared across ALL subprojects.
// Keep this minimal - use convention plugins for detailed configuration.
// =============================================================================

plugins {
    // Apply plugins without activating them
    // This makes them available to subprojects without version conflicts
    java apply false
    kotlin("jvm") version "1.9.21" apply false
    id("org.springframework.boot") version "3.2.0" apply false
}

// =============================================================================
// All Projects (including root)
// =============================================================================

allprojects {
    group = "com.example.myapp"
    version = "1.0.0-SNAPSHOT"
}

// =============================================================================
// Subprojects Only
// =============================================================================

subprojects {
    // Apply to all subprojects
    apply(plugin = "java-library")
    
    // Java toolchain for all subprojects
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    
    // Common test configuration
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    
    // Consistent encoding
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

// =============================================================================
// Aggregation Tasks
// =============================================================================

tasks.register("buildAll") {
    description = "Build all modules"
    group = "build"
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("testAll") {
    description = "Run tests in all modules"
    group = "verification"
    dependsOn(subprojects.map { it.tasks.named("test") })
}

tasks.register("cleanAll") {
    description = "Clean all modules"
    group = "build"
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

// =============================================================================
// Dependency Report for debugging
// =============================================================================

tasks.register("allDependencies") {
    description = "Show dependencies for all modules"
    group = "help"
    
    doLast {
        subprojects.forEach { project ->
            println("\n=== ${project.name} ===")
            project.configurations
                .filter { it.isCanBeResolved }
                .forEach { config ->
                    println("  ${config.name}:")
                    config.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                        println("    - ${artifact.moduleVersion.id}")
                    }
                }
        }
    }
}
