import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Still needed after CatDto moved out: the NavKeys are @Serializable too, and without
    // the plugin that fails at runtime rather than at compile time.
    alias(libs.plugins.kotlin.serialization)
    id("catslist.quality")
}

// Release signing credentials, if this machine has any. Read defensively: a fresh clone and
// CI have no keystore, and failing here would fail configuration for every build type.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun releaseSigningValue(key: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv("CATSLIST_${key.uppercase()}")

/** Only true when every part is present; a half-configured signing config fails at package time. */
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !releaseSigningValue(it).isNullOrBlank() }

// versionCode is derived from the name, so the two cannot drift apart. Minor and patch are
// allowed 0-99 each.
val versionMajor = 2
val versionMinor = 2
val versionPatch = 0

android {
    namespace = "com.example.catslist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.catslist"
        minSdk = 27
        targetSdk = 37
        versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigningValue("storeFile")!!)
                storePassword = releaseSigningValue("storePassword")
                keyAlias = releaseSigningValue("keyAlias")
                keyPassword = releaseSigningValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Null with no keystore, producing an unsigned APK rather than a failed build.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        // App.kt gates Koin's reflective logger on DEBUG.
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            // Keeps android.util.Log, a JVM stub that throws, out of the way.
            isReturnDefaultValues = true
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:favorites"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // Koin — App assembles the module graph; the ViewModel definitions themselves live
    // in the feature modules.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
