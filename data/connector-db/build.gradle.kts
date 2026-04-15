plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.kapt)
    alias(libs.plugins.google.dagger.hilt)
}

android {
    namespace = "com.vine.connector_db"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":data:connector-api"))
    implementation(project(":core:database"))

    implementation(libs.androidx.room.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}