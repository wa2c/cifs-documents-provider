plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${Deps.namespaceBase}.data.storage.smbj"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage:interfaces"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.smbj)
    implementation(libs.dcerpc)
    implementation(libs.listenablefuture) // to avoid conflict

    testImplementation(libs.junit)
}