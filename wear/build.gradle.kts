plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.healthbridge.wear"
    
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.healthbridge.wear"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // Wear OS specific
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-material:1.3.0")
    
    // Compose dependencies
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.runtime:runtime:1.6.0")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Health Services for heart rate
    implementation("androidx.health:health-services-client:1.0.0-beta04")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Data Layer (Wear OS communication)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
}

