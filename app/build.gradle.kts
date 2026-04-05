import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "dev.mizzenmast.letta"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.mizzenmast.letta"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api.letta.mizzenmast.dev/\"")
            buildConfigField("String", "WS_URL", "\"wss://api.letta.mizzenmast.dev/ws\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.letta.mizzenmast.dev/\"")
            buildConfigField("String", "WS_URL", "\"wss://api.letta.mizzenmast.dev/ws\"")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.services.auth.api.phone)
    debugImplementation(libs.androidx.ui.tooling)

    // SMS Retriever
//    implementation(libs.play.services.auth.phone)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore + Security
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.messaging)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Media playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}