package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.PowerMode

interface PowerApi {
    fun observePowerMode(): Flow<PowerMode>

    suspend fun getCurrentMode(): Result<PowerMode>

    suspend fun setPowerMode(mode: PowerMode): Result<Unit>

    suspend fun getBatteryLevel(): Result<Int>

    fun observeBatteryLevel(): Flow<Int>

    suspend fun getEstimatedRuntime(): Result<EstimatedRuntime>

    suspend fun requestWakeLock(durationMs: Long): Result<WakeLockToken>

    suspend fun releaseWakeLock(token: WakeLockToken): Result<Unit>
}

@Serializable
data class EstimatedRuntime(
    val normalModeHours: Double,
    val conservationModeHours: Double,
    val survivalModeHours: Double,
    val criticalModeHours: Double
)

@Serializable
data class WakeLockToken(
    val id: String,
    val acquiredAt: Instant,
    val expiresAt: Instant
)