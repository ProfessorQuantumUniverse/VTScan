plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.quantum_prof.vtscansuite"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.quantum_prof.vtscansuite"
        minSdk = 30
        targetSdk = 36
        versionCode = 5
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // Wird für BuildConfig.DEBUG gebraucht (HTTP-Logging nur im Debug-Build).
        buildConfig = true
    }

    dependenciesInfo {
        // Deaktiviert die Metadaten für Abhängigkeiten in der APK
        includeInApk = false
        // Deaktiviert die Metadaten für Abhängigkeiten im App Bundle (AAB)
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        // Opt-in auf das künftige Standardverhalten (Annotation gilt für Param + Feld), KT-73255
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Integration with Activities
    implementation("androidx.activity:activity-compose:1.13.0")

    // Tooling support (debug)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ... other dependencies
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    // Falls du zusätzlich Proto DataStore nutzt:
    implementation("androidx.datastore:datastore:1.2.1")
}