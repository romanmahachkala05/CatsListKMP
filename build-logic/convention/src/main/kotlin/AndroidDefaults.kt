import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion

/**
 * The Android defaults every library module shares, whether it is Android-only
 * (`catslist.android.library`) or multiplatform with an Android target
 * (`catslist.kmp.android.library`). One copy, so the two cannot drift.
 */
internal fun LibraryExtension.applyCatsListAndroidDefaults() {
    compileSdk = 37

    defaultConfig {
        minSdk = 27
        // Without this AGP falls back to the legacy runner, which discovers no JUnit4 tests:
        // instrumented tests then run zero tests and report success.
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
