package org.telegram.messenger.fork.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore backed AES-GCM used for locally archived message content.
 *
 * The key never leaves the keystore and is created per account, so data of one
 * account cannot be decrypted with the key of another one. Only ciphertext is
 * written to the archive databases. If the keystore entry is invalidated the
 * stored content becomes unreadable, which is the intended failure mode.
 */
object ForkArchiveCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val ALIAS_PREFIX = "berrygram_archive_aes_"

    private val lock = Any()
    private val keys = HashMap<Int, SecretKey>()
    private val unavailable = HashSet<Int>()

    private fun alias(account: Int): String = ALIAS_PREFIX + account

    private fun key(account: Int): SecretKey? {
        synchronized(lock) {
            if (account in unavailable) return null
            keys[account]?.let { return it }
            try {
                val store = KeyStore.getInstance(KEYSTORE)
                store.load(null)
                val existing = store.getKey(alias(account), null)
                if (existing is SecretKey) {
                    keys[account] = existing
                    return existing
                }
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        alias(account),
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                val generated = generator.generateKey()
                keys[account] = generated
                return generated
            } catch (error: Throwable) {
                unavailable.add(account)
                keys.remove(account)
                return null
            }
        }
    }

    fun isAvailable(account: Int): Boolean = key(account) != null

    /** Returns null when encryption is unavailable; callers must not write plaintext. */
    fun encrypt(account: Int, plain: String): ByteArray? {
        if (plain.isEmpty()) return ByteArray(0)
        val secret = key(account) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            // Android Keystore rejects caller-provided IVs for randomized keys.
            cipher.init(Cipher.ENCRYPT_MODE, secret)
            val iv = cipher.iv ?: return null
            if (iv.size != IV_BYTES) return null
            val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val result = ByteArray(iv.size + body.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(body, 0, result, iv.size, body.size)
            result
        } catch (error: Throwable) {
            null
        }
    }

    fun decrypt(account: Int, stored: ByteArray?): String? {
        if (stored == null || stored.size <= IV_BYTES) return null
        val secret = key(account) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secret,
                GCMParameterSpec(TAG_BITS, stored, 0, IV_BYTES)
            )
            val plain = cipher.doFinal(stored, IV_BYTES, stored.size - IV_BYTES)
            String(plain, Charsets.UTF_8)
        } catch (error: Throwable) {
            null
        }
    }
}
