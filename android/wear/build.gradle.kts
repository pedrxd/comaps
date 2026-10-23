plugins {
    alias(libs.plugins.android.application)
}

val appName = "CoMaps"
val appId = "app.comaps"
// These are properly set in the 'secure.properties.release' file but must be declared here for build sync to succeed.
project.ext["secretReleaseStoreFile"] = "comaps-release.keystore"
project.ext["secretReleaseStorePassword"] = ""
project.ext["secretReleaseKeyAlias"] = "CoMaps Release"
project.ext["secretReleaseKeyPassword"] = ""

val securePropertiesReleasePath = "${project.projectDir}/../app/secure.properties.release"
val secureReleasePropertiesFileExists = File(securePropertiesReleasePath).exists()
if (secureReleasePropertiesFileExists) {
    apply(securePropertiesReleasePath)
}

android {
    namespace = "app.organicmaps.wear"
    compileSdk = providers.gradleProperty("propCompileSdkVersion").get().toInt()

    defaultConfig {
        applicationId = appId
        // Keep the Wear artifact version code unique from the phone app while preserving
        // the same base CoMaps version code for Google Play multi-form-factor uploads.
        versionCode = rootProject.ext.get("versionCode") as Int * 10 + 1
        versionName = rootProject.ext.get("versionName") as String
        minSdk = 26
        targetSdk = providers.gradleProperty("propTargetSdkVersion").get().toInt()
    }

    flavorDimensions += "default"

    productFlavors {
        create("google") {
            dimension = "default"
            versionName = "${android.defaultConfig.versionName!!}-Google"
        }
    }

    signingConfigs {
        getByName("debug") {
            // Same debug keystore as the phone app so the Wear Data Layer accepts the pairing.
            storeFile = File("${project.projectDir}/../app/comaps-debug.keystore")
            storePassword = "12345678"
            keyAlias = "$appName Debug"
            keyPassword = "12345678"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs["debug"]
            resValue("string", "app_name", "$appName Debug")
        }
        release {
            signingConfigs.create("release") {
                if (secureReleasePropertiesFileExists) {
                    println("Using RELEASE signing keys from secure.properties.release")
                    storeFile = File("${project.projectDir}/../app/${project.ext["secretReleaseStoreFile"]}")
                    storePassword = project.ext["secretReleaseStorePassword"] as String
                    keyAlias = project.ext["secretReleaseKeyAlias"] as String
                    keyPassword = project.ext["secretReleaseKeyPassword"] as String
                } else {
                    println("NO RELEASE signing keys found for :wear")
                }
            }
            signingConfig = if (secureReleasePropertiesFileExists)
                signingConfigs["release"]
            else
                signingConfigs["debug"]
            isMinifyEnabled = true
            isShrinkResources = true
            resValue("string", "app_name", appName)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "MissingTranslation"
    }
}

dependencies {
    implementation(project(":libs:wear-protocol"))

    implementation(libs.androidx.annotation)
    implementation(libs.google.gms.wearable)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}
