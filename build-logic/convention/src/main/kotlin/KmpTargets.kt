import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * A device and the simulator on Apple silicon. No `iosX64`: nothing here builds on an Intel
 * Mac, and a target nobody compiles is a claim the build cannot back up.
 */
internal fun KotlinMultiplatformExtension.iosTargets() {
    iosArm64()
    iosSimulatorArm64()
}

/**
 * The default hierarchy plus `jvmAndAndroid`: code for the two JVM-based targets only — the
 * shared OkHttp client, JUnit rules. Declared in every module so a source set by that name
 * means the same thing wherever it appears.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal fun KotlinMultiplatformExtension.jvmAndAndroidHierarchy() {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }
}
