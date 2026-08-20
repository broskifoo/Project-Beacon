package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface NetworkApi {
    fun observeTopology(): Flow<NetworkTopology>

    suspend fun getTopology(): Result<NetworkTopology>

    suspend fun getRouteTo(target: PeerId): Result<RouteInfo>

    suspend fun getNetworkStats(): Result<NetworkStats>

    fun observeNetworkStats(): Flow<NetworkStats>

    suspend fun requestPeerList(): Result<Unit>

    suspend fun announcePresence(): Result<Unit>
}

@Serializable
data class RouteInfo(
    val nextHop: PeerId,
    val hopCount: Int,
    val estimatedLatencyMs: Long,
    val path: List<PeerId>,
    var confidence: Float = 1.0f
)

@Serializable
data class NetworkStats(
    val peerCount: Int,
    val connectedPeerCount: Int,
    val messagesSent: Long,
    val messagesReceived: Long,
    val messagesForwarded: Long,
    val bytesSent: Long,
    val bytesReceived: Long,
    val averageLatencyMs: Double,
    val deliveryRate: Double,
    val uptimeSeconds: Long
)