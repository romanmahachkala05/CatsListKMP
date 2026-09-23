plugins {
    id("catslist.kmp.android.library")
    id("catslist.kmp.compose")
    id("catslist.koin")
}

/**
 * The whole UI above the screens: the navigation between them, the bottom bar and the Koin
 * module list. What is left in each app is only what that platform owns — an Activity or a
 * window, and starting Koin.
 */
kotlin {
    android {
        namespace = "com.example.catslist.shared"
    }

    sourceSets {
        commonMain.dependencies {
            // `api`: the apps start Koin with [appModules], whose elements are these modules'.
            api(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(project(":core:designsystem"))
            implementation(project(":feature:feed"))
            implementation(project(":feature:favorites"))

            implementation(libs.compose.mp.material.icons.core)
            implementation(libs.koin.compose)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.junit)
            implementation(libs.truth)
        }
    }
}
