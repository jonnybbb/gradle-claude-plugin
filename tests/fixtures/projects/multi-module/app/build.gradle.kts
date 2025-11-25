// app module - application entry point
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":api"))
    
    runtimeOnly(libs.logback)
    
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("com.example.app.Main")
}
