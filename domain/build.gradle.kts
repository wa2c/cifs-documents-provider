plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val applicationId: String by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${applicationId}.domain"

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
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(project(":common"))
    implementation(project(":data:data"))
    implementation(project(":data:storage:interfaces"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    // Json
    implementation(libs.kotlinx.serialization.json)

    // Test
    testImplementation(libs.junit)

}
