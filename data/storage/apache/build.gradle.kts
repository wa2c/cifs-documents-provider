plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val applicationId: String by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${applicationId}.data.storage.apache"

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
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage:interfaces"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.apache.commons.net)
    implementation(libs.apache.commons.vfs)
    implementation("com.jcraft:jsch:0.1.55")
//    implementation("commons-logging:commons-logging:1.3.0")
//    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("androidx.documentfile:documentfile:1.0.1")




    testImplementation(libs.junit)
}
