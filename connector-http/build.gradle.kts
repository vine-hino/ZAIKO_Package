plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":data:connector-api"))
    implementation(project(":shared:inventory-contract"))

    implementation("io.ktor:ktor-client-core:<ktor-version>")
    implementation("io.ktor:ktor-client-content-negotiation:<ktor-version>")
    implementation("io.ktor:ktor-serialization-kotlinx-json:<ktor-version>")
}