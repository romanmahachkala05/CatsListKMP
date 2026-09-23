import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The shared Android defaults, used by every module except `:app` and `:core:model`. Compose
 * and Koin are separate additive plugins, applied only by the modules that need them.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("catslist.quality")
            }

            extensions.configure<LibraryExtension> {
                applyCatsListAndroidDefaults()
            }

        }
    }
}
