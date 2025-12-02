plugins {
    id("com.gradle.develocity") version "4.2.2"
}

develocity {
    server.set("https://ge.gradle.org")
    buildScan {
        publishing.onlyIf { true }
    }
}

rootProject.name = "gradle-claude-plugin-tests"
