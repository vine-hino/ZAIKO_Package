plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0"
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
}

compose.desktop {
    application {
        mainClass = "com.vine.pcapp.MainKt"

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