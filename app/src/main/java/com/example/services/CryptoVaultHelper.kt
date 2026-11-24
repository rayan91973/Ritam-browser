package com.example.services

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoVaultHelper {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "ritam_vault_aes_master_v1"
    private const val LOCAL_PREFIX = "ritam_aes_v1:"
    private const val BACKUP_FORMAT = "ritam_encrypted_backup_v1"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val PBKDF2_ITERATIONS = 100_000
    private val random = SecureRandom()

    fun isEncryptedLocalSecret(value: String): Boolean = value.startsWith(LOCAL_PREFIX)

    fun encryptLocalSecret(plaintext: String): String {
        if (plaintext.isBlank() || isEncryptedLocalSecret(plaintext)) return plaintext
        val secretKey = getOrCreateKeystoreKey() ?: return plaintext
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val ivB64 = Base64.getEncoder().encodeToString(cipher.iv)
            val ctB64 = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)))
            "$LOCAL_PREFIX$ivB64:$ctB64"
        }.getOrElse {
            plaintext
        }
    }

    fun decryptLocalSecret(stored: String): String {
        if (!isEncryptedLocalSecret(stored)) return stored
        val body = stored.removePrefix(LOCAL_PREFIX)
        val parts = body.split(":")
        if (parts.size != 2) return stored
        val secretKey = getOrCreateKeystoreKey() ?: return ""
        return runCatching {
            val iv = Base64.getDecoder().decode(parts[0])
            val ct = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun isEncryptedBackupEnvelope(rawText: String): Boolean {
        val trimmed = rawText.trim()
        if (!trimmed.startsWith("{")) return false
        return runCatching {
            JSONObject(trimmed).optString("format") == BACKUP_FORMAT
        }.getOrDefault(false)
    }

    fun encryptBackupWithPassphrase(plainJson: String, passphrase: String): String {
        require(passphrase.length >= 6) { "Passphrase must be at least 6 characters" }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { random.nextBytes(it) }
        val key = deriveAesKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val encoder = Base64.getEncoder()
        return JSONObject().apply {
            put("format", BACKUP_FORMAT)
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", PBKDF2_ITERATIONS)
            put("salt", encoder.encodeToString(salt))
            put("iv", encoder.encodeToString(iv))
            put("ciphertext", encoder.encodeToString(ciphertext))
        }.toString(2)
    }

    fun decryptBackupWithPassphrase(encryptedEnvelope: String, passphrase: String): String {
        require(passphrase.isNotEmpty()) { "Passphrase required" }
        val obj = JSONObject(encryptedEnvelope)
        require(obj.optString("format") == BACKUP_FORMAT) { "Not an encrypted Ritam backup" }

        val decoder = Base64.getDecoder()
        val salt = decoder.decode(obj.getString("salt"))
        val iv = decoder.decode(obj.getString("iv"))
        val ciphertext = decoder.decode(obj.getString("ciphertext"))
        val iterations = obj.optInt("iterations", PBKDF2_ITERATIONS).coerceAtLeast(10_000)

        val key = deriveAesKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun deriveAesKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val raw = factory.generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun getOrCreateKeystoreKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return@runCatching it }

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }
}
