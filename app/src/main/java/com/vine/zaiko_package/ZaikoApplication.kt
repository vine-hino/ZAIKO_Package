package com.vine.zaiko_package

import android.app.Application
import com.vine.zaiko_package.bootstrap.InitialMasterSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import com.vine.zaiko_package.BuildConfig

@HiltAndroidApp
class ZaikoApplication : Application() {

    @Inject
    lateinit var initialMasterSeeder: InitialMasterSeeder

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            if (BuildConfig.DEBUG && BuildConfig.ENABLE_LOCAL_MASTER_SEED) {
                initialMasterSeeder.seedIfNeeded()
            }
        }
    }
}