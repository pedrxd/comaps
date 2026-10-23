plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.organicmaps.wear.protocol"
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
    testImplementation(libs.junit)
}
