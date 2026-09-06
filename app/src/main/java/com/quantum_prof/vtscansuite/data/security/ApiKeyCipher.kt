// data/security/ApiKeyCipher.kt
package com.quantum_prof.vtscansuite.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verschlüsselt den VirusTotal-API-Key mit einem AES-256-GCM-Schlüssel aus dem
 * Android Keystore. Der Schlüssel selbst verlässt den Keystore nie (und wird
 * weder gesichert noch auf ein anderes Gerät übertragen), sodass der in
 * DataStore abgelegte Chiffretext ausserhalb dieses Geräts wertlos ist.
 *
 * Format des gespeicherten Werts: Base64( IV (12 Byte) || Ciphertext || GCM-Tag ).
 */
@Singleton
class ApiKeyCipher @Inject constructor() {

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "vt_api_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val BASE64_FLAGS = Base64.NO_WRAP
    }

    /** Verschlüsselt [plain]. Gibt `null` zurück, wenn der Keystore nicht verfügbar ist. */
    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == IV_LENGTH) { "Unexpected IV length ${iv.size}" }
        Base64.encodeToString(iv + encrypted, BASE64_FLAGS)
    }.getOrNull()

    /**
     * Entschlüsselt einen zuvor mit [encrypt] erzeugten Wert. Gibt `null` zurück, wenn der
     * Wert beschädigt ist oder der Keystore-Schlüssel fehlt (z. B. nach einer
     * Wiederherstellung auf einem anderen Gerät) – der Nutzer gibt den Key dann neu ein.
     */
    fun decrypt(stored: String): String? = runCatching {
        val raw = Base64.decode(stored, BASE64_FLAGS)
        if (raw.size <= IV_LENGTH) return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, raw, 0, IV_LENGTH)
            )
        }
        String(cipher.doFinal(raw, IV_LENGTH, raw.size - IV_LENGTH), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
