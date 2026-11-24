package com.evmap.serverapp.features.auth

import com.evmap.serverapp.config.PasswordResetProperties
import com.evmap.serverapp.features.auth.application.PasswordResetCrypto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

class PasswordResetCryptoTest {
    private val crypto = PasswordResetCrypto(PasswordResetProperties(secret = Base64.getEncoder().encodeToString(ByteArray(32) { 17 })))

    @Test fun `code hash binds code to its request`() {
        val id = UUID.randomUUID(); val code = crypto.newCode(); val hash = crypto.codeHash(id, code)
        assertTrue(code.matches(Regex("[0-9]{8}")))
        assertTrue(crypto.matchesCode(id, code, hash))
        assertFalse(crypto.matchesCode(UUID.randomUUID(), code, hash))
    }
    @Test fun `mail encryption authenticates ciphertext`() {
        val encrypted = crypto.encrypt("Recovery code 12345678")
        assertEquals("Recovery code 12345678", crypto.decrypt(encrypted))
        val bytes = Base64.getDecoder().decode(encrypted)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { crypto.decrypt(Base64.getEncoder().encodeToString(bytes)) }
    }
    @Test fun `reset grants are unpredictable url safe and not stored verbatim`() {
        val token = crypto.newToken()
        assertTrue(token.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertNotEquals(token, crypto.newToken())
        assertEquals(64, crypto.tokenHash(token).length)
        assertNotEquals(token, crypto.tokenHash(token))
    }
}
