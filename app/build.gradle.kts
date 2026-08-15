plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}




android {
    namespace = "com.example.smarthome"
    compileSdk= 37
//    compileSdk {
//        version = release(37) {
//            minorApiLevel = 1
//        }
//    }

    defaultConfig {
        applicationId = "com.example.smarthome"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))

    // Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Optional (good for your assignment)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


}
