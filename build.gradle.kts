// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    //kotlin("kapt") version "1.9.10"
    alias(libs.plugins.kapt)
    //id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
}

buildscript {

    repositories {
        google()
        mavenCentral()
        maven(url="https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.jarjar)

        //classpath(libs.kotlin.serialization)
        //classpath(libs.aboutlibraries.plugin)

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
