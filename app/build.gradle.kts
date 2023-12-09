plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt.android)
}

val applicationId: String by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = applicationId
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    defaultConfig {
        applicationId = applicationId
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidCompileSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

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

    signingConfigs  {
        getByName("debug") {
            storeFile = file("$rootDir/debug.keystore")
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation(project(":common"))
    implementation(project(":presentation"))
    implementation(project(":domain"))
    implementation(project(":data:data"))


    implementation(libs.androidx.appcompat)
    implementation(libs.hilt.android)
    implementation(libs.androidx.work.runtime)
    kapt(libs.hilt.android.compiler)
}

kapt {
    // https://kotlinlang.org/docs/kapt.html
    correctErrorTypes = true
}

tasks.configureEach {
    if (name == "collectDebugDependencies" ||
        name == "collectReleaseDependencies") {
        dependsOn(":data:storage:jcifs:preJarJar")
    }
}
