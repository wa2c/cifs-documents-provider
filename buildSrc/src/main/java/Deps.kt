object Deps {

    const val compileSdkVersion = 31
    const val minSdkVersion = 26
    const val targetSdkVersion = compileSdkVersion
    const val kotlinVersion = "1.6.21"

    object App {
        const val core = "androidx.core:core-ktx:1.8.0"
        const val appcompat = "androidx.appcompat:appcompat:1.4.2"
        const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val gradleTool = "com.android.tools.build:gradle:7.2.1"

        private const val coroutineVersion = "1.6.0"
        const val coroutine = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutineVersion}"

        private const val daggerVersion = "2.42"
        const val daggerHiltPlugin = "com.google.dagger:hilt-android-gradle-plugin:$daggerVersion"
        const val daggerHilt = "com.google.dagger:hilt-android:$daggerVersion"
        const val daggerHiltCompiler = "com.google.dagger:hilt-android-compiler:$daggerVersion"

        const val documentFile = "androidx.documentfile:documentfile:1.0.1"
    }

    object Ui {
        const val activityKtx = "androidx.activity:activity-ktx:1.4.0"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:1.4.1"
        const val material = "com.google.android.material:material:1.6.1"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.4"

        private const val lifeCycleVersion = "2.4.1"
        const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifeCycleVersion"
        const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifeCycleVersion"

        private const val navigationVersion = "2.4.2"
        const val navigationPlugin = "androidx.navigation:navigation-safe-args-gradle-plugin:$navigationVersion"
        const val navigationUi = "androidx.navigation:navigation-ui-ktx:$navigationVersion"
        const val navigationFragmentKtx = "androidx.navigation:navigation-fragment-ktx:$navigationVersion"
    }

    object Data {
        private const val roomVersion = "2.4.2"
        const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
        const val roomKtx = "androidx.room:room-ktx:$roomVersion"
        const val roomPaging = "androidx.room:room-paging:$roomVersion"
        private const val pagingVersion = "3.1.1"
        const val pagingKtx = "androidx.paging:paging-runtime-ktx:$pagingVersion"

        const val kotlinxSerialization = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
        const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3"
        const val jcifsNg = "eu.agno3.jcifs:jcifs-ng:2.1.7"
        const val networkTools = "com.github.stealthcopter:AndroidNetworkTools:0.4.5.3"
    }

    object Util {
        const val timber = "com.jakewharton.timber:timber:4.7.1" // TODO: Update after supporting Kotlin static call
        const val localization = "com.akexorcist:localization:1.2.11"
        const val ossLicensePlugin = "com.google.android.gms:oss-licenses-plugin:0.10.5"
        const val ossLicense = "com.google.android.gms:play-services-oss-licenses:17.0.0"
    }

    object Test {
        const val junit = "junit:junit:4.13.2"
        const val junitExt = "androidx.test.ext:junit:1.1.3"
        const val espressoCore = "androidx.test.espresso:espresso-core:3.4.0"
    }

}