plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.gamer631.crashtracker"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gamer631.crashtracker"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
