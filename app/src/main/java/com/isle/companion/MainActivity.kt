package com.isle.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.isle.companion.di.appContainer
import com.isle.companion.ui.navigation.IsleNavHost
import com.isle.companion.ui.screen.auth.AuthScreen
import com.isle.companion.ui.screen.auth.LockScreen
import com.isle.companion.ui.screen.splash.SplashScreen
import com.isle.companion.ui.theme.IsleTheme
import com.isle.companion.ui.theme.isleBackground
import kotlinx.coroutines.delay

/**
 * 应用主 Activity，承载 Compose 界面。
 *
 * 启动顺序：
 * 本地账户（未创建时先注册/游客）→ 应用锁 → 主界面。
 */
class MainActivity : ComponentActivity() {

    /** 账户存在且超过后台时限时，回到前台需要重新解锁。 */
    private var locked by mutableStateOf(false)
    private var backgroundAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locked = appContainer.localAuthRepository.hasAccount

        setContent {
            IsleTheme {
                val container = LocalContext.current.appContainer
                val account by container.localAuthRepository.account.collectAsState()
                val guest by container.localAuthRepository.isGuest.collectAsState()
                val profile by container.userProfileRepository.profile
                    .collectAsState(initial = null)
                var showSplash by rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1400)
                    showSplash = false
                }

                // 账户删除后重新创建、或游客首次进入时，确保演示数据存在。
                LaunchedEffect(account?.id, guest) {
                    if (account != null || guest) {
                        runCatching { container.demoDataSeeder.bootstrap() }
                    }
                }

                if (showSplash) {
                    SplashScreen()
                } else Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // 还没有本地账户，也没有选择游客：先注册/登录
                        account == null && !guest -> AuthScreen(
                            repository = container.localAuthRepository,
                            onAuthenticated = { locked = false },
                            onGuest = { container.localAuthRepository.continueAsGuest() }
                        )

                        // 已有账户但从后台回来：应用锁
                        account != null && locked -> LockScreen(
                            nickname = account?.nickname.orEmpty(),
                            repository = container.localAuthRepository,
                            onUnlocked = { locked = false }
                        )

                        profile == null -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .isleBackground()
                        )

                        else -> IsleNavHost(
                            onLock = { locked = true },
                            onAccountDeleted = {
                                locked = false
                            }
                        )
                    }
                }

            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && appContainer.localAuthRepository.hasAccount) {
            backgroundAt = System.currentTimeMillis()
        }
    }

    override fun onStart() {
        super.onStart()
        if (backgroundAt > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundAt
            if (elapsed >= LOCK_AFTER_MS) locked = true
            backgroundAt = 0L
        }
    }

    private companion object {
        /** 后台超过 60 秒，回到前台需要重新解锁。 */
        const val LOCK_AFTER_MS = 60_000L
    }
}
