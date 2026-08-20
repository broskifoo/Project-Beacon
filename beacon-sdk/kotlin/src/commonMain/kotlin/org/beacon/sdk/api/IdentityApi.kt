package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface IdentityApi {
    suspend fun getIdentity(): Result<Identity>

    suspend fun generateIdentity(): Result<Identity>

    suspend fun importIdentity(privateKeyHex: String): Result<Identity>

    suspend fun exportIdentity(): Result<String>  // Private key hex (encrypted)

    suspend fun sign(data: ByteArray): Result<ByteArray>

    suspend fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: String): Result<Boolean>

    suspend fun deriveSharedSecret(peerPublicKey: String): Result<ByteArray>

    fun observeIdentityChanges(): Flow<Identity>
}

@Serializable
data class Identity(
    val publicKey: String,       // Ed25519 public key (hex)
    val keyId: String,           // Key identifier (first 16 chars of public key)
    val createdAt: Instant,
    val keyType: KeyType = KeyType.ED25519
)

enum class KeyType {
    ED25519,
    X25519
}