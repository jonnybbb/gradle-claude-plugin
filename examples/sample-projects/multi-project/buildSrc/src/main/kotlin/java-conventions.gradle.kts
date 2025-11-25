// =============================================================================
// Example: Convention Plugin - java-conventions.gradle.kts
// =============================================================================
// Place in: buildSrc/src/main/kotlin/java-conventions.gradle.kts
// 
// Convention plugins encapsulate build logic that can be reused across
// multiple subprojects. This is cleaner than duplicating configuration
// in every build.gradle.kts file.
// =============================================================================

plugins {
    `java-library`
    `maven-publish`
}

// =============================================================================
// Java Configuration
// =============================================================================

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    
    // Generate sources and javadoc JARs
    withSourcesJar()
    withJavadocJar()
}

// =============================================================================
// Dependencies
// =============================================================================

// Access the version catalog
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    // Common dependencies for all Java projects
    implementation(libs.slf4j.api)
    implementation(libs.guava)
    
    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// =============================================================================
// Compile Options
// =============================================================================

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Xlint:-serial",
        "-Xlint:-processing",
        "-Werror"  // Treat warnings as errors
    ))
}

// =============================================================================
// Test Configuration
// =============================================================================

tasks.test {
    useJUnitPlatform()
    
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    
    // Fail fast
    failFast = false
    
    // JVM args for tests
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Xmx1g"
    )
}

// =============================================================================
// JAR Configuration
// =============================================================================

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Built-By" to System.getProperty("user.name"),
            "Built-JDK" to System.getProperty("java.version"),
            "Built-Gradle" to gradle.gradleVersion
        )
    }
}

// =============================================================================
// Publishing
// =============================================================================

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set(project.name)
                description.set(project.description ?: "")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
        
        // Add company repository
        // maven {
        //     name = "company"
        //     url = uri("https://artifacts.company.com/maven")
        //     credentials {
        //         username = providers.gradleProperty("repo.user").orNull
        //         password = providers.gradleProperty("repo.password").orNull
        //     }
        // }
    }
}

// =============================================================================
// Usage in subproject:
// =============================================================================
//
// // app/build.gradle.kts
// plugins {
//     id("java-conventions")  // Apply this convention plugin
// }
//
// dependencies {
//     // Only project-specific dependencies here
//     implementation(project(":core"))
// }
// =============================================================================
