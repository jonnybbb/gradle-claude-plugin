// api module - interfaces
plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
