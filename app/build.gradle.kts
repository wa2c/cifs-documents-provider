plugins {
    id(Deps.App.appPlugin)
    id(Deps.App.kotlinAndroidPlugin)
    //id(Deps.App.kotlinKaptPlugin)
    //kotlin("kapt") version "1.9.10"
    alias(libs.plugins.kapt)
    id(Deps.App.daggerHiltPlugin)
    //alias(libs.plugins.hilt.android)
    //id("com.google.dagger.hilt.android") version "2.44" apply false
}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = Deps.javaVersionEnum
        targetCompatibility = Deps.javaVersionEnum
    }
    namespace = Deps.namespaceBase

    defaultConfig {
        applicationId = Deps.namespaceBase
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidCompileSdk.get().toInt()
        versionCode = 20
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        base.archivesName.set("CIFSDocumentsProvider-${versionName}")
    }

    buildTypes {
        debug {
            versionNameSuffix = "D"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(Deps.javaVersion))
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation(project(":common"))
    implementation(project(":presentation"))
    implementation(project(":data"))

    implementation(libs.androidx.appcompat)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
}

kapt {
    // https://kotlinlang.org/docs/kapt.html
    correctErrorTypes = true
}
