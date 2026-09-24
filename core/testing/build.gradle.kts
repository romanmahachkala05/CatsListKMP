plugins {
    id("catslist.kmp.android.library")
}

kotlin {
    android {
        namespace = "com.example.catslist.core.testing"
    }

    sourceSets {
        commonMain.dependencies {
            // Fakes implement the contracts :core:data/:core:ui own (CatRepository,
            // CatApiService, CatDao, ImageDownloader, SnackbarNotifier), so this depends on
            // them rather than the reverse. :core:data's own tests depend back on this module
            // — a test-to-main dependency in the other direction, not a cycle.
            api(project(":core:model"))
            api(project(":core:data"))
            api(project(":core:ui"))

            api(libs.kotlinx.coroutines.test)
            api(libs.kotlinx.collections.immutable)
        }

        // MainDispatcherRule is a JUnit 4 rule, and JUnit exists only on the JVM. The fakes
        // above are plain Kotlin and stay common; the rule lives in a source set the JVM and
        // Android targets share (`catslist.kmp.android.library` declares it), so iOS does not
        // inherit JUnit.
        named("jvmAndAndroidMain").dependencies {
            api(libs.junit)
        }
    }
}
