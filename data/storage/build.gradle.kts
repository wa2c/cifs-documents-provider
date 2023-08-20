plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
}

android {
    compileSdk = Deps.compileSdkVersion
    namespace = "${Deps.namespaceBase}.data.storage"

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
    implementation(project(":data:storage:jcifsng"))
    implementation(project(":data:storage:smbj"))

    implementation(Deps.App.core)
    implementation(Deps.App.appcompat)
    implementation(Deps.App.coroutine)

    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.junitExt)
}