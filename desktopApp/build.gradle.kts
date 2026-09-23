plugins {
    id("catslist.kmp.library")
    id("catslist.kmp.compose")
    id("catslist.koin")
}

/** The desktop entry point: a window around `CatsApp()`, and Koin and Coil started for it. */
kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))

            // The machine's own Skia and AWT integration, so `run` works on whatever builds it.
            implementation(compose.desktop.currentOs)
            // Dispatchers.Main on desktop: the AWT event thread that Compose and the
            // ViewModels run on.
            implementation(libs.kotlinx.coroutines.swing)

            // main() builds Coil's singleton loader over the one OkHttpClient the Koin graph
            // provides, as Android's App does (ADR-0030).
            implementation(libs.okhttp)
            implementation(project.dependencies.platform(libs.coil.bom))
            implementation(libs.coil)
            implementation(libs.coil.core)
            implementation(libs.coil.network.okhttp)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.example.catslist.MainKt"
    }
}
