import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Additive: applied alongside `catslist.kmp.android.library` (or, for the desktop app, a bare
 * `jvm()` target) by modules that render Compose UI. The multiplatform counterpart of `catslist.compose`, which stays for
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
                // Desktop UI tests (`runComposeUiTest`), plus what a desktop app has that a bare
                // JVM does not: Skia's native library for the machine running them — even
                // reading a string asks it for the system theme — and a `Dispatchers.Main`,
                // which Paging's Compose collector runs on. Without either, a test fails to
                // initialise rather than failing an assertion. Matched lazily: `:desktopApp`
                // declares its `jvm()` target after this plugin is applied.
                sourceSets.matching { it.name == "jvmTest" }.configureEach {
                    dependencies {
                        implementation(library("compose-mp-ui-test"))
                        implementation(ComposePlugin.DesktopDependencies.currentOs)
                        implementation(library("kotlinx-coroutines-swing"))
                    }
                }
            }
        }
    }

    private fun Project.library(alias: String) = versionCatalog.findLibrary(alias).get()
}
