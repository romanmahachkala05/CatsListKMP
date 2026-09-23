pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    // Auto-provisions a JDK 17 toolchain for compilation/tests so the build does
    // not depend on which JDK happens to run the Gradle daemon.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CatsList"
include(":app")
include(":shared")
include(":desktopApp")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:testing")
include(":core:ui")
include(":core:designsystem")
include(":feature:feed")
include(":feature:favorites")
