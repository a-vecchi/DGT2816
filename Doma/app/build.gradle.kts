plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.doma"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.doma"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.wear:wear:1.3.0")
}