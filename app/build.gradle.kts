plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.Deysdeveloper.dailyvoicejournalapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.Deysdeveloper.dailyvoicejournalapp"
        minSdk = 31
        targetSdk = 36
        versionCode = 4
        versionName = "1.1"

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
    buildFeatures {
        compose = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // useLegacyPackaging = false: .so files stored uncompressed + 16KB aligned in the APK.
            // Combined with android:extractNativeLibs="false" in manifest for 16 KB page size support.
            useLegacyPackaging = false
            pickFirsts.add("**/libjnidispatch.so")
            pickFirsts.add("**/libvosk.so")
        }
    }
    
    // Add jniLibs source set
    sourceSets["main"].jniLibs.setSrcDirs(listOf("src/main/jniLibs"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Biometric
    implementation(libs.androidx.biometric.ktx)
    
    // Fragment - required for ActivityResult API
    implementation(libs.androidx.fragment.ktx)
    
    // Vosk for on-device speech recognition
    implementation(libs.vosk.android) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    
    // JNA required by Vosk - use AAR version which includes Android native libs
    // Version 5.15.0+ has 16KB page size alignment fix
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
