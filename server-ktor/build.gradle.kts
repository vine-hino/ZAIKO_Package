plugins {
    kotlin("jvm")
    application
}

val ktorVersion = "2.3.9"
val logbackVersion = "1.5.6"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:inventory-contract"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}