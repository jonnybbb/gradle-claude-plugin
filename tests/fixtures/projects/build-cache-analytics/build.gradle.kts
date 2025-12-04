// Test Fixture: build-cache-analytics
// Purpose: Demonstrate build cache behavior for analytics testing
// Scenarios:
//   - CACHE_SCENARIO=fresh - Clean build (all cache misses)
//   - CACHE_SCENARIO=warm - Incremental build (cache hits)
//   - CACHE_SCENARIO=partial - Some inputs changed (partial cache hits)
//   - CACHE_SCENARIO=invalidate - Force cache invalidation

plugins {
    java
    application
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    mainClass.set("com.example.Main")
}

val cacheScenario = providers.environmentVariable("CACHE_SCENARIO").orElse("default").get()
val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0").get()

// Tag the build scan with cache scenario info
develocity {
    buildScan {
        tag("CACHE_ANALYTICS_FIXTURE")
        value("cacheScenario", cacheScenario)
        value("buildNumber", buildNumber)
    }
}

dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Generate version file - input changes based on scenario
tasks.register("generateVersionFile") {
    val outputFile = layout.buildDirectory.file("generated/version.txt")
    val versionContent = if (cacheScenario == "partial" || cacheScenario == "invalidate") {
        // Changing input - will cause cache miss
        "version=$version-$buildNumber-${System.currentTimeMillis()}"
    } else {
        // Stable input - will cache hit
        "version=$version"
    }

    inputs.property("versionContent", versionContent)
    outputs.file(outputFile)

    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(versionContent)
        }
    }
}

// Custom cacheable task that processes data
abstract class DataProcessingTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun process() {
        val input = inputFile.get().asFile.readText()
        val output = "Processed: $input\nTimestamp: ${System.currentTimeMillis()}"
        outputFile.get().asFile.writeText(output)
    }
}

// Register cacheable processing task
tasks.register<DataProcessingTask>("processData") {
    inputFile.set(layout.buildDirectory.file("generated/version.txt"))
    outputFile.set(layout.buildDirectory.file("processed/data.txt"))
    dependsOn("generateVersionFile")

    // Mark as cacheable
    outputs.cacheIf { true }
}

// Generate source file with stable or changing content
tasks.register("generateSources") {
    val outputDir = layout.buildDirectory.dir("generated/sources/java/main")
    val scenario = cacheScenario

    outputs.dir(outputDir)

    // Input that affects caching
    val sourceContent = if (scenario == "invalidate") {
        // Changing content invalidates cache
        """
        package com.example.generated;
        public class BuildInfo {
            public static final String BUILD_TIME = "${System.currentTimeMillis()}";
            public static final String BUILD_NUMBER = "$buildNumber";
        }
        """.trimIndent()
    } else {
        // Stable content - cache hit
        """
        package com.example.generated;
        public class BuildInfo {
            public static final String BUILD_TIME = "stable";
            public static final String BUILD_NUMBER = "stable";
        }
        """.trimIndent()
    }

    inputs.property("sourceContent", sourceContent)

    doLast {
        val generatedDir = outputDir.get().asFile.resolve("com/example/generated")
        generatedDir.mkdirs()
        generatedDir.resolve("BuildInfo.java").writeText(sourceContent)
    }
}

// Add generated sources to compilation
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/java/main"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("generateSources")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Lifecycle task to run the full build
tasks.register("fullBuild") {
    dependsOn("build", "processData")
}
