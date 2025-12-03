plugins {
    id("com.gradle.develocity") version "4.2.2"
}

// Load environment variables from local.env if it exists
val localEnvFile = file("local.env")
val localEnv = mutableMapOf<String, String>()
if (localEnvFile.exists()) {
    localEnvFile.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
        .forEach { line ->
            val (key, value) = line.split("=", limit = 2)
            localEnv[key.trim()] = value.trim()
        }
}

val develocityServerUrl = localEnv["DEVELOCITY_SERVER"]?.let {
    providers.environmentVariable("DEVELOCITY_SERVER")
        .orElse(it)
        .get()
}

develocity {
    server.set(develocityServerUrl)
    buildScan {
        publishing.onlyIf { true }
    }
}

rootProject.name = "gradle-claude-plugin-tests"
