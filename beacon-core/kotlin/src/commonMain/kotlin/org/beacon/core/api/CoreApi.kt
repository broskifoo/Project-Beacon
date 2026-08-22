package org.beacon.core.api

import kotlinx.coroutines.flow.Flow
import org.beacon.core.model.*
import org.beacon.sdk.model.MessagePriority
import org.beacon.sdk.model.PeerId
import org.beacon.sdk.model.Result

interface BundleApi {
    suspend fun createBundle(
        destination: PeerId?,
        payload: ByteArray,
        priority: MessagePriority = MessagePriority.NORMAL,
        maxHops: Int = 5,
        ttlSeconds: Long = 3600
    ): Result<BundleId>

    suspend fun getBundle(bundleId: BundleId): Result<Bundle>
    suspend fun deleteBundle(bundleId: BundleId): Result<Unit>
    
    fun observeBundle(bundleId: BundleId): Flow<Bundle?>
    fun observeOutbox(): Flow<List<Bundle>>
    fun observeInbox(): Flow<List<Bundle>>
}

interface RoutingApi {
    suspend fun findRoute(destination: PeerId): Result<RoutingTableEntry>
    suspend fun updateRoute(entry: RoutingTableEntry): Result<Unit>
    suspend fun removeRoute(destination: PeerId): Result<Unit>
    
    fun observeRoutingTable(): Flow<Map<PeerId, RoutingTableEntry>>
    fun observeRoutesTo(destination: PeerId): Flow<RoutingTableEntry?>
}

interface NeighborApi {
    suspend fun getNeighbor(peerId: PeerId): Result<NodeInfo>
    suspend fun updateNeighbor(info: NodeInfo): Result<Unit>
    suspend fun removeNeighbor(peerId: PeerId): Result<Unit>
    
    fun observeNeighbors(): Flow<List<NodeInfo>>
    fun observeNeighbor(peerId: PeerId): Flow<NodeInfo?>
    
    suspend fun getActiveNeighbors(transport: TransportType? = null): Result<List<NodeInfo>>
}

interface CustodyApi {
    suspend fun acceptCustody(bundleId: BundleId, nodeId: PeerId): Result<Unit>
    suspend fun releaseCustody(bundleId: BundleId): Result<Unit>
    suspend fun getCustodyBundles(nodeId: PeerId): Result<List<BundleId>>
}

interface TopologyApi {
    fun observeTopology(): Flow<NetworkTopology>
    suspend fun getTopology(): Result<NetworkTopology>
    
    suspend fun announcePresence(): Result<Unit>
    suspend fun requestTopologySync(): Result<Unit>
}

interface SchedulerApi {
    suspend fun schedulePeriodic(task: SuspendTask, intervalMs: Long): Result<JobHandle>
    suspend fun scheduleDelayed(task: SuspendTask, delayMs: Long): Result<JobHandle>
    suspend fun cancelJob(handle: JobHandle): Result<Unit>
    
    interface SuspendTask {
        suspend fun run()
    }
    
    data class JobHandle(val id: String)
}

interface StorageApi {
    suspend fun putBundle(bundle: Bundle): Result<Unit>
    suspend fun getBundle(bundleId: BundleId): Result<Bundle?>
    suspend fun deleteBundle(bundleId: BundleId): Result<Unit>
    suspend fun queryBundles(
        destination: PeerId? = null,
        status: BundleStatus? = null,
        limit: Int = 100
    ): Result<List<Bundle>>
    
    suspend fun putNeighbor(info: NodeInfo): Result<Unit>
    suspend fun getNeighbor(peerId: PeerId): Result<NodeInfo?>
    suspend fun deleteNeighbor(peerId: PeerId): Result<Unit>
    suspend fun getAllNeighbors(): Result<List<NodeInfo>>
    
    suspend fun putRoute(entry: RoutingTableEntry): Result<Unit>
    suspend fun getRoute(destination: PeerId): Result<RoutingTableEntry?>
    suspend fun deleteRoute(destination: PeerId): Result<Unit>
    suspend fun getAllRoutes(): Result<List<RoutingTableEntry>>
}

enum class BundleStatus {
    OUTBOX, IN_TRANSIT, INBOX, DELIVERED, ACKNOWLEDGED, EXPIRED, FAILED
}