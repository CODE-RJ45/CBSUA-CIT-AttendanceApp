plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.coderj45.cbsua_cit_attendanceapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coderj45.cbsua_cit_attendanceapp"
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
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

        // 1. CameraX (The Camera) - Updated for Android 15 / 16 KB support
        val camerax_version = "1.4.1"
        implementation("androidx.camera:camera-camera2:$camerax_version")
        implementation("androidx.camera:camera-lifecycle:$camerax_version")
        implementation("androidx.camera:camera-view:$camerax_version")

        // 2. ML Kit (The Scanner) - Updated for Android 15 / 16 KB support
        implementation("com.google.mlkit:barcode-scanning:17.3.0")

        // 3. Room Database (The Storage)
        val room_version = "2.6.1"
        implementation("androidx.room:room-runtime:$room_version")
        kapt("androidx.room:room-compiler:$room_version")
        implementation("androidx.room:room-ktx:$room_version")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
