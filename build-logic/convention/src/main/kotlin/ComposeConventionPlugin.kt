import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Additive: applied alongside `catslist.android.library` by modules that render Compose UI. */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            configureComposeStability()
            configureComposeMetrics()

            val bom = versionCatalog.findLibrary("androidx-compose-bom").get()
            dependencies.add("implementation", dependencies.platform(bom))
            // The same BOM for the UI tests, so a Compose module that adds one does not have
            // to remember to pin its test artifacts separately.
            dependencies.add("androidTestImplementation", dependencies.platform(bom))
        }
    }
}

/**
 * Declares stable the classes the compiler cannot work out for itself — a domain model in a
 * module it does not compile, and a third-party type nobody here can annotate (ADR-0033).
 * Always on: this one changes what the compiler generates, not what it reports.
 */
private fun Project.configureComposeStability() {
    val config = rootProject.layout.projectDirectory.file("config/compose-stability.conf")
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(config)
    }
}

/**
 * Opt-in Compose compiler metrics: `./gradlew assembleRelease -Pcatslist.composeMetrics`
 * writes, per module, which composables skip and which classes the compiler considers stable
 * (ADR-0033). Off by default — it is diagnostic output, and generating it on every build would
 * cost time no ordinary build gets anything back for.
 */
private fun Project.configureComposeMetrics() {
    if (!providers.gradleProperty(COMPOSE_METRICS_PROPERTY).isPresent) return

    // Each module's own build directory, not one shared folder under the root: the compiler
    // takes this as a plugin option, and a module path in the value carries a `:`, which is
    // the option format's own separator.
    val destination = layout.buildDirectory.dir("compose-metrics")
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        metricsDestination.set(destination)
        reportsDestination.set(destination)
    }
}

private const val COMPOSE_METRICS_PROPERTY = "catslist.composeMetrics"
