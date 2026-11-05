package com.isle.companion.data.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

sealed interface AuthResult {
    data class Success(
        val account: LocalAccount,
        val recoveryCode: String? = null
    ) : AuthResult

    data class Failure(val message: String) : AuthResult
    data class Locked(val seconds: Int) : AuthResult
}

/**
 * 本地匿名账户：不联网、不收集邮箱/手机号，只做本机身份与数据隔离。
 *
 * 注意：这不是实名/学籍核验。它验证的是“这台设备上的账户密码/恢复码”，
 * 无法验证邮箱、手机号或学生身份。
 */
class LocalAuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secureStore = SecureStore()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _account = MutableStateFlow(loadAccount())
    val account: StateFlow<LocalAccount?> = _account.asStateFlow()

    private val _isGuest = MutableStateFlow(prefs.getBoolean(KEY_GUEST, false) && _account.value == null)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    val hasAccount: Boolean get() = _account.value != null

    suspend fun createAccount(nickname: String, password: String): AuthResult =
        withContext(Dispatchers.Default) {
            val name = nickname.trim()
            if (name.isBlank()) return@withContext AuthResult.Failure("先给这个账户起一个昵称")
            if (password.length < 6) return@withContext AuthResult.Failure("密码至少 6 位")

            val passwordHash = PasswordHasher.hash(password)
            val recoveryCode = PasswordHasher.newRecoveryCode()
            val recoveryHash = PasswordHasher.hash(normalizeRecovery(recoveryCode))
            val now = System.currentTimeMillis()
            val account = LocalAccount(
                id = UUID.randomUUID().toString(),
                nickname = name,
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                passwordIterations = passwordHash.iterations,
                recoveryHash = recoveryHash.hash,
                recoverySalt = recoveryHash.salt,
                recoveryIterations = recoveryHash.iterations,
                createdAt = now,
                lastLoginAt = now
            )
            if (!runCatching { saveAccount(account) }.isSuccess) {
                return@withContext AuthResult.Failure("本地安全存储初始化失败，请重启应用后重试")
            }
            prefs.edit().putBoolean(KEY_GUEST, false).apply()
            _account.value = account
            _isGuest.value = false
            AuthResult.Success(account, recoveryCode)
        }

    suspend fun login(password: String): AuthResult = withContext(Dispatchers.Default) {
        val account = _account.value ?: return@withContext AuthResult.Failure("还没有本地账户")
        val remaining = prefs.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis()
        if (remaining > 0) {
            return@withContext AuthResult.Locked(((remaining + 999) / 1000).toInt())
        }
        if (!PasswordHasher.verify(password, account.passwordStored)) {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
            if (attempts >= MAX_ATTEMPTS) {
                val lockSeconds = minOf(300, 30 * (attempts - MAX_ATTEMPTS + 1))
                prefs.edit()
                    .putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + lockSeconds * 1000L)
                    .apply()
                return@withContext AuthResult.Locked(lockSeconds)
            }
            return@withContext AuthResult.Failure("密码不对，还可以再试 ${MAX_ATTEMPTS - attempts} 次")
        }
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
        val updated = account.copy(lastLoginAt = System.currentTimeMillis())
        if (!runCatching { saveAccount(updated) }.isSuccess) {
            return@withContext AuthResult.Failure("本地安全存储初始化失败，请重启应用后重试")
        }
        _account.value = updated
        _isGuest.value = false
        AuthResult.Success(updated)
    }

    suspend fun resetWithRecovery(code: String, newPassword: String): AuthResult =
        withContext(Dispatchers.Default) {
            val account = _account.value ?: return@withContext AuthResult.Failure("还没有本地账户")
            if (newPassword.length < 6) return@withContext AuthResult.Failure("新密码至少 6 位")
            val normalized = normalizeRecovery(code)
            if (!PasswordHasher.verify(normalized, account.recoveryStored)) {
                return@withContext AuthResult.Failure("恢复码不对，请检查有没有抄错")
            }
            val passwordHash = PasswordHasher.hash(newPassword)
            val newRecoveryCode = PasswordHasher.newRecoveryCode()
            val recoveryHash = PasswordHasher.hash(normalizeRecovery(newRecoveryCode))
            val updated = account.copy(
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                passwordIterations = passwordHash.iterations,
                recoveryHash = recoveryHash.hash,
                recoverySalt = recoveryHash.salt,
                recoveryIterations = recoveryHash.iterations,
                lastLoginAt = System.currentTimeMillis()
            )
            if (!runCatching { saveAccount(updated) }.isSuccess) {
                return@withContext AuthResult.Failure("本地安全存储初始化失败，请重启应用后重试")
            }
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .remove(KEY_LOCKED_UNTIL)
                .apply()
            _account.value = updated
            AuthResult.Success(updated, newRecoveryCode)
        }

    /** 用当前密码验证后重新生成恢复码；旧恢复码立即失效。 */
    suspend fun regenerateRecoveryCode(password: String): AuthResult =
        withContext(Dispatchers.Default) {
            val account = _account.value ?: return@withContext AuthResult.Failure("还没有本地账户")
            if (!PasswordHasher.verify(password, account.passwordStored)) {
                return@withContext AuthResult.Failure("密码不对，无法重新生成恢复码")
            }
            val recoveryCode = PasswordHasher.newRecoveryCode()
            val recoveryHash = PasswordHasher.hash(normalizeRecovery(recoveryCode))
            val updated = account.copy(
                recoveryHash = recoveryHash.hash,
                recoverySalt = recoveryHash.salt,
                recoveryIterations = recoveryHash.iterations
            )
            if (!runCatching { saveAccount(updated) }.isSuccess) {
                return@withContext AuthResult.Failure("本地安全存储初始化失败，请重启应用后重试")
            }
            _account.value = updated
            AuthResult.Success(updated, recoveryCode)
        }

    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult =
        withContext(Dispatchers.Default) {
            val account = _account.value ?: return@withContext AuthResult.Failure("还没有本地账户")
            if (!PasswordHasher.verify(oldPassword, account.passwordStored)) {
                return@withContext AuthResult.Failure("旧密码不对")
            }
            if (newPassword.length < 6) return@withContext AuthResult.Failure("新密码至少 6 位")
            val passwordHash = PasswordHasher.hash(newPassword)
            val updated = account.copy(
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                passwordIterations = passwordHash.iterations
            )
            if (!runCatching { saveAccount(updated) }.isSuccess) {
                return@withContext AuthResult.Failure("本地安全存储初始化失败，请重启应用后重试")
            }
            _account.value = updated
            AuthResult.Success(updated)
        }

    fun continueAsGuest() {
        prefs.edit().putBoolean(KEY_GUEST, true).apply()
        _isGuest.value = true
    }

    fun leaveGuest() {
        prefs.edit().putBoolean(KEY_GUEST, false).apply()
        _isGuest.value = false
    }

    /** 删除本地账户；调用方负责同时清除聊天、记忆和图片。 */
    fun deleteAccount() {
        prefs.edit().clear().apply()
        _account.value = null
        _isGuest.value = false
    }

    private fun loadAccount(): LocalAccount? = runCatching {
        val encrypted = prefs.getString(KEY_ACCOUNT, null) ?: return null
        json.decodeFromString<LocalAccount>(secureStore.decrypt(encrypted))
    }.getOrNull()

    private fun saveAccount(account: LocalAccount) {
        val encrypted = secureStore.encrypt(json.encodeToString(LocalAccount.serializer(), account))
        prefs.edit().putString(KEY_ACCOUNT, encrypted).apply()
    }

    private fun normalizeRecovery(code: String): String =
        PasswordHasher.normalizeRecoveryCode(code)

    private companion object {
        const val PREFS = "isle_local_auth"
        const val KEY_ACCOUNT = "account"
        const val KEY_GUEST = "guest_mode"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
        const val MAX_ATTEMPTS = 5
    }
}
