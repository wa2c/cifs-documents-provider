// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Global Definition
val applicationId by extra ("com.wa2c.android.cifsdocumentsprovider")
val javaVersion by extra (JavaVersion.VERSION_21)

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.hilt.android) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

task("clean", Delete::class) {
    delete = setOf(rootProject.buildDir)
}

tasks.register("convertString") {
    dependsOn(gradle.includedBuild("string_converter").task(":convertString"))
}
