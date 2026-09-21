import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Additive: applied alongside `catslist.android.library` by modules that declare a Koin module.
 * Unlike the Hilt plugin it replaces, this applies no KSP — Koin resolves at runtime, so a
 * module that needs KSP for something else (Room in `:core:data`) now applies it itself.
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", platform(versionCatalog.findLibrary("koin-bom").get()))
                add("implementation", versionCatalog.findLibrary("koin-android").get())

                // The graph is checked by a verify() test per module rather than by the
                // compiler, so every Koin module gets the test harness (ADR-0026).
                add("testImplementation", platform(versionCatalog.findLibrary("koin-bom").get()))
                add("testImplementation", versionCatalog.findLibrary("koin-test").get())
                add("testImplementation", versionCatalog.findLibrary("koin-test-junit4").get())
            }
        }
    }
}
