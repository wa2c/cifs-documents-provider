plugins {
   id("com.android.application")
   id("kotlin-android")
   id("kotlin-kapt")
   id("kotlin-parcelize")
   id("kotlinx-serialization")
   id("androidx.navigation.safeargs.kotlin")
   id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 31
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "com.wa2c.android.cifsdocumentsprovider"
        minSdk = 26
        targetSdk = 31
        versionCode = 12
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "K", "\"com.wa2c.android\"")

        base.archivesBaseName = "CIFSDocumentsProvider-${versionName}"
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

object versions {
    const val kotlin = "1.6.21"
    const val coroutine = "1.6.0"
    const val lifecycle = "2.4.1"
    const val navigation = "2.4.2"
    const val dagger_hilt = "2.42"
}


dependencies {


    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${versions.lifecycle}")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:${versions.navigation}")
    implementation("androidx.navigation:navigation-ui-ktx:${versions.navigation}")

    // Dagger
    implementation("com.google.dagger:hilt-android:${versions.dagger_hilt}")
    kapt("com.google.dagger:hilt-android-compiler:${versions.dagger_hilt}")

    // Connection
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.7")
    // Android Network Tools
    implementation("com.github.stealthcopter:AndroidNetworkTools:0.4.5.3")
    // Timber
    implementation("com.jakewharton.timber:timber:4.7.1")
    // TODO: Update after supporting Kotlin static call

    // Test
    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
