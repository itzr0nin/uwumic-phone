plugins { id("com.android.application") }
android {
    namespace = "com.uwumic"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.uwumic.wo"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "3.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
