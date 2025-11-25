// Test Fixture: simple-java
// Purpose: Healthy baseline - all best practices applied

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

dependencies {
    implementation(libs.guava)
    implementation(libs.slf4j.api)
    
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.example.Main")
}

// ✅ All lazy task registration
tasks.register("generateBuildInfo") {
    val outputFile = layout.buildDirectory.file("resources/main/build-info.txt")
    outputs.file(outputFile)
    
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("version=${project.version}")
        }
    }
}

// ✅ Lazy task configuration
tasks.named("processResources") {
    dependsOn("generateBuildInfo")
}

// ✅ Lazy typed task configuration
tasks.named<Test>("test") {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

// ✅ ConfigureEach instead of all
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// ✅ Provider-based properties
val appEnv = providers.environmentVariable("APP_ENV").orElse("development")

tasks.named<JavaExec>("run") {
    environment("APP_ENV", appEnv.get())
}
