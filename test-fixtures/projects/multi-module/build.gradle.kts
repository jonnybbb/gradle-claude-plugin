// Test Fixture: multi-module
// Root build configuration

plugins {
    java apply false
}

allprojects {
    group = "com.example"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java-library")
    
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
