plugins {
    id("com.android.library")
    id(Deps.App.kotlinAndroidPlugin)
    //id(Deps.App.kotlinKaptPlugin)
    //kotlin("kapt") version "1.9.10"
    alias(libs.plugins.kapt)
    id(Deps.App.daggerHiltPlugin)
    //alias(libs.plugins.hilt.android)
    //id("com.google.dagger.hilt.android") version "2.44" apply false
    id(Deps.App.parcelizePlugin)
    //id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.20-Beta"
    //alias(libs.plugins.parcelize)
    alias(libs.plugins.aboutlibraries)

}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${Deps.namespaceBase}.presentation"

        defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = Deps.javaVersionEnum
        targetCompatibility = Deps.javaVersionEnum
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(Deps.javaVersion))
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Deps.kotlinCompilerExtensionVersion
    }

}

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))

    // App

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)


    // UI

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.reorderable)
    implementation(libs.accompanist.systemuicontroller)
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Util

    // OSS License
    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.compose)

    // Test

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}