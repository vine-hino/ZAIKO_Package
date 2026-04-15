package com.vine.zaiko_package

import android.app.Application
import com.vine.zaiko_package.bootstrap.InitialMasterSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class ZaikoApplication : Application() {

    @Inject
    lateinit var initialMasterSeeder: InitialMasterSeeder

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            initialMasterSeeder.seedIfNeeded()
        }
    }
}