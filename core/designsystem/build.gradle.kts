plugins {
    id("catslist.kmp.android.library")
    id("catslist.kmp.compose")
}

kotlin {
    android {
        namespace = "com.example.catslist.core.designsystem"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:ui"))

            implementation(project.dependencies.platform(libs.coil.bom))
            implementation(libs.coil.compose)
            // No network fetcher here: each app registers one explicitly over the shared
            // OkHttpClient (ADR-0030). This module's tests load `file://` URLs, which Coil
            // serves without one.
        }

        androidMain.dependencies {
            // WindowCompat, for the status bar icons' tint.
            implementation(libs.androidx.core.ktx)
        }

        jvmTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.truth)
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

compose.resources {
    // The features' UI tests find the card's buttons by these content descriptions.
    publicResClass = true
}
