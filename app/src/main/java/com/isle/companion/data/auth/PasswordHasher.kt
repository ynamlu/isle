package com.isle.companion.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 本地密码哈希。使用 Android/JDK 自带的 PBKDF2WithHmacSHA256，
 * 不保存明文密码，也不依赖任何后端。
 */
object PasswordHasher {

    const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class StoredHash(
        val hash: String,
        val salt: String,
        val iterations: Int
    )

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String = newSalt(), iterations: Int = ITERATIONS): StoredHash {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            iterations,
            KEY_LENGTH_BITS
        )
        val encoded = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
        return StoredHash(
            hash = Base64.getEncoder().encodeToString(encoded),
            salt = salt,
            iterations = iterations
        )
    }

    fun verify(password: String, stored: StoredHash): Boolean = runCatching {
        val candidate = hash(password, stored.salt, stored.iterations)
        MessageDigest.isEqual(
            Base64.getDecoder().decode(candidate.hash),
            Base64.getDecoder().decode(stored.hash)
        )
    }.getOrDefault(false)

    /** 生成一次性恢复码，形如 K7Q2-9M4X-B8TW-3F6N。 */
    fun newRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val chars = CharArray(16) { alphabet[random.nextInt(alphabet.length)] }
        return chars.toList()
            .chunked(4)
            .joinToString("-") { it.joinToString("") }
    }

    fun normalizeRecoveryCode(code: String): String =
        code.uppercase().filter { it.isLetterOrDigit() }
}
