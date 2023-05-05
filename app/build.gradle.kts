plugins {
    id(Deps.App.appPlugin)
    id(Deps.App.kotlinAndroidPlugin)
    id(Deps.App.kotlinKaptPlugin)
    id(Deps.App.daggerHiltPlugin)
}

android {
    compileSdk = Deps.compileSdkVersion
    compileOptions {
        sourceCompatibility = Deps.javaVersionEnum
        targetCompatibility = Deps.javaVersionEnum
    }
    namespace = Deps.namespaceBase

    defaultConfig {
        applicationId = "com.wa2c.android.cifsdocumentsprovider"
        minSdk = Deps.minSdkVersion
        targetSdk = Deps.targetSdkVersion
        versionCode = 17
        versionName = "1.7.0"

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

    buildFeatures {
        dataBinding = true
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation(project(":common"))
    implementation(project(":presentation"))
    implementation(project(":data"))

    implementation(Deps.App.daggerHilt)
    kapt(Deps.App.daggerHiltCompiler)

    // Util

    // Localization
    implementation(Deps.Util.localization)
}

kapt {
    // https://kotlinlang.org/docs/kapt.html
    correctErrorTypes = true
}
