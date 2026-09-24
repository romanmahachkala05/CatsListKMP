import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Additive: applied alongside `catslist.android.library` or `catslist.kmp.android.library` by
 * modules that declare a Koin module. Unlike the Hilt plugin it replaces, this applies no KSP
 * — Koin resolves at runtime, so a module that needs KSP for something else (Room in
 * `:core:data`) now applies it itself.
 *
 * A multiplatform module has no flat `implementation` configuration, so the dependencies go
 * to `commonMain`/`commonTest` there and to the Android configurations everywhere else.
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val bom = versionCatalog.findLibrary("koin-bom").get()
            val core = versionCatalog.findLibrary("koin-core").get()
            val android = versionCatalog.findLibrary("koin-android").get()
            val test = versionCatalog.findLibrary("koin-test").get()
            val testJunit4 = versionCatalog.findLibrary("koin-test-junit4").get()

            val kmp = extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (kmp == null) {
                dependencies {
                    add("implementation", platform(bom))
                    add("implementation", android)
                    // The graph is checked by a verify() test per module rather than by the
                    // compiler, so every Koin module gets the test harness (ADR-0026).
                    add("testImplementation", platform(bom))
                    add("testImplementation", test)
                    add("testImplementation", testJunit4)
                }
            } else {
                kmp.sourceSets.named("commonMain").configure {
                    dependencies {
                        implementation(project.dependencies.platform(bom))
                        implementation(core)
                    }
                }
                // Only where there is an Android target: the desktop app has none.
                kmp.sourceSets.matching { it.name == "androidMain" }.configureEach {
                    dependencies { implementation(android) }
                }
                kmp.sourceSets.named("commonTest").configure {
                    dependencies {
                        implementation(project.dependencies.platform(bom))
                        implementation(test)
                    }
                }
            }
        }
    }
}
