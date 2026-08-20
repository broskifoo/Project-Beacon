pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "Beacon"
include(":app")
include(":beacon-sdk:kotlin")
include(":beacon-core:kotlin")
include(":beacon-mesh:kotlin")
include(":beacon-radio:kotlin")