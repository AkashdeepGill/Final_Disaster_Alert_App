plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services) // Firebase plugin
}

android {
    namespace = "edu.msoe.gilla.finalapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.msoe.gilla.finalapp"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Location Services
    implementation(libs.play.services.location)
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.google.android.gms:play-services-maps:19.2.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}