plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
    id(Deps.App.kotlinKaptPlugin)
    id(Deps.App.daggerHiltPlugin)
    id(Deps.App.parcelizePlugin)
    id(Deps.Util.licensePlugin)
}

android {
    compileSdk = Deps.compileSdkVersion
    namespace = "${Deps.namespaceBase}.presentation"

    defaultConfig {
        minSdk = Deps.minSdkVersion

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
        sourceCompatibility = Deps.javaVersionEnum
        targetCompatibility = Deps.javaVersionEnum
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(Deps.javaVersion))
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Deps.kotlinCompilerExtensionVersion
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

    // Compose
    val composeBom = platform(Deps.Ui.composeBom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(Deps.Ui.composeUi)
    implementation(Deps.Ui.composeMaterial)
    implementation(Deps.Ui.composeUiPreview)
    debugImplementation(Deps.Ui.composeUiTooling)
    implementation(Deps.Ui.composeReorderable)
    implementation(Deps.Ui.systemUiController)
    // Lifecycle
    implementation(Deps.Ui.lifecycleViewModelCompose)
    implementation(Deps.Ui.lifecycleRuntimeCompose)
    // Navigation
    implementation(Deps.Ui.navigationCompose)
    implementation(Deps.Ui.navigationComposeHilt)

    // Util

    // OSS License
    implementation(Deps.Util.license)
    implementation(Deps.Util.licenseCompose)

    // Test

    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.junitExt)
}