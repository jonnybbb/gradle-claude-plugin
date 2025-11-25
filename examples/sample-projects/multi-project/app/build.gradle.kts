// =============================================================================
// Example: Application Module - app/build.gradle.kts
// =============================================================================
// Demonstrates a typical application module using convention plugins
// and version catalogs.
// =============================================================================

plugins {
    // Apply our convention plugin (from buildSrc)
    id("java-conventions")
    
    // Application plugin for runnable apps
    application
    
    // Spring Boot (if using Spring)
    // alias(libs.plugins.spring.boot)
}

// =============================================================================
// Project Dependencies
// =============================================================================

dependencies {
    // Internal project dependencies
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":libs:database"))
    
    // With typesafe project accessors (if enabled in settings):
    // implementation(projects.core)
    // implementation(projects.common)
    // implementation(projects.libs.database)
    
    // External dependencies from version catalog
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    
    // Runtime only
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.postgresql)
    
    // Test dependencies (junit etc. come from convention plugin)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
}

// =============================================================================
// Application Configuration
// =============================================================================

application {
    mainClass.set("com.example.myapp.MainKt")
    
    // JVM arguments
    applicationDefaultJvmArgs = listOf(
        "-Xmx512m",
        "-Dspring.profiles.active=\${APP_PROFILE:-dev}"
    )
}

// =============================================================================
// Distribution Configuration
// =============================================================================

distributions {
    main {
        distributionBaseName.set("myapp")
        
        contents {
            // Include configuration files
            from("src/main/resources") {
                into("config")
                include("*.yml", "*.properties")
            }
            
            // Include scripts
            from("scripts") {
                into("bin")
                filePermissions {
                    unix("rwxr-xr-x")
                }
            }
        }
    }
}

// =============================================================================
// Custom Tasks
// =============================================================================

// Generate build info
tasks.register("generateBuildInfo") {
    val outputFile = layout.buildDirectory.file("resources/main/build-info.properties")
    outputs.file(outputFile)
    
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
                build.version=${project.version}
                build.timestamp=${java.time.Instant.now()}
                build.gradle=${gradle.gradleVersion}
                git.branch=${System.getenv("GIT_BRANCH") ?: "unknown"}
                git.commit=${System.getenv("GIT_COMMIT") ?: "unknown"}
            """.trimIndent())
        }
    }
}

tasks.named("processResources") {
    dependsOn("generateBuildInfo")
}

// =============================================================================
// Run Task Configuration
// =============================================================================

tasks.named<JavaExec>("run") {
    // Enable debugging
    jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    
    // Environment variables
    environment("APP_ENV", "development")
    environment("LOG_LEVEL", "DEBUG")
    
    // System properties from gradle properties
    val dbUrl = providers.gradleProperty("database.url").orNull
    if (dbUrl != null) {
        systemProperty("database.url", dbUrl)
    }
}
