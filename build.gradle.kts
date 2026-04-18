plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.kapt) apply false
    alias(libs.plugins.google.dagger.hilt) apply false
    alias(libs.plugins.google.ksp) apply false

    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}