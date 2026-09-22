plugins {
    id("catslist.android.library")
    id("catslist.compose")
}

android {
    namespace = "com.example.catslist.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    // No network fetcher here: :app registers one explicitly over the shared OkHttpClient
    // (ADR-0026). This module's own instrumented tests load `file://` URLs, which Coil
    // serves without one.

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
