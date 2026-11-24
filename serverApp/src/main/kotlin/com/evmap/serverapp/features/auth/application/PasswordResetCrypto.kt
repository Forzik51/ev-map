package com.evmap.serverapp.features.auth.application

import com.evmap.serverapp.config.PasswordResetProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class PasswordResetCrypto(properties: PasswordResetProperties) {
    private val random = SecureRandom()
    private val key = if (properties.secret.isBlank()) {
        LoggerFactory.getLogger(javaClass).warn(
            "APP_PASSWORD_RESET_SECRET is unset: using an ephemeral development key. Pending recovery requests will not survive a restart.",
        )
        ByteArray(32).also(random::nextBytes)
    } else {
        Base64.getDecoder().decode(properties.secret).also {
            require(it.size == 32) { "APP_PASSWORD_RESET_SECRET must be a Base64-encoded 32-byte random key" }
        }
    }
    val keyId: String = sha256(key)
    private val encryptionKey = SecretKeySpec(hmac("mail-encryption"), "AES")

    fun newCode(): String = random.nextInt(100_000_000).toString().padStart(8, '0')
    fun newToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
    fun codeHash(id: UUID, code: String): String = hmac("code:$id:$code").toHex()
    fun matchesCode(id: UUID, code: String, expected: String): Boolean =
        MessageDigest.isEqual(codeHash(id, code).toByteArray(Charsets.US_ASCII), expected.toByteArray(Charsets.US_ASCII))
    fun identifierHash(value: String): String = hmac(value).toHex()
    fun tokenHash(token: String): String = sha256(token.toByteArray(Charsets.UTF_8))

    fun encrypt(body: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, GCMParameterSpec(128, iv))
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(body.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(encrypted: String): String {
        val bytes = Base64.getDecoder().decode(encrypted)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    private fun hmac(value: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(value.toByteArray(Charsets.UTF_8))
    }
    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
