package org.beacon.sdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.beacon.sdk.api.*
import org.beacon.sdk.model.BeaconConfig
import org.beacon.sdk.model.BeaconError
import org.beacon.sdk.model.ErrorCode
import org.beacon.sdk.model.Result

expect class BeaconSdk private constructor() {
    val messaging: MessagingApi
    val peers: PeerApi
    val network: NetworkApi
    val maps: MapsApi
    val identity: IdentityApi
    val power: PowerApi
    val storage: StorageApi
    val lifecycle: LifecycleApi

    companion object {
        fun create(config: BeaconConfig = BeaconConfig()): Result<BeaconSdk>
    }

    suspend fun initialize(): Result<Unit>
    suspend fun shutdown(): Result<Unit>
}

internal class DefaultBeaconSdk(
    override val messaging: MessagingApi,
    override val peers: PeerApi,
    override val network: NetworkApi,
    override val maps: MapsApi,
    override val identity: IdentityApi,
    override val power: PowerApi,
    override val storage: StorageApi,
    override val lifecycle: LifecycleApi,
    private val config: BeaconConfig
) : BeaconSdk() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun initialize(): Result<Unit> {
        return lifecycle.start()
    }

    override suspend fun shutdown(): Result<Unit> {
        scope.coroutineContext.cancelChildren()
        return lifecycle.stop()
    }

    companion object {
        fun create(config: BeaconConfig): Result<BeaconSdk> {
            return try {
                val sdk = DefaultBeaconSdk(
                    messaging = createMessagingApi(),
                    peers = createPeerApi(),
                    network = createNetworkApi(),
                    maps = createMapsApi(),
                    identity = createIdentityApi(),
                    power = createPowerApi(),
                    storage = createStorageApi(),
                    lifecycle = createLifecycleApi(),
                    config = config
                )
                Result.Success(sdk)
            } catch (e: Exception) {
                Result.Failure(BeaconError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Failed to create Beacon SDK: ${e.message}",
                    cause = e.toString()
                ))
            }
        }
    }
}

internal fun createMessagingApi(): MessagingApi = TODO("Implementation in platform-specific modules")
internal fun createPeerApi(): PeerApi = TODO("Implementation in platform-specific modules")
internal fun createNetworkApi(): NetworkApi = TODO("Implementation in platform-specific modules")
internal fun createMapsApi(): MapsApi = TODO("Implementation in platform-specific modules")
internal fun createIdentityApi(): IdentityApi = TODO("Implementation in platform-specific modules")
internal fun createPowerApi(): PowerApi = TODO("Implementation in platform-specific modules")
internal fun createStorageApi(): StorageApi = TODO("Implementation in platform-specific modules")
internal fun createLifecycleApi(): LifecycleApi = TODO("Implementation in platform-specific modules")