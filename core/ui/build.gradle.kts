plugins {
    id("catslist.kmp.android.library")
    id("catslist.kmp.compose")
    id("catslist.koin")
}

kotlin {
    android {
        namespace = "com.example.catslist.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            // The use cases and AppError, not the data layer that implements them.
            implementation(project(":core:domain"))

            implementation(libs.androidx.lifecycle.viewmodel)
            // UiText.Resource holds its format arguments as an ImmutableList, so this type is
            // part of a public signature here (ADR-0029).
            api(libs.kotlinx.collections.immutable)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.truth)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
