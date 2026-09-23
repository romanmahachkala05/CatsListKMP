plugins {
    id("catslist.kmp.android.library")
    id("catslist.koin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

kotlin {
    android {
        namespace = "com.example.catslist.core.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            // `api`: CatRepository, ImageDownloader and the use cases are this module's public
            // surface as far as every consumer is concerned.
            api(project(":core:domain"))

            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Not Ktor's engine dependency — the OkHttpClient the engine and Coil share
            // (ADR-0030). `jvm()`/`androidTarget()` both resolve it; see CatOkHttpClient.kt.
            implementation(libs.okhttp)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            // OkHttp on both platforms; only its construction is per-platform.
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }

        // JUnit4 and Truth are JVM-only, so the existing suite lives in the per-target test
        // source sets rather than commonTest. `:core:testing` is an Android library, which
        // is the other reason these cannot be common yet (ADR-0028).
        androidHostTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.junit)
            implementation(libs.truth)
            implementation(libs.androidx.paging.testing)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.truth)
            implementation(libs.androidx.paging.testing)
        }

        androidDeviceTest.dependencies {
            // FakeNetworkMonitor, so an instrumented test can build a real ErrorMapper.
            implementation(project(":core:testing"))
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
            implementation(libs.androidx.room.testing)
            implementation(libs.androidx.paging.testing)
        }
    }
}

room {
    // Committed history of every schema version, checked by Room at compile
    // time against each Migration and usable by MigrationTestHelper.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room's processor runs once per target that compiles the database.
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}
