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
        versionCode = 7
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Only package arm64-v8a native libraries.
        // - minSdk 31 (Android 12) means all supported devices are 64-bit capable.
        // - The prebuilt Vosk and JNA .so files for armeabi-v7a, x86, x86_64, armeabi,
        //   mips, mips64 have only 4 KB ELF LOAD alignment (2^12), which fails
        //   Google Play's 16 KB page-size requirement.
        // - The arm64-v8a variants are compiled with 16 KB LOAD alignment (2^14+),
        //   which satisfies the requirement.
        ndk {
            abiFilters += "arm64-v8a"
        }
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
            // Store native libs uncompressed in the APK so Android can mmap them
            // directly at 16 KB-aligned offsets (required for 16 KB page-size support).
            useLegacyPackaging = false
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
