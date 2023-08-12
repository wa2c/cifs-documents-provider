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
        applicationId = Deps.namespaceBase
        minSdk = Deps.minSdkVersion
        targetSdk = Deps.targetSdkVersion
        versionCode = 19
        versionName = "1.8.0-beta2"

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

    implementation(Deps.App.appcompat)
    implementation(Deps.App.daggerHilt)
    kapt(Deps.App.daggerHiltCompiler)
}

kapt {
    // https://kotlinlang.org/docs/kapt.html
    correctErrorTypes = true
}
