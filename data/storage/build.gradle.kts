plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    namespace = "${Deps.namespaceBase}.data.storage"

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
    implementation(project(":data:storage:jcifs"))
    implementation(project(":data:storage:jcifsng"))
    implementation(project(":data:storage:smbj"))

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}