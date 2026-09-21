import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * For modules that compile for more than one platform. Replaces `catslist.jvm.library` as
 * modules move to `commonMain`.
 *
 * Only `jvm()` is declared — that is the desktop target, and it is the one non-Android
 * platform this build can actually compile and test on any host. iOS targets need a macOS
 * machine, so they are not declared here rather than being declared and never built.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("catslist.quality")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvm()

                compilerOptions {
                    // An Android consumer resolves the `jvm` variant of these modules, so the
                    // bytecode level has to match what AGP compiles the rest of the app to.
                    jvmToolchain(17)
                }
            }
        }
    }
}
