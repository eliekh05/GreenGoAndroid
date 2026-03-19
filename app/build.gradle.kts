plugins {
    id("com.android.application")
    // No need for 'org.jetbrains.kotlin.android' with AGP 9+
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace   = "com.greengo.app"
    compileSdk  = 36 // ✅ updated from 35

    defaultConfig {
        applicationId = "com.greengo.app"
        minSdk        = 26
        targetSdk     = 36 // ✅ updated from 35
        versionCode   = 1
        versionName   = "1.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("RELEASE_STORE_FILE")
            if (!storeFilePath.isNullOrEmpty()) {
                storeFile      = file(storeFilePath)
                storePassword  = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias       = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword    = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = false
            isShrinkResources = false
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Kotlin JVM toolchain setup (Kotlin 2.x)
    kotlin {
        jvmToolchain(11)
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.0" // ✅ must match Compose version
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00") // updated to latest
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
