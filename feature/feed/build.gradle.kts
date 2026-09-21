plugins {
    id("catslist.android.library")
    id("catslist.compose")
    id("catslist.koin")
    // CatsListNavKey is @Serializable, for Navigation 3's saved-state support.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.catslist.feature.feed"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.paging.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.paging.testing)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
    // The host activity, so a test can run the composable edge-to-edge as the app does.
    androidTestImplementation(libs.androidx.activity.compose)
    // Compose's test rule syncs through Espresso, and the version it pulls in transitively
    // (3.5.0) reflects on an InputManager method this platform no longer has.
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
