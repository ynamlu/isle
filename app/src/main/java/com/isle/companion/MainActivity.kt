package com.isle.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.isle.companion.di.appContainer
import com.isle.companion.ui.navigation.IsleNavHost
import com.isle.companion.ui.screen.onguard.AgeGateScreen
import com.isle.companion.ui.theme.IsleTheme
import com.isle.companion.ui.theme.isleBackground
import kotlinx.coroutines.launch

/**
 * 应用主 Activity，承载 Compose 界面。
 * 首次启动需先完成年龄确认（年龄门），未满 18 周岁将进入青少年模式。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsleTheme {
                val container = LocalContext.current.appContainer
                // 先区分「尚未加载」与「未确认」，避免冷启动时年龄门闪一下
                val profile by container.userProfileRepository.profile
                    .collectAsState(initial = null)
                val scope = rememberCoroutineScope()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val current = profile
                    when {
                        current == null -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .isleBackground()
                        )

                        !current.isAgeConfirmed -> AgeGateScreen(
                            onConfirm = { isMinor ->
                                scope.launch {
                                    container.userProfileRepository.setAgeGroup(
                                        if (isMinor) "minor" else "adult"
                                    )
                                }
                            }
                        )

                        else -> IsleNavHost()
                    }
                }
            }
        }
    }
}
