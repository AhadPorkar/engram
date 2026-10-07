pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Engram"

include(":app")

// Pure Kotlin (no Android) — the learning engine, unit-tested on the JVM
include(":core:srs")
include(":core:model")
include(":core:learning")

// Android core
include(":core:database")
include(":core:data")
include(":core:designsystem")

// Features
include(":feature:decks")
include(":feature:editor")
include(":feature:study")
include(":feature:stats")
include(":feature:settings")
