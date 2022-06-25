plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("androidx.navigation.safeargs.kotlin")
    id("dagger.hilt.android.plugin")
    //id("com.google.android.gms.oss-licenses-plugin")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 26
        targetSdk = 32

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        dataBinding = true
    }

}

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))

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

    // Util

    // Localization
    implementation(Deps.Util.localization)
    // OSS License
    implementation(Deps.Util.ossLicense)

//    implementation("androidx.core:core-ktx:1.7.0")
//    implementation("androidx.appcompat:appcompat:1.4.2")
//    implementation("com.google.android.material:material:1.6.1")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.3")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}