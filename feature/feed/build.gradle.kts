plugins {
    id("catslist.kmp.android.library")
    id("catslist.kmp.compose")
    id("catslist.koin")
    // CatsListNavKey is @Serializable, for Navigation 3's saved-state support.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.example.catslist.feature.feed"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:ui"))
            implementation(project(":core:designsystem"))

            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // The Koin BOM that `catslist.koin` adds to commonMain pins this one's version.
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.androidx.paging.compose)
        }

        // The ViewModel, StateHolder and ErrorHandler tests, on the desktop JVM: nothing in
        // them is Android, so `verify` runs them without an emulator or Robolectric.
        jvmTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.truth)
            implementation(libs.androidx.paging.testing)
        }

        androidDeviceTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.androidx.junit)
            // The host activity, so a test can run the composable edge-to-edge as the app does.
            implementation(libs.androidx.activity.compose)
            // Compose's test rule syncs through Espresso, and the version it pulls in
            // transitively (3.5.0) reflects on an InputManager method this platform no longer has.
            implementation(libs.androidx.espresso.core)
            implementation(libs.truth)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}
