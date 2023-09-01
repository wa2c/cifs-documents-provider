plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
}

android {
    compileSdk = Deps.compileSdkVersion
    namespace = "${Deps.namespaceBase}.data.storage.jcifs"

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
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage:interfaces"))

    implementation(Deps.App.coroutine)
    implementation(Deps.Data.jcifs)

    testImplementation(Deps.Test.junit)
}