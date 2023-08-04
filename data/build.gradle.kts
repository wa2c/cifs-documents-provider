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

    // App

    implementation(Deps.App.core)
    implementation(Deps.App.appcompat)
    implementation(Deps.App.coroutine)
    implementation(Deps.App.daggerHilt)
    kapt(Deps.App.daggerHiltCompiler)
    implementation(Deps.App.documentFile)

    // Data

    // Room
    kapt(Deps.Data.roomCompiler)
    implementation(Deps.Data.roomKtx)
    implementation(Deps.Data.roomPaging)
    // DataStore
    implementation(Deps.Data.dataStore)
    // Json
    implementation(Deps.Data.kotlinxSerializationJson)
    // SMB
    implementation(Deps.Data.jcifsNg)
    implementation(Deps.Data.smbj)
    implementation(Deps.Data.smbjRpc)
    implementation(Deps.Data.guava) // to avoid conflict
    // Android Network Tools
    implementation(Deps.Data.networkTools)

    // Test

    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.junitExt)
}