package com.vine.zaiko_package

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vine.designsystem.theme.ZaikoTheme
import com.vine.zaiko_package.navigation.ZaikoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZaikoTheme {
                ZaikoNavHost()
            }
        }
    }
}