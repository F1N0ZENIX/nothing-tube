plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.f1n0.nothingtube"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.f1n0.nothingtube"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
