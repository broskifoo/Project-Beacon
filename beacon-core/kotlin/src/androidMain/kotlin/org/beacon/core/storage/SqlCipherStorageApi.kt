package org.beacon.core.storage

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.beacon.core.api.StorageApi
import org.beacon.core.model.*
import org.beacon.sdk.model.Result
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure

class SqlCipherStorageApi(
    private val context: Context,
    private val database: BeaconDatabase
) : StorageApi, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val bundleDao = database.bundleDao()
    private val peerDao = database.peerDao()
    private val routeDao = database.routeDao()
    private val poiDao = database.poiDao()
    private val messageDao = database.messageDao()
    private val resourceDao = database.resourceDao()
    private val alertDao = database.alertDao()

    private val _keys = MutableStateFlow<List<String>>(emptyList())
    override val observeKeys = _keys.asStateFlow()

    override suspend fun put(key: String, value: ByteArray): Result<Unit> {
        return try {
            // Store in a generic key-value table or preferences
            // For now, use a simple approach
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Write failed: ${e.message}"))
        }
    }

    override suspend fun get(key: String): Result<ByteArray?> {
        return try {
            Success(null) // TODO: Implement generic KV storage
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Read failed: ${e.message}"))
        }
    }

    override suspend fun delete(key: String): Result<Unit> {
        return try {
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Delete failed: ${e.message}"))
        }
    }

    override suspend fun exists(key: String): Result<Boolean> {
        return Success(false)
    }

    override suspend fun keys(prefix: String = ""): Result<List<String>> {
        return Success(_keys.value.filter { it.startsWith(prefix) })
    }

    override suspend fun clear(): Result<Unit> {
        return try {
            // Clear all tables
            bundleDao.cleanupOldBundles(0, listOf(
                org.beacon.core.model.BundleStatus.DELIVERED.ordinal,
                org.beacon.core.model.BundleStatus.ACKNOWLEDGED.ordinal,
                org.beacon.core.model.BundleStatus.EXPIRED.ordinal,
                org.beacon.core.model.BundleStatus.FAILED.ordinal
            ))
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Clear failed: ${e.message}"))
        }
    }

    override suspend fun size(): Result<Long> {
        return Success(0L)
    }

    // Bundle operations
    suspend fun putBundle(bundle: Bundle): Result<Unit> {
        return try {
            bundleDao.insert(org.beacon.core.storage.BundleEntity.fromBundle(bundle))
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Bundle insert failed: ${e.message}"))
        }
    }

    suspend fun getBundle(bundleId: BundleId): Result<Bundle?> {
        return try {
            val entity = bundleDao.getById(bundleId.value)
            Success(entity?.toBundle())
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Bundle get failed: ${e.message}"))
        }
    }

    suspend fun deleteBundle(bundleId: BundleId): Result<Unit> {
        return try {
            bundleDao.deleteById(bundleId.value)
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Bundle delete failed: ${e.message}"))
        }
    }

    suspend fun queryBundles(
        destination: PeerId? = null,
        status: BundleStatus? = null,
        limit: Int = 100
    ): Result<List<Bundle>> {
        return try {
            val entities = when {
                destination != null && status != null -> bundleDao.getOutboxForDestination(destination.value, listOf(status.ordinal))
                destination != null -> bundleDao.getOutboxForDestination(destination.value, (0..6).toList())
                status != null -> bundleDao.getByStatus(status.ordinal, limit)
                else -> bundleDao.getByStatus(org.beacon.core.model.BundleStatus.OUTBOX.ordinal, limit)
            }
            Success(entities.take(limit).map { it.toBundle() })
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Bundle query failed: ${e.message}"))
        }
    }

    // Peer operations
    suspend fun putPeer(info: PeerInfo): Result<Unit> {
        return try {
            val entity = PeerEntity(
                peerId = info.peerId.value,
                displayName = info.displayName,
                lastSeen = info.lastSeen.toEpochMilliseconds(),
                locationX = info.location?.latitude,
                locationY = info.location?.longitude,
                batteryLevel = info.batteryLevel,
                powerMode = info.powerMode?.ordinal,
                transports = info.transports.joinToString(","),
                signalStrength = info.signalStrength,
                isOnline = info.isOnline,
                trustScore = info.trustScore,
                isTrusted = info.isTrusted,
                isBlocked = false
            )
            peerDao.upsert(entity)
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Peer insert failed: ${e.message}"))
        }
    }

    suspend fun getPeer(peerId: PeerId): Result<PeerInfo?> {
        return try {
            val entity = peerDao.getById(peerId.value)
            Success(entity?.toPeerInfo())
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Peer get failed: ${e.message}"))
        }
    }

    suspend fun deletePeer(peerId: PeerId): Result<Unit> {
        return try {
            peerDao.deleteById(peerId.value)
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Peer delete failed: ${e.message}"))
        }
    }

    suspend fun getAllPeers(): Result<List<PeerInfo>> {
        return try {
            val entities = peerDao.getActivePeers(0)
            Success(entities.map { it.toPeerInfo() })
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Peer query failed: ${e.message}"))
        }
    }

    // Route operations
    suspend fun putRoute(entry: RoutingTableEntry): Result<Unit> {
        return try {
            routeDao.upsert(RouteEntity(
                destination = entry.destination.value,
                nextHop = entry.nextHop.value,
                hopCount = entry.hopCount,
                quality = entry.quality,
                routeType = entry.routeType.ordinal,
                lastUpdate = entry.lastUpdate.toEpochMilliseconds(),
                expiresAt = entry.lastUpdate.plus(androidx.lifecycle.Lifecycle.Event.ON_STOP).toEpochMilliseconds()
            ))
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Route insert failed: ${e.message}"))
        }
    }

    suspend fun getRoute(destination: PeerId): Result<RoutingTableEntry?> {
        return try {
            val entity = routeDao.getRoute(destination.value)
            Success(entity?.toRoutingEntry())
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Route get failed: ${e.message}"))
        }
    }

    suspend fun deleteRoute(destination: PeerId): Result<Unit> {
        return try {
            routeDao.deleteRoute(destination.value)
            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Route delete failed: ${e.message}"))
        }
    }

    suspend fun getAllRoutes(): Result<List<RoutingTableEntry>> {
        return try {
            // TODO: Implement
            Success(emptyList())
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Routes query failed: ${e.message}"))
        }
    }

    // Cleanup
    suspend fun performMaintenance(): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val cutoff = now - (30 * 24 * 60 * 60 * 1000) // 30 days

            bundleDao.cleanupOldBundles(cutoff, listOf(
                org.beacon.core.model.BundleStatus.DELIVERED.ordinal,
                org.beacon.core.model.BundleStatus.ACKNOWLEDGED.ordinal,
                org.beacon.core.model.BundleStatus.EXPIRED.ordinal,
                org.beacon.core.model.BundleStatus.FAILED.ordinal
            ))
            peerDao.cleanupStalePeers(cutoff)
            routeDao.cleanupExpiredRoutes(now)
            poiDao.cleanupExpiredPois(now)
            messageDao.cleanupOldMessages(cutoff)
            resourceDao.cleanupExpiredResources(now)
            alertDao.cleanupExpiredAlerts(now)

            Success(Unit)
        } catch (e: Exception) {
            Failure(BeaconError(ErrorCode.STORAGE_UNAVAILABLE, "Maintenance failed: ${e.message}"))
        }
    }
}

// Extension functions for entity conversion
private fun PeerEntity.toPeerInfo(): PeerInfo {
    return PeerInfo(
        peerId = PeerId(peerId),
        displayName = displayName,
        lastSeen = kotlinx.datetime.Instant.ofEpochMilliseconds(lastSeen),
        location = (locationX?.let { latitude -> locationY?.let { longitude -> 
            org.beacon.core.model.Location(latitude, longitude) }
        }) ?: null,
        batteryLevel = batteryLevel,
        powerMode = powerMode?.let { org.beacon.core.model.PowerMode.values()[it] },
        transports = transports?.split(",")?.map { org.beacon.core.model.TransportType.valueOf(it.trim().uppercase()) }?.toSet() ?: emptySet(),
        signalStrength = signalStrength,
        isOnline = isOnline,
        trustScore = trustScore
    )
}

private fun RouteEntity.toRoutingEntry(): RoutingTableEntry {
    return RoutingTableEntry(
        destination = PeerId(destination),
        nextHop = PeerId(nextHop),
        hopCount = hopCount,
        quality = quality,
        lastUpdate = kotlinx.datetime.Instant.ofEpochMilliseconds(lastUpdate),
        routeType = org.beacon.core.model.RouteType.values()[routeType]
    )
}