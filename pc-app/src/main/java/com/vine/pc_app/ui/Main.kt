package com.vine.pc_app.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ZAIKO Package"
    ) {
        PcAppShell()
    }
}