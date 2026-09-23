import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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

        nativeDistributions {
            // Each is built on its own OS: jpackage cannot cross-package (see release.yml).
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "CatsList"
            packageVersion = providers.gradleProperty("catslist.version").get()
            description = "An endless feed of cats, with favorites and downloads."
            vendor = "CatsList"

            // The JDK modules jlink keeps beyond its default set, as `suggestRuntimeModules`
            // (jdeps) found them. Checked by running the packaged app, not just by building it:
            // a missing module compiles fine and fails when the code that needs it first runs.
            modules("java.instrument", "jdk.unsupported")

            windows {
                iconFile.set(project.file("icons/app-icon.ico"))
                menuGroup = "CatsList"
                perUserInstall = true
                shortcut = true
                // Fixed for good: it is how Windows recognises a newer MSI as an upgrade of
                // this app rather than a second, separate install.
                upgradeUuid = "4b2d9a4e-7c1f-4d8e-9a53-2f6c0e8b1d47"
            }
            macOS {
                iconFile.set(project.file("icons/app-icon.icns"))
                bundleID = "com.example.catslist"
            }
            linux {
                iconFile.set(project.file("icons/app-icon.png"))
            }
        }
    }
}
