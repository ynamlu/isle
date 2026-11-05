package com.isle.companion.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun correctPasswordVerifies() {
        val stored = PasswordHasher.hash("isle-2026")
        assertTrue(PasswordHasher.verify("isle-2026", stored))
        assertFalse(PasswordHasher.verify("wrong-password", stored))
    }

    @Test
    fun samePasswordUsesDifferentSalt() {
        val first = PasswordHasher.hash("same-password")
        val second = PasswordHasher.hash("same-password")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @Test
    fun recoveryCodeHasReadableFormat() {
        val code = PasswordHasher.newRecoveryCode()
        assertTrue(code.matches(Regex("[A-Z2-9]{4}(-[A-Z2-9]{4}){3}")))
        assertTrue(PasswordHasher.normalizeRecoveryCode(code).length == 16)
    }
}
