plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)

    implementation(project(":shared:inventory-contract"))
    implementation(project(":pc-data-postgres"))

    implementation("io.ktor:ktor-client-core:2.3.9")
    implementation("io.ktor:ktor-client-cio:2.3.9")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-client-websockets:2.3.9")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")
}

compose.desktop {
    application {
        mainClass = "com.vine.pc_app.ui.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "ZaikoPcApp"
            packageVersion = "0.1.0"
        }
    }
}