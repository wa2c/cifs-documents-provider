plugins {
    id(Deps.App.libraryPlugin)
    id(Deps.App.kotlinAndroidPlugin)
    id(Deps.App.jarJarPlugin)
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

    implementation(libs.kotlinx.coroutines.android)

    // rename package name (avoid conflict with jcifs-ng)
    // TODO fix build dependency (copy generated jar file in lib directory until resolved)
    // jarJar(Deps.Data.jcifs)
    // implementation(fileTree(mapOf("dir" to "./build/libs", "include" to arrayOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))

    testImplementation(libs.junit)
}

//jarJar {
//    rules = mapOf(Deps.Data.jcifsJar to "jcifs.** jcifs.legacy.@1")
//}
