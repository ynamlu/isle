package com.isle.companion.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class LocalAccount(
    val id: String,
    val nickname: String,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordIterations: Int,
    val recoveryHash: String,
    val recoverySalt: String,
    val recoveryIterations: Int,
    val createdAt: Long,
    val lastLoginAt: Long
) {
    val passwordStored: PasswordHasher.StoredHash
        get() = PasswordHasher.StoredHash(passwordHash, passwordSalt, passwordIterations)

    val recoveryStored: PasswordHasher.StoredHash
        get() = PasswordHasher.StoredHash(recoveryHash, recoverySalt, recoveryIterations)
}
