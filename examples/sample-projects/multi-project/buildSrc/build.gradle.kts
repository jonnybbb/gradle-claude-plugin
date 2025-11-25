// =============================================================================
// buildSrc/build.gradle.kts
// =============================================================================
// Configuration for convention plugins in buildSrc.
// =============================================================================

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Add plugins that convention plugins will apply
    // This makes them available in precompiled script plugins
    
    // Example: If your convention plugin applies the Kotlin plugin
    // implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    
    // Example: Spring Boot plugin
    // implementation("org.springframework.boot:spring-boot-gradle-plugin:3.2.0")
    
    // Access version catalog in buildSrc
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
