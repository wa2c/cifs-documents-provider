import org.gradle.api.JavaVersion

@Suppress("SpellCheckingInspection")
object Deps {

    const val namespaceBase = "com.wa2c.android.cifsdocumentsprovider"

    const val compileSdkVersion = 33
    const val minSdkVersion = 26
    const val targetSdkVersion = compileSdkVersion
    const val kotlinVersion = "1.8.20"
    const val javaVersion = 17
    val javaVersionEnum =  JavaVersion.VERSION_17
    const val kotlinCompilerExtensionVersion = "1.4.6"

    object App {

        const val appPlugin = "com.android.application"
        const val libraryPlugin = "com.android.library"
        const val gradlePluginClasspath = "com.android.tools.build:gradle:8.0.1"
        const val core = "androidx.core:core-ktx:1.10.0"
        const val appcompat = "androidx.appcompat:appcompat:1.6.1"

        const val kotlinAndroidPlugin = "org.jetbrains.kotlin.android"
        const val kotlinKaptPlugin = "kotlin-kapt"
        const val kotlinPluginClasspath = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

        private const val coroutineVersion = "1.6.4"
        const val coroutine = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutineVersion}"

        private const val daggerVersion = "2.45"
        const val daggerHiltPlugin = "dagger.hilt.android.plugin"
        const val daggerHiltPluginClasspath = "com.google.dagger:hilt-android-gradle-plugin:$daggerVersion"
        const val daggerHilt = "com.google.dagger:hilt-android:$daggerVersion"
        const val daggerHiltCompiler = "com.google.dagger:hilt-android-compiler:$daggerVersion"

        const val documentFile = "androidx.documentfile:documentfile:1.0.1"
    }

    object Ui {
        const val activityKtx = "androidx.activity:activity-ktx:1.7.1"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:1.5.7"
        const val material = "com.google.android.material:material:1.8.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.4"

        const val composeBom = "androidx.compose:compose-bom:2023.01.00"
        const val composeUi = "androidx.compose.ui:ui"
        const val composeMaterial = "androidx.compose.material3:material3"
        const val composeUiPreview = "androidx.compose.ui:ui-tooling-preview"
        const val composeUiTooling = "androidx.compose.ui:ui-tooling"

        private const val lifeCycleVersion = "2.6.1"
        const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifeCycleVersion"
        const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifeCycleVersion"
        const val lifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:$lifeCycleVersion"

        private const val navigationVersion = "2.5.3"
        const val navigationSafeargsPlugin = "androidx.navigation.safeargs.kotlin"
        const val navigationPluginClasspath = "androidx.navigation:navigation-safe-args-gradle-plugin:$navigationVersion"
        const val navigationUi = "androidx.navigation:navigation-ui-ktx:$navigationVersion"
        const val navigationFragmentKtx = "androidx.navigation:navigation-fragment-ktx:$navigationVersion"
    }

    object Data {
        private const val roomVersion = "2.5.1"
        const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
        const val roomKtx = "androidx.room:room-ktx:$roomVersion"
        const val roomPaging = "androidx.room:room-paging:$roomVersion"

        const val dataStore = "androidx.datastore:datastore-preferences:1.0.0"

        private const val pagingVersion = "3.1.1"
        const val pagingKtx = "androidx.paging:paging-runtime-ktx:$pagingVersion"

        const val kotlinParcelizePlugin = "kotlin-parcelize"
        const val kotlinSerializationPlugin = "kotlinx-serialization"
        const val kotlinxSerializationClasspath = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
        const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0"

        const val jcifsNg = "eu.agno3.jcifs:jcifs-ng:2.1.9"
        const val smbj = "com.hierynomus:smbj:0.11.5"
        const val smbjRpc = "com.rapid7.client:dcerpc:0.12.0"
        const val guava = "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"
        const val networkTools = "com.github.stealthcopter:AndroidNetworkTools:0.4.5.3"
    }

    object Util {
        const val timber = "com.jakewharton.timber:timber:4.7.1" // TODO: Update after supporting Kotlin static call
        const val localization = "com.akexorcist:localization:1.2.11"
        private const val licenseVersion = "10.6.3"
        const val licensePluginClassPath = "com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${licenseVersion}"
        const val licensePlugin = "com.mikepenz.aboutlibraries.plugin"
        const val license = "com.mikepenz:aboutlibraries:${licenseVersion}"
    }

    object Test {
        const val junit = "junit:junit:4.13.2"
        const val junitExt = "androidx.test.ext:junit:1.1.5"
        const val espressoCore = "androidx.test.espresso:espresso-core:3.5.1"
    }

}