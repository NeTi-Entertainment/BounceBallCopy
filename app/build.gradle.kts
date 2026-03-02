// ═══════════════════════════════════════════════════════
// BUILD.GRADLE.KTS CORRIGÉ — 3 bugs de crash supprimés
// ═══════════════════════════════════════════════════════
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bounceball"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bounceball"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    // Google Ads (AdMob) — une seule ligne suffit
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    // Google Play Billing (achats in-app)
    implementation("com.android.billingclient:billing:7.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}