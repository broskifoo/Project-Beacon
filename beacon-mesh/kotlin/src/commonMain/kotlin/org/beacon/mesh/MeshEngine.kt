package org.beacon.mesh

import kotlinx.coroutines.flow.Flow
import org.beacon.core.model.*
import org.beacon.sdk.model.PeerId
import org.beacon.sdk.model.Result

interface MeshEngine {

    // Lifecycle
    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>

    // Bundle handling
    suspend fun sendBundle(bundle: Bundle): Result<Unit>
    suspend fun receiveBundle(bundle: Bundle, fromPeer: PeerId, linkQuality: Float): Result<ReceiveResult>

    // Peer management
    suspend fun updateNeighbor(info: NeighborInfo): Result<Unit>
    suspend fun removeNeighbor(peerId: PeerId): Result<Unit>

    // Routing
    suspend fun getRouteTo(destination: PeerId): Result<RoutingTableEntry?>
    fun observeRoutingTable(): Flow<RoutingTable>

    // Topology
    fun observeTopology(): Flow<NetworkTopology>
    suspend fun getTopology(): Result<NetworkTopology>

    // Network stats
    fun observeNetworkStats(): Flow<NetworkStats>
    suspend fun getNetworkStats(): Result<NetworkStats>

    // Configuration
    suspend fun setConfig(config: MeshConfig): Result<Unit>
    suspend fun getConfig(): Result<MeshConfig>
}

enum class ReceiveResult {
    DELIVERED,
    FORWARD,
    DUPLICATE,
    TTL_EXPIRED,
    DROPPED,
    CUSTODY_ACCEPTED
}

data class MeshConfig(
    val maxHops: Int = 5,
    val defaultTtlSeconds: Long = 3600,
    val bundleBufferSize: Int = 1000,
    val neighborTimeoutSeconds: Long = 300,
    val topologyBroadcastIntervalSeconds: Long = 30,
    val enableGeographicRouting: Boolean = true,
    val enableEpidemicRouting: Boolean = true,
    val forwardingProbability: Float = 0.5f
) {
    companion object {
        const val DEFAULT = MeshConfig()
    }
}

data class NetworkStats(
    val peerCount: Int = 0,
    val connectedPeerCount: Int = 0,
    val messagesSent: Long = 0,
    val messagesReceived: Long = 0,
    val messagesForwarded: Long = 0,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val averageLatencyMs: Double = 0.0,
    val deliveryRate: Double = 0.0,
    val uptimeSeconds: Long = 0
)