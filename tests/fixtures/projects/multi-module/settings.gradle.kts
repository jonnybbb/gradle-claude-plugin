// Test Fixture: multi-module
// Purpose: Multi-project build for scale testing

rootProject.name = "multi-module"

include("app")
include("core")
include("common")
include("api")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
