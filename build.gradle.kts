// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

/**
 * The gate every change has to pass; needs no device. Depends on each subproject's own `check`
 * rather than named task paths, so a new module is wired in without touching this.
 */
tasks.register("verify") {
    group = "verification"
    description = "Checks formatting and static analysis, assembles the debug APK, runs every unit test in every module. No device needed."
    dependsOn(":app:assembleDebug")
    // `include(":core:data")` also creates an unbuildable ":core" grouping project.
    dependsOn(subprojects.filter { it.buildFile.exists() }.map { "${it.path}:check" })
}

/**
 * The gate before a PR. Adds the instrumented tests, which need a device and are the only
 * coverage of the paths that can destroy user data.
 */
tasks.register("verifyOnDevice") {
    group = "verification"
    description = "Everything in `verify`, plus every module's instrumented tests. Needs a device."
    dependsOn("verify")
    dependsOn(
        subprojects
            .filter { it.buildFile.exists() && it.projectDir.resolve("src/androidTest").exists() }
            .map { "${it.path}:connectedDebugAndroidTest" },
    )
}
