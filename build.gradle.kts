// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(Deps.App.gradleTool)
        classpath(Deps.App.kotlinPlugin)
        classpath(Deps.App.daggerHiltPlugin)
        classpath(Deps.Ui.navigationPlugin)
        classpath(Deps.Data.kotlinxSerialization)
        classpath(Deps.Util.ossLicensePlugin)
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
