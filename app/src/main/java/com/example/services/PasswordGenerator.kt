package com.example.services

import com.example.data.local.VaultEntryEntity
import java.security.SecureRandom

enum class PasswordStrength(val label: String, val scorePercent: Float) {
    WEAK("Weak", 0.25f),
    FAIR("Fair", 0.55f),
    STRONG("Strong", 0.80f),
    FORTRESS("Fortress", 1.0f)
}

data class VaultAuditReport(
    val totalCount: Int,
    val weakCount: Int,
    val reusedCount: Int,
    val reusedSecrets: Set<String>
)

object PasswordGenerator {
    private val random = SecureRandom()
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]"

    private val passphraseWords = listOf(
        "lotus", "forest", "amber", "granite", "river", "zenith", "harbor", "cedar",
        "obsidian", "mist", "breeze", "lantern", "summit", "meadow", "prism", "cipher",
        "solace", "horizon", "willow", "verge", "pebble", "canyon", "beacon", "timber"
    )

    fun generateStrongPassword(
        length: Int = 18,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val targetLen = length.coerceIn(8, 64)
        val pools = mutableListOf(LOWER)
        if (includeUppercase) pools.add(UPPER)
        if (includeDigits) pools.add(DIGITS)
        if (includeSymbols) pools.add(SYMBOLS)

        val chars = ArrayList<Char>(targetLen)
        pools.forEach { pool ->
            chars.add(pool[random.nextInt(pool.length)])
        }
        val combined = pools.joinToString("")
        while (chars.size < targetLen) {
            chars.add(combined[random.nextInt(combined.length)])
        }
        chars.shuffle(random)
        return chars.joinToString("")
    }

    fun generatePassphrase(wordCount: Int = 4, separator: String = "-"): String {
        val count = wordCount.coerceIn(3, 8)
        val words = (0 until count).map { passphraseWords[random.nextInt(passphraseWords.size)] }
        val pin = 10 + random.nextInt(89)
        return words.joinToString(separator) + separator + pin
    }

    fun evaluateStrength(secret: String): PasswordStrength {
        if (secret.length < 8) return PasswordStrength.WEAK
        var categories = 0
        if (secret.any { it.isLowerCase() }) categories++
        if (secret.any { it.isUpperCase() }) categories++
        if (secret.any { it.isDigit() }) categories++
        if (secret.any { !it.isLetterOrDigit() }) categories++

        return when {
            secret.length >= 16 && categories >= 3 -> PasswordStrength.FORTRESS
            secret.length >= 12 && categories >= 3 -> PasswordStrength.STRONG
            secret.length >= 10 && categories >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    fun auditVault(entries: List<VaultEntryEntity>): VaultAuditReport {
        val decryptedSecrets = entries.map { CryptoVaultHelper.decryptLocalSecret(it.secretValue) }
        val counts = decryptedSecrets.groupingBy { it }.eachCount()
        val reused = counts.filterValues { it > 1 }.keys
        val weakCount = decryptedSecrets.count { evaluateStrength(it) == PasswordStrength.WEAK }
        val reusedCount = decryptedSecrets.count { it in reused }
        return VaultAuditReport(
            totalCount = entries.size,
            weakCount = weakCount,
            reusedCount = reusedCount,
            reusedSecrets = reused
        )
    }
}
