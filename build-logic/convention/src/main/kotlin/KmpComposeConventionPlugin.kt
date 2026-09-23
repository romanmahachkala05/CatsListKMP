import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Additive: applied alongside `catslist.kmp.android.library` by modules that render Compose
 * UI from `commonMain`. The multiplatform counterpart of `catslist.compose`, which stays for
 * the Android-only modules until each one moves.
 *
 * Strings and drawables live in `src/commonMain/composeResources/` and are read through a
 * generated `Res` class instead of `R`. Each module gets its own `Res`, in a package derived
 * from its Gradle path — `:core:ui` → `com.example.catslist.core.ui.resources` — so two
 * modules' resources can never collide, the same guarantee Android's per-namespace `R` gave.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }

            configureComposeStability()
            configureComposeMetrics()

            extensions.configure<ComposeExtension> {
                extensions.configure<ResourcesExtension> {
                    packageOfResClass = "com.example.catslist" + path.replace(':', '.') + ".resources"
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.named("commonMain").configure {
                    dependencies {
                        implementation(library("compose-mp-runtime"))
                        implementation(library("compose-mp-foundation"))
                        implementation(library("compose-mp-ui"))
                        implementation(library("compose-mp-material3"))
                        implementation(library("compose-mp-resources"))
                        implementation(library("compose-mp-ui-tooling-preview"))
                    }
                }
                // Skia's native library for the machine running the tests. Even reading a
                // string asks it for the system theme, so without this a desktop test that
                // touches resources fails to initialise rather than failing an assertion.
                sourceSets.named("jvmTest").configure {
                    dependencies {
                        implementation(ComposePlugin.DesktopDependencies.currentOs)
                    }
                }
            }
        }
    }

    private fun Project.library(alias: String) = versionCatalog.findLibrary(alias).get()
}
