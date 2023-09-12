plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val applicationId: String by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${applicationId}.data.storage.jcifs"

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
            languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        }
    }

}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage:interfaces"))

    implementation(libs.kotlinx.coroutines.android)
    // Used package renamed JCIFS jar file (TODO: change package name from original library on build. (e.g. jarjar, shadowJar))
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))

    testImplementation(libs.junit)
}
