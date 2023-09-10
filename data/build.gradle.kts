plugins {
    id(Deps.App.libraryPlugin)
    //alias(libs.plugins.android.application) apply false
    id(Deps.App.kotlinAndroidPlugin)
    //id(Deps.App.kotlinKaptPlugin)
    //kotlin("kapt") version "1.9.10"
    //id("org.jetbrains.kotlin.kapt") version "1.8.20"
    alias(libs.plugins.kapt)
    //alias(libs.plugins.kapt)
    //alias(libs.plugins.)
//    id("dagger.hilt.android.plugin")
    //id("com.google.dagger.hilt.android") version "2.44" apply false
    //alias(libs.plugins.hilt.android) apply false
    id(Deps.App.daggerHiltPlugin)
    alias(libs.plugins.parcelize)
    //id(Deps.Data.kotlinParcelizePlugin)
    //id(Deps.Data.kotlinSerializationPlugin)
    //kotlin("jvm") version "1.9.0"
    //kotlin("plugin.serialization") version "1.9.0"
    //`kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
    //id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${Deps.namespaceBase}.data"

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "K", "\"com.wa2c.android\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage"))
    implementation(project(":data:storage:interfaces"))

    // App

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.documentfile)

    // Data

    // Room
    implementation(libs.androidx.room.runtime)
    //annotationProcessor(libs.androidx.room.compiler)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // Json
    implementation(libs.kotlinx.serialization.json)
    // Android Network Tools
    implementation(libs.androidnetworktools)

    // Test

    testImplementation(libs.junit)
}