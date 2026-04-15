plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:inventory-contract"))
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("com.google.code.gson:gson:2.10.1")
}