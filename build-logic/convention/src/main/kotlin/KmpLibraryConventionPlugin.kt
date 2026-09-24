import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * For modules that compile for more than one platform. Replaces `catslist.jvm.library` as
 * modules move to `commonMain`.
 *
 * `jvm()` is the desktop target; the iOS pair is in [iosTargets]. On a host that cannot build
 * iOS (anything but macOS) Kotlin skips those targets and the rest builds as usual.
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
                iosTargets()

                compilerOptions {
                    // An Android consumer resolves the `jvm` variant of these modules, so the
                    // bytecode level has to match what AGP compiles the rest of the app to.
                    jvmToolchain(17)
                }
            }
        }
    }
}
