package org.beacon.sdk.impl

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.beacon.sdk.api.EncryptedStorageApi
import org.beacon.sdk.api.StorageApi
import org.beacon.sdk.model.*
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidStorageApi(
    private val context: Context,
    private val config: BeaconConfig
) : EncryptedStorageApi {

    private val storageDir: File = File(context.filesDir, "beacon_storage").apply { mkdirs() }
    private val cryptoDir: File = File(context.filesDir, "beacon_crypto").apply { mkdirs() }
    
    private val masterKeyAlias = "beacon_master_key"
    private var masterKey: SecretKey? = null
    private val _keys = MutableStateFlow<List<String>>(loadKeys())
    override val observeKeys: kotlinx.coroutines.flow.Flow<List<String>> = _keys.asStateFlow()

    init {
        initializeCrypto()
    }

    private fun initializeCrypto() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            if (!keyStore.containsAlias(masterKeyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGenerator.init(KeyGenParameterSpec.Builder(masterKeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build())
                keyGenerator.generateKey()
            }
            
            masterKey = keyStore.getKey(masterKeyAlias, null) as SecretKey
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun loadKeys(): List<String> {
        return storageDir.listFiles()?.map { it.name } ?: emptyList()
    }

    private fun refreshKeys() {
        _keys.value = loadKeys()
    }

    override suspend fun put(key: String, value: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(storageDir, key).writeBytes(value)
            refreshKeys()
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Write failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun get(key: String): Result<ByteArray?> = withContext(Dispatchers.IO) {
        try {
            val file = File(storageDir, key)
            Success(file.readBytes().takeIf { it.isNotEmpty() })
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Read failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun delete(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(storageDir, key).delete()
            refreshKeys()
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Delete failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun exists(key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        Success(File(storageDir, key).exists())
    }

    override suspend fun keys(prefix: String = ""): Result<List<String>> = withContext(Dispatchers.IO) {
        Success(_keys.value.filter { it.startsWith(prefix) })
    }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storageDir.listFiles()?.forEach { it.delete() }
            refreshKeys()
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Clear failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun size(): Result<Long> = withContext(Dispatchers.IO) {
        val totalSize = storageDir.listFiles()?.sumOf { it.length() } ?: 0L
        Success(totalSize)
    }

    override suspend fun putEncrypted(key: String, value: ByteArray, associatedData: ByteArray? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, masterKey!!)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value, 0, value.size, associatedData?.let { it.copyOf() } ?: arrayOf())
            
            // Store IV + encrypted data
            val output = iv + encrypted
            File(storageDir, key).writeBytes(output)
            refreshKeys()
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.ENCRYPTION_FAILED, "Encryption failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun getEncrypted(key: String, associatedData: ByteArray? = null): Result<ByteArray?> = withContext(Dispatchers.IO) {
        try {
            val file = File(storageDir, key)
            if (!file.exists()) return Success(null)
            
            val data = file.readBytes()
            if (data.size < 12) return Success(null) // Invalid
            
            val iv = data.copyOfRange(0, 12)
            val encrypted = data.copyOfRange(12, data.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey!!, spec)
            val decrypted = cipher.doFinal(encrypted, 0, encrypted.size, associatedData?.let { it.copyOf() } ?: arrayOf())
            
            Success(decrypted)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.ENCRYPTION_FAILED, "Decryption failed: ${e.message}", cause = e.toString()))
        }
    }
}