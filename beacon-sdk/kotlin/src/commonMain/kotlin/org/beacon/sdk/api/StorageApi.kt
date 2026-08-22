package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface StorageApi {
    suspend fun put(key: String, value: ByteArray): Result<Unit>
    suspend fun get(key: String): Result<ByteArray?>
    suspend fun delete(key: String): Result<Unit>
    suspend fun exists(key: String): Result<Boolean>
    suspend fun keys(prefix: String = ""): Result<List<String>>
    suspend fun clear(): Result<Unit>
    suspend fun size(): Result<Long>

    fun observeKeys(prefix: String = ""): Flow<List<String>>
}

interface EncryptedStorageApi : StorageApi {
    suspend fun putEncrypted(key: String, value: ByteArray, associatedData: ByteArray? = null): Result<Unit>
    suspend fun getEncrypted(key: String, associatedData: ByteArray? = null): Result<ByteArray?>
}