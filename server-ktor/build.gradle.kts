plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":inventory-contract"))
    implementation(project(":usecase-export-inventory"))
    implementation(project(":usecase-import-inventory"))

    implementation("io.ktor:ktor-server-core:<ktor-version>")
    implementation("io.ktor:ktor-server-netty:<ktor-version>")
    implementation("io.ktor:ktor-server-content-negotiation:<ktor-version>")
    implementation("io.ktor:ktor-serialization-kotlinx-json:<ktor-version>")
}