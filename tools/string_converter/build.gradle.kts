@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    kotlin("jvm") version "1.9.20"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.opencsv:opencsv:5.9")
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
}

/**
 * CSV-XML conversion task.
 */
val convertString by tasks.registering(JavaExec::class) {
    group = "tools"
    classpath = java.sourceSets["main"].runtimeClasspath
    mainClass.set("com.wa2c.android.cifsdocumentsprovider.tools.string_converter.MainKt")
    args(
        File(projectDir, "strings.csv").canonicalPath,
        File(projectDir, "../../presentation/src/main/res/").canonicalPath,
        "https://docs.google.com/spreadsheets/d/1y71DyM31liwjcAUuPIk3CuIqxJD2l9Y2Q-YZ0I0XE_E/export?format=csv#gid=0"
    )
}
