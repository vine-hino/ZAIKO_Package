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

rootProject.name = "ZAIKO_Package"

include(":app")
include(":core:designsystem")
include(":core:navigation")
include(":feature:auth")
include(":feature:ht-home")
include(":feature:ht-operations")
include(":data:connector-api")
include(":data:connector-ftp")
include(":data:connector-db")
include(":data:connector-cloud")
include(":core:database")
include(":pc-app")
