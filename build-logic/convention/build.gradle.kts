plugins {
    `kotlin-dsl`
}

group = "com.example.catslist.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Applied programmatically by the convention plugins below, so they need the plugin
    // artifacts themselves on the classpath, not just an `id(...)`/`alias(...)` reference.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "catslist.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "catslist.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("kmpLibrary") {
            id = "catslist.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpAndroidLibrary") {
            id = "catslist.kmp.android.library"
            implementationClass = "KmpAndroidLibraryConventionPlugin"
        }
        register("koin") {
            id = "catslist.koin"
            implementationClass = "KoinConventionPlugin"
        }
        register("compose") {
            id = "catslist.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("quality") {
            id = "catslist.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
