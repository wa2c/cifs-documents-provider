plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
    id(Deps.App.kotlinKaptPlugin)
    id(Deps.App.daggerHiltPlugin)
    id(Deps.Data.kotlinParcelizePlugin)
    id(Deps.Data.kotlinSerializationPlugin)
}

android {
    compileSdk = Deps.compileSdkVersion
    namespace = "${Deps.namespaceBase}.data"

    defaultConfig {
        minSdk = Deps.minSdkVersion

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