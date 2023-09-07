// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    repositories {
        google()
        mavenCentral()
        maven(url="https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(Deps.App.gradlePluginClasspath)
        classpath(Deps.App.kotlinPluginClasspath)
        classpath(Deps.App.daggerHiltPluginClasspath)
        classpath(Deps.App.jarJarClaspath)
        classpath(Deps.Ui.navigationPluginClasspath)
        classpath(Deps.Data.kotlinxSerializationClasspath)
        classpath(Deps.Util.licensePluginClassPath)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

task("clean", Delete::class) {
    delete = setOf(rootProject.buildDir)
}
