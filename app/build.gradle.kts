plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.tiksave.downloader"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.tiksave.downloader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
