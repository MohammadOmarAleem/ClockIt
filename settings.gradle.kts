pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // Ensure this repository is added
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ClockIt"
include(":app")
