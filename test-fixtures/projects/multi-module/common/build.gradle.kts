// common module - shared utilities
plugins {
    `java-library`
}

dependencies {
    api(libs.guava)
    implementation(libs.slf4j.api)
    
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
