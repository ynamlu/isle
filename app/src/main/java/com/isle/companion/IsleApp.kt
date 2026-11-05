package com.isle.companion

import android.app.Application
import com.isle.companion.di.AppContainer

/**
 * 心屿（Isle）应用入口。
 *
 * 演示数据在用户创建本地账户或选择游客模式后，由 MainActivity 触发写入，
 * 避免未进入应用就写入数据，也避免启动阶段重复 seed。
 */
class IsleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
