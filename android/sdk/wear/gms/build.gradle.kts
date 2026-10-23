plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.organicmaps.sdk.wear.gms"
    compileSdk = providers.gradleProperty("propCompileSdkVersion").get().toInt()

    defaultConfig {
        minSdk = providers.gradleProperty("propMinSdkVersion").get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":sdk:wear:core"))

    implementation(libs.androidx.annotation)
    implementation(libs.google.gms.wearable)
}
