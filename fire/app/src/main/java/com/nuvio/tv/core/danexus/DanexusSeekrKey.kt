package com.nuvio.tv.core.danexus

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The per-user API key is encrypted by Android Keystore and excluded from backups. */
object DanexusSeekrKey {
    private const val ALIAS = "danexus-seekr-user-key-v1"
    private val changes = MutableStateFlow(0)
    val revision: StateFlow<Int> get() = changes
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "danexus-seekr.enc"))
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun read(context: Context): String = runCatching {
        val bytes = file(context).readFully()
        require(bytes.size > 28)
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }.getOrDefault("")
    @Synchronized fun save(context: Context, value: String): Boolean {
        val stored = runCatching {
            val target = file(context)
            if (value.isBlank()) target.delete() else {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
                val bytes = cipher.iv + cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
                val stream = target.startWrite()
                try { stream.write(bytes); target.finishWrite(stream) }
                catch (failure: Exception) { target.failWrite(stream); throw failure }
            }
        }.isSuccess
        if (stored) changes.value += 1
        return stored
    }
}
