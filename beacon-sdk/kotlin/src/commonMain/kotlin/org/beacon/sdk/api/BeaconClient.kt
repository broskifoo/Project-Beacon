package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface BeaconClient {
    val messaging: MessagingApi
    val peers: PeerApi
    val network: NetworkApi
    val maps: MapsApi
    val identity: IdentityApi
    val power: PowerApi
    val lifecycle: LifecycleApi

    suspend fun initialize(config: BeaconConfig): Result<Unit>
    suspend fun shutdown(): Result<Unit>
}

@Serializable
data class BeaconConfig(
    val deviceName: String = "Beacon Node",
    val enableBle: Boolean = true,
    val enableWifiDirect: Boolean = true,
    val enableLora: Boolean = false,
    val powerMode: PowerMode = PowerMode.NORMAL,
    val maxPeers: Int = 50,
    val messageTtl: Int = 5,
    val storagePath: String? = null,
    val logLevel: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure<T>(val error: BeaconError) : Result<T>
}

@Serializable
data class BeaconError(
    val code: ErrorCode,
    val message: String,
    val cause: String? = null,
    val recoverable: Boolean = true
)

enum class ErrorCode {
    NOT_INITIALIZED,
    ALREADY_INITIALIZED,
    PERMISSION_DENIED,
    BLUETOOTH_DISABLED,
    LOCATION_DISABLED,
    STORAGE_UNAVAILABLE,
    NETWORK_UNAVAILABLE,
    PEER_NOT_FOUND,
    MESSAGE_TOO_LARGE,
    ENCRYPTION_FAILED,
    SIGNATURE_VERIFICATION_FAILED,
    TIMEOUT,
    INVALID_ARGUMENT,
    INTERNAL_ERROR,
    UNSUPPORTED_OPERATION
}