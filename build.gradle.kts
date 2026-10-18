plugins {
    id("com.android.application") version "8.7.0"
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maya.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maya.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
