plugins {
    id("com.android.application") version "8.7.0"
    id("org.jetbrains.kotlin.android") version "2.0.0"
}

android {
    namespace = "com.maya.gurugram"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maya.gurugram"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }
}
