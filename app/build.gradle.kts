plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("androidx.navigation.safeargs.kotlin")
    id("dagger.hilt.android.plugin")
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    compileSdk = Deps.compileSdkVersion
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "com.wa2c.android.cifsdocumentsprovider"
        minSdk = Deps.minSdkVersion
        targetSdk = Deps.targetSdkVersion
        versionCode = 13
        versionName = "1.5.0"

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

    buildFeatures {
        dataBinding = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":presentation"))
    implementation(project(":data"))

    implementation(Deps.App.daggerHilt)
    kapt(Deps.App.daggerHiltCompiler)


    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))

    // App

    implementation(Deps.App.core)
    implementation(Deps.App.appcompat)
    implementation(Deps.App.coroutine)
    implementation(Deps.App.daggerHilt)
    kapt(Deps.App.daggerHiltCompiler)

    // UI

    implementation(Deps.Ui.constraintLayout)
    implementation(Deps.Ui.material)
    implementation(Deps.Ui.activityKtx)
    implementation(Deps.Ui.fragmentKtx)
    // Lifecycle
    implementation(Deps.Ui.lifecycleViewModel )
    implementation(Deps.Ui.lifecycleRuntime)
    // Navigation
    implementation(Deps.Ui.navigationUi)
    implementation(Deps.Ui.navigationFragmentKtx)

    // Data

    // Json
    implementation(Deps.Data.kotlinxSerializationJson)
    // jCIFS-ng
    implementation(Deps.Data.jcifsNg)
    // Android Network Tools
    implementation(Deps.Data.networkTools)

    // Util

    // Timber
    implementation(Deps.Util.timber)
    // Localization
    implementation(Deps.Util.localization)
    // OSS License
    implementation(Deps.Util.ossLicense)

    // Test

    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.junitExt)
    androidTestImplementation(Deps.Test.espressoCore)
}
