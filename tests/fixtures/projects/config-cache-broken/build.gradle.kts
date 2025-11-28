// Test Fixture: config-cache-broken
// Purpose: Intentional config cache issues for detection testing
// Expected: 15+ issues detected

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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    mainClass.set("com.example.Main")
}

// ❌ ISSUE 1: System.getProperty at configuration time
val dbUrl = System.getProperty("db.url", "jdbc:h2:mem:test")

// ❌ ISSUE 2: System.getenv at configuration time
val apiKey = System.getenv("API_KEY") ?: "default-key"

// ❌ ISSUE 3: Eager task creation
tasks.create("eagerTask1") {
    doLast {
        println("This task was created eagerly")
    }
}

// ❌ ISSUE 4: Another eager task creation
tasks.create("eagerTask2") {
    doLast {
        println("Another eager task")
    }
}

// ❌ ISSUE 5: Eager task creation with type
tasks.create<Copy>("eagerCopy") {
    from("src")
    into(layout.buildDirectory.dir("copied"))
}

// ❌ ISSUE 6: Eager task reference with getByName
tasks.getByName("jar") {
    doLast {
        println("Configuring jar eagerly")
    }
}

// ❌ ISSUE 7: Another getByName
tasks.getByName("test") {
    // Force eager configuration
}

// ❌ ISSUE 8: project.copy in doLast
tasks.register("copyResources") {
    doLast {
        project.copy {
            from("src/main/resources")
            into("$buildDir/config")
        }
    }
}

// ❌ ISSUE 9: Capturing project reference at execution time
tasks.register("runScript") {
    doLast {
        // Capturing project at execution time is a config cache issue
        val projectRef = project
        println("Project name: ${projectRef.name}")
    }
}

// ❌ ISSUE 10: project.delete in doLast
tasks.register("cleanTemp") {
    doLast {
        project.delete("$buildDir/temp")
    }
}

// ❌ ISSUE 11: Task.project access at execution time
tasks.register("accessProject") {
    doLast {
        val outputDir = project.buildDir
        println("Build dir: $outputDir")
    }
}

// ❌ ISSUE 12: project.file in doLast
tasks.register("readConfig") {
    doLast {
        val configFile = project.file("config.properties")
        if (configFile.exists()) {
            println(configFile.readText())
        }
    }
}

// ❌ ISSUE 13: System.getProperty in doLast
tasks.register("printSystemProp") {
    doLast {
        val javaHome = System.getProperty("java.home")
        println("Java home: $javaHome")
    }
}

// ❌ ISSUE 14: System.getenv in doLast
tasks.register("printEnvVar") {
    doLast {
        val path = System.getenv("PATH")
        println("Path: $path")
    }
}

// ❌ ISSUE 15: Accessing sourceSets at execution time
tasks.register("runJava") {
    doLast {
        // Accessing sourceSets at execution time is a config cache issue
        val cp = sourceSets.main.get().runtimeClasspath
        println("Classpath has ${cp.files.size} entries")
    }
}

// ❌ ISSUE 16: buildDir direct access
tasks.register("useBuildDir") {
    val outputFile = file("$buildDir/output.txt")
    outputs.file(outputFile)
    
    doLast {
        outputFile.writeText("Generated content")
    }
}
