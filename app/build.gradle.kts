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
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

        // 1. CameraX (The Camera)
        val camerax_version = "1.3.0"
        implementation("androidx.camera:camera-camera2:$camerax_version")
        implementation("androidx.camera:camera-lifecycle:$camerax_version")
        implementation("androidx.camera:camera-view:$camerax_version")

        // 2. ML Kit (The Scanner)
        implementation("com.google.mlkit:barcode-scanning:17.2.0")

        // 3. Room Database (The Storage)
        val room_version = "2.6.1"
        implementation("androidx.room:room-runtime:$room_version")
        kapt("androidx.room:room-compiler:$room_version") // You might need to add 'id("kotlin-kapt")' in plugins at the top
        implementation("androidx.room:room-ktx:$room_version") // For Coroutines suppor
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
