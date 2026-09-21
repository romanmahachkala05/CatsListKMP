import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
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
                compileSdk = 37

                defaultConfig {
                    minSdk = 27
                    // Without this AGP falls back to the legacy runner, which discovers no
                    // JUnit4 tests: instrumented tests then run zero tests and report success.
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                testOptions {
                    unitTests {
                        // Keeps android.util.Log, a JVM stub that throws, out of the way.
                        isReturnDefaultValues = true
                    }
                }
            }

        }
    }
}
