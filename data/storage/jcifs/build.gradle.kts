import ru.tinkoff.gradle.jarjar.task.JarJarTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id(libs.plugins.jarjar.get().pluginId)
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
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
    }

}

dependencies {
    implementation(project(":common"))
    implementation(project(":data:storage:interfaces"))

    implementation(libs.kotlinx.coroutines.android)

    // JCIFS relocation
    jarJar(libs.jcifs.legacy)
    implementation(fileTree(mapOf("dir" to "build/libs", "include" to arrayOf("*.jar"))))

    testImplementation(libs.junit)
}

// relocate JCIFS
tasks.withType<JarJarTask> {
    jarJar.jarJarDependency = libs.jcifs.legacy.get().toString()
    jarJar.rules = mapOf(libs.jcifs.legacy.get().let { "${it.name}-${it.version}.jar" } to "jcifs.** jcifs.legacy.@1")
}

tasks.configureEach {
    if (name == "copyDebugJniLibsProjectAndLocalJars" ||
        name == "copyReleaseJniLibsProjectAndLocalJars") {
        dependsOn("preJarJar")
    }
}
