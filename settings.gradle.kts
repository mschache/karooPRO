pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // karoo-ext is published to GitHub Packages (needs a token) and built by JitPack from the
        // same tags (no token). JitPack keeps the project buildable for anyone who clones it.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.hammerheadnav") }
        }
    }
}

rootProject.name = "KarooPRO"
include("app")
