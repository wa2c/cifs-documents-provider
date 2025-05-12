import org.gradle.kotlin.dsl.extra

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Global Definition
val applicationId by extra ("com.wa2c.android.cifsdocumentsprovider")
val javaVersion by extra (JavaVersion.VERSION_21)
val androidCompileSdk by extra (35)
val androidMinSdk by extra (26)
val appVersionName by extra ("2.4.0")
val appVersionCode by extra (31)

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
    alias(libs.plugins.room) apply false
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
    // Spreadsheet URL
    System.setProperty(
        "string_converter_spreadsheet_url",
        "https://docs.google.com/spreadsheets/d/1y71DyM31liwjcAUuPIk3CuIqxJD2l9Y2Q-YZ0I0XE_E/export?format=csv#gid=0"
    )
    // res folder path
    System.setProperty(
        "string_converter_res_path",
        "../../presentation/src/main/res/"
    )
    // res file name
    System.setProperty(
        "string_converter_res_name",
        "strings"
    )
    dependsOn(gradle.includedBuild("string_converter").task(":convertString"))
}
