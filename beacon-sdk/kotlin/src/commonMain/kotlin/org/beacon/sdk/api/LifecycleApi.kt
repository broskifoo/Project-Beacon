package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface LifecycleApi {
    val state: LifecycleState
    fun observeState(): Flow<LifecycleState>

    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun restart(): Result<Unit>

    fun observeHealth(): Flow<HealthStatus>
}

enum class LifecycleState {
    UNINITIALIZED,
    INITIALIZING,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR
}

@Serializable
data class HealthStatus(
    val isHealthy: Boolean,
    val checks: Map<String, HealthCheck>,
    val timestamp: Instant = Instant.now()
)

@Serializable
data class HealthCheck(
    val name: String,
    val status: HealthStatusType,
    val message: String? = null,
    val latencyMs: Long? = null
)

enum class HealthStatusType {
    PASS, WARN, FAIL
}