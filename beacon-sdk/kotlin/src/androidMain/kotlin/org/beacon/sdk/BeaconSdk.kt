package org.beacon.sdk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.beacon.sdk.api.*
import org.beacon.sdk.model.*

actual class BeaconSdk internal constructor(
    actual override val messaging: MessagingApi,
    actual override val peers: PeerApi,
    actual override val network: NetworkApi,
    actual override val maps: MapsApi,
    actual override val identity: IdentityApi,
    actual override val power: PowerApi,
    actual override val storage: StorageApi,
    actual override val lifecycle: LifecycleApi,
    private val config: BeaconConfig,
    private val context: Context
) {

    actual suspend fun initialize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Request permissions
                val permResult = requestPermissions()
                if (permResult is Result.Failure) return@withContext permResult

                // Initialize platform-specific components
                val initResult = lifecycle.start()
                return@withContext initResult
            } catch (e: Exception) {
                Result.Failure(BeaconError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Initialization failed: ${e.message}",
                    cause = e.toString()
                ))
            }
        }
    }

    actual suspend fun shutdown(): Result<Unit> {
        return lifecycle.stop()
    }

    private suspend fun requestPermissions(): Result<Unit> {
        // Permissions handled by host app
        return Result.Success(Unit)
    }

    companion object {
        actual fun create(config: BeaconConfig): Result<BeaconSdk> {
            // This is called from common code, but Android needs Context
            // The actual creation should use the factory with Context
            throw UnsupportedOperationException("Use BeaconSdkFactory.create(context, config) on Android")
        }
    }
}

class BeaconSdkFactory {
    companion object {
        fun create(context: Context, config: BeaconConfig = BeaconConfig()): Result<BeaconSdk> {
            return try {
                val appContext = context.applicationContext
                
                val sdk = DefaultBeaconSdk(
                    messaging = AndroidMessagingApi(appContext, config),
                    peers = AndroidPeerApi(appContext, config),
                    network = AndroidNetworkApi(appContext, config),
                    maps = AndroidMapsApi(appContext, config),
                    identity = AndroidIdentityApi(appContext, config),
                    power = AndroidPowerApi(appContext, config),
                    storage = AndroidStorageApi(appContext, config),
                    lifecycle = AndroidLifecycleApi(appContext, config),
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