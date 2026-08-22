package org.beacon.sdk.impl

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import org.beacon.sdk.api.IdentityApi
import org.beacon.sdk.model.*
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

class AndroidIdentityApi(
    private val context: Context,
    private val config: BeaconConfig
) : IdentityApi {

    private val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private val KEY_ALIAS = "beacon_identity_key"
    private val _identity = MutableStateFlow<Identity?>(null)
    override val observeIdentityChanges: kotlinx.coroutines.flow.Flow<Identity> = _identity
        .filterNotNull()
        .distinctUntilChanged()
        .asStateFlow()

    override suspend fun getIdentity(): Result<Identity> {
        val identity = _identity.value
        return identity?.let { Success(it) } ?: Failure(BeaconError(ErrorCode.NOT_INITIALIZED, "Identity not generated"))
    }

    override suspend fun generateIdentity(): Result<Identity> {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
                kpg.initialize(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(android.security.keystore.ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build())
                kpg.generateKeyPair()
            }
            
            val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
            val publicKeyBytes = publicKey.encoded
            val publicKeyHex = publicKeyBytes.joinToString("") { "%02X".format(it) }
            
            val identity = Identity(
                publicKey = publicKeyHex,
                keyId = publicKeyHex.substring(0, 16),
                createdAt = Instant.now()
            )
            _identity.value = identity
            Success(identity)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.INTERNAL_ERROR, "Failed to generate identity: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun importIdentity(privateKeyHex: String): Result<Identity> {
        // TODO: Import private key (not supported in Android Keystore for EC)
        return Failure(BeaconError(ErrorCode.UNSUPPORTED_OPERATION, "Key import not supported"))
    }

    override suspend fun exportIdentity(): Result<String> {
        // Android Keystore doesn't allow private key export
        return Failure(BeaconError(ErrorCode.UNSUPPORTED_OPERATION, "Private key export not supported"))
    }

    override suspend fun sign(data: ByteArray): Result<ByteArray> {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey
            
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(data)
            val signed = signature.sign()
            Success(signed)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.ENCRYPTION_FAILED, "Signing failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: String): Result<Boolean> {
        return try {
            val publicKeyBytes = publicKey.hexToByteArray()
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val pubKeySpec = X509EncodedKeySpec(publicKeyBytes)
            val pubKey = keyFactory.generatePublic(pubKeySpec)
            
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(pubKey)
            sig.update(data)
            Success(sig.verify(signature))
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.SIGNATURE_VERIFICATION_FAILED, "Verification failed: ${e.message}", cause = e.toString()))
        }
    }

    override suspend fun deriveSharedSecret(peerPublicKey: String): Result<ByteArray> {
        return try {
            val peerKeyBytes = peerPublicKey.hexToByteArray()
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val pubKeySpec = X509EncodedKeySpec(peerKeyBytes)
            val peerPubKey = keyFactory.generatePublic(pubKeySpec)
            
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey
            
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(peerPubKey, true)
            
            Success(keyAgreement.generateSecret())
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.ENCRYPTION_FAILED, "Key agreement failed: ${e.message}", cause = e.toString()))
        }
    }

    private fun String.hexToByteArray(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}