plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.arcadehub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.arcadehub"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "arcadehub123"
            keyAlias = "arcadehub"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "arcadehub123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.google.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}