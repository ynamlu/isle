package com.isle.companion.di

import android.content.Context
import com.isle.companion.IsleApp

/**
 * 从任意 Context 获取依赖容器。
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as IsleApp).container
