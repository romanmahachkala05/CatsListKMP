plugins {
    id("catslist.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // All `api`: every one of these appears in a use case or repository signature,
            // so a consumer of this module needs them on its own compile classpath.
            api(project(":core:model"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
            // paging-common, not paging-runtime: the Android-only half is the UI's concern.
            api(libs.androidx.paging.common)
        }
    }
}
