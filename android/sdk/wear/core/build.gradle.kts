plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.organicmaps.sdk.wear"
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
    // WearMapState is part of this module's public API (see WearMapPublisher),
    // so expose wear-protocol transitively to consumers.
    api(project(":libs:wear-protocol"))

    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
}
