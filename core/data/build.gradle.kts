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

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        // OkHttp is JVM-only, so the client the engine and Coil share (ADR-0030) is built
        // here rather than in commonMain; iOS drives URLSession through the Darwin engine.
        named("jvmAndAndroidMain").dependencies {
            implementation(libs.okhttp)
            implementation(libs.ktor.client.okhttp)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }

        // JUnit4 and Truth are JVM-only, so the existing suite lives in the per-target test
        // source sets rather than commonTest (ADR-0028).
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

        // The real database and ErrorMapper on iOS, the way jvmTest checks them on desktop.
        iosTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
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
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
