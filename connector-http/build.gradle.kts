plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":connector-api"))
    implementation(project(":inventory-contract"))

    implementation("io.ktor:ktor-client-core:<ktor-version>")
    implementation("io.ktor:ktor-client-content-negotiation:<ktor-version>")
    implementation("io.ktor:ktor-serialization-kotlinx-json:<ktor-version>")
}