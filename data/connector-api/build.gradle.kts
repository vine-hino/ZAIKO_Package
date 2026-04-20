plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:inventory-contract"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}