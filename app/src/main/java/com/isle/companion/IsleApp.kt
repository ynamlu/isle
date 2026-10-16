package com.isle.companion

import android.app.Application
import com.isle.companion.di.AppContainer

/**
 * 心屿（Isle）应用入口。
 */
class IsleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
