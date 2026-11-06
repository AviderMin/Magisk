plugins {
    alias(libs.plugins.android.application)
    kotlin("plugin.parcelize")
    alias(libs.plugins.compose.compiler)
}

setupMainApk()

android {
    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    defaultConfig {
        // Glasmio requires Android 13 (API 33)
        minSdk = 33
        proguardFile("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    coreLibraryDesugaring(libs.jdk.libs)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.window.core)
    implementation(libs.material)

    // Glasmio (iOS-style Liquid Glass Compose UI framework)
    implementation(libs.glasmio.scaffold)
    implementation(libs.glasmio.preference)
    implementation(libs.glasmio.icons)

    // Navigation3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigationevent.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
}
