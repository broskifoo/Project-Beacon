package org.beacon.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.beacon.core.model.Bundle
import org.beacon.core.model.BundleId
import org.beacon.core.model.BundleStatus
import org.beacon.core.model.MessagePriority
import org.beacon.core.model.PeerId

@Dao
interface BundleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bundle: BundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bundles: List<BundleEntity>)

    @Update
    suspend fun update(bundle: BundleEntity)

    @Delete
    suspend fun delete(bundle: BundleEntity)

    @Query("DELETE FROM bundles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM bundles WHERE id = :id")
    suspend fun getById(id: String): BundleEntity?

    @Query("SELECT * FROM bundles WHERE status = :status ORDER BY created_at DESC LIMIT :limit")
    suspend fun getByStatus(status: Int, limit: Int): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE destination = :destination AND status IN (:statuses) ORDER BY priority DESC, created_at ASC")
    suspend fun getOutboxForDestination(destination: String, statuses: List<Int>): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE status = :status AND destination IS NULL ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getBroadcastOutbox(status: Int, limit: Int): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE custody_holder = :custodian AND status = :status ORDER BY last_retry ASC")
    suspend fun getCustodyBundles(custodian: String, status: Int): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE expires_at < :now AND status NOT IN (:terminalStatuses)")
    suspend fun getExpiredBundles(now: Long, terminalStatuses: List<Int>): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE status = :status AND (destination IS NULL OR destination = :localId) ORDER BY created_at DESC LIMIT :limit")
    suspend fun getInbox(localId: String, status: Int, limit: Int): List<BundleEntity>

    @Query("SELECT COUNT(*) FROM bundles WHERE status = :status")
    suspend fun countByStatus(status: Int): Int

    @Query("DELETE FROM bundles WHERE expires_at < :cutoff AND status IN (:terminalStatuses)")
    suspend fun cleanupOldBundles(cutoff: Long, terminalStatuses: List<Int>): Int

    @Transaction
    suspend fun markDelivered(id: String) {
        val bundle = getById(id) ?: return
        update(bundle.copy(status = BundleStatus.DELIVERED.ordinal))
    }

    @Transaction
    suspend fun markAcknowledged(id: String) {
        val bundle = getById(id) ?: return
        update(bundle.copy(status = BundleStatus.ACKNOWLEDGED.ordinal))
    }

    @Transaction
    suspend fun incrementRetry(id: String, now: Long) {
        val bundle = getById(id) ?: return
        update(bundle.copy(
            retryCount = bundle.retryCount + 1,
            lastRetry = now,
            status = BundleStatus.IN_TRANSIT.ordinal
        ))
    }
}

@Dao
interface PeerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: PeerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(peers: List<PeerEntity>)

    @Query("SELECT * FROM peers WHERE peer_id = :peerId")
    suspend fun getById(peerId: String): PeerEntity?

    @Query("SELECT * FROM peers WHERE is_online = 1 AND (last_seen > :since OR :since <= 0) ORDER BY last_seen DESC")
    suspend fun getActivePeers(since: Long): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE is_online = 1 ORDER BY signal_strength DESC LIMIT :limit")
    suspend fun getPeersBySignal(limit: Int): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE is_trusted = 1")
    suspend fun getTrustedPeers(): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE is_blocked = 1")
    suspend fun getBlockedPeers(): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE last_seen < :cutoff")
    suspend fun getStalePeers(cutoff: Long): List<PeerEntity>

    @Query("DELETE FROM peers WHERE peer_id = :peerId")
    suspend fun deleteById(peerId: String)

    @Query("UPDATE peers SET is_online = 0 WHERE peer_id = :peerId")
    suspend fun markOffline(peerId: String)

    @Query("UPDATE peers SET is_online = 1, last_seen = :now WHERE peer_id = :peerId")
    suspend fun markOnline(peerId: String, now: Long)

    @Query("UPDATE peers SET signal_strength = :signal, last_seen = :now WHERE peer_id = :peerId")
    suspend fun updateSignal(peerId: String, signal: Int, now: Long)

    @Query("UPDATE peers SET trust_score = :trust, is_trusted = :trusted WHERE peer_id = :peerId")
    suspend fun updateTrust(peerId: String, trust: Float, trusted: Boolean)

    @Query("UPDATE peers SET is_blocked = :blocked WHERE peer_id = :peerId")
    suspend fun setBlocked(peerId: String, blocked: Boolean)

    @Query("DELETE FROM peers WHERE last_seen < :cutoff AND is_online = 0")
    suspend fun cleanupStalePeers(cutoff: Long): Int
}

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(routes: List<RouteEntity>)

    @Query("SELECT * FROM routes WHERE destination = :destination")
    suspend fun getRoute(destination: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE next_hop = :nextHop AND expires_at > :now")
    suspend fun getRoutesViaNextHop(nextHop: String, now: Long): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE expires_at < :now")
    suspend fun getExpiredRoutes(now: Long): List<RouteEntity>

    @Query("DELETE FROM routes WHERE destination = :destination")
    suspend fun deleteRoute(destination: String)

    @Query("DELETE FROM routes WHERE expires_at < :now")
    suspend fun cleanupExpiredRoutes(now: Long): Int

    @Transaction
    suspend fun recordSuccess(destination: String, latencyMs: Double, hops: Int) {
        val route = getRoute(destination) ?: return
        val newSuccessCount = route.successCount + 1
        val newAvgLatency = (route.avgLatencyMs * route.successCount + latencyMs) / newSuccessCount
        val newAvgHops = (route.avgHopCount * route.successCount + hops) / newSuccessCount
        update(route.copy(
            successCount = newSuccessCount,
            avgLatencyMs = newAvgLatency,
            avgHopCount = newAvgHops,
            lastUpdate = System.currentTimeMillis()
        ))
    }

    @Transaction
    suspend fun recordFailure(destination: String) {
        val route = getRoute(destination) ?: return
        update(route.copy(failureCount = route.failureCount + 1))
    }
}

@Dao
interface PoiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poi: PoiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pois: List<PoiEntity>)

    @Query("SELECT * FROM pois WHERE id = :id")
    suspend fun getById(id: String): PoiEntity?

    @Query("SELECT * FROM pois WHERE category = :category AND expires_at > :now OR expires_at IS NULL ORDER BY confidence DESC LIMIT :limit")
    suspend fun getByCategory(category: String, now: Long, limit: Int): List<PoiEntity>

    @Query("SELECT * FROM pois WHERE location_x BETWEEN :minX AND :maxX AND location_y BETWEEN :minY AND :maxY AND (expires_at > :now OR expires_at IS NULL) LIMIT :limit")
    suspend fun getInBounds(minX: Double, maxX: Double, minY: Double, maxY: Double, now: Long, limit: Int): List<PoiEntity>

    @Query("SELECT * FROM pois WHERE (location_x - :lat) * (location_x - :lat) + (location_y - :lng) * (location_y - :lng) < :radiusSq AND (expires_at > :now OR expires_at IS NULL) ORDER BY ((location_x - :lat) * (location_x - :lat) + (location_y - :lng) * (location_y - :lng)) LIMIT :limit")
    suspend fun getNearby(lat: Double, lng: Double, radiusSq: Double, now: Long, limit: Int): List<PoiEntity>

    @Query("SELECT * FROM pois WHERE name LIKE :query OR description LIKE :query AND (expires_at > :now OR expires_at IS NULL) LIMIT :limit")
    suspend fun search(query: String, now: Long, limit: Int): List<PoiEntity>

    @Query("SELECT * FROM pois WHERE expires_at < :now")
    suspend fun getExpiredPois(now: Long): List<PoiEntity>

    @Query("DELETE FROM pois WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pois WHERE expires_at < :now")
    suspend fun cleanupExpiredPois(now: Long): Int

    @Query("SELECT * FROM pois WHERE source = :source AND (expires_at > :now OR expires_at IS NULL)")
    suspend fun getBySource(source: String, now: Long): List<PoiEntity>

    @Query("UPDATE pois SET confidence = :confidence, updated_at = :now WHERE id = :id")
    suspend fun updateConfidence(id: String, confidence: Float, now: Long)
}

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE (sender_id = :peerId OR recipient_id = :peerId) ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getConversation(peerId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByStatus(status: Int, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sender_id = :senderId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getSentBy(senderId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE recipient_id = :recipientId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getReceivedBy(recipientId: String, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE timestamp < :cutoff")
    suspend fun cleanupOldMessages(cutoff: Long): Int

    @Transaction
    suspend fun markDelivered(id: String) {
        val msg = getById(id) ?: return
        update(msg.copy(status = org.beacon.sdk.model.DeliveryState.DELIVERED.ordinal))
    }

    @Transaction
    suspend fun markAcknowledged(id: String) {
        val msg = getById(id) ?: return
        update(msg.copy(status = org.beacon.sdk.model.DeliveryState.ACKNOWLEDGED.ordinal))
    }
}

@Dao
interface ResourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: ResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<ResourceEntity>)

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getById(id: String): ResourceEntity?

    @Query("SELECT * FROM resources WHERE type = :type AND expires_at > :now ORDER BY confidence DESC LIMIT :limit")
    suspend fun getByType(type: String, now: Long, limit: Int): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE (location_x - :lat) * (location_x - :lat) + (location_y - :lng) * (location_y - :lng) < :radiusSq AND expires_at > :now LIMIT :limit")
    suspend fun getNearby(lat: Double, lng: Double, radiusSq: Double, now: Long, limit: Int): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE expires_at < :now")
    suspend fun getExpiredResources(now: Long): List<ResourceEntity>

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM resources WHERE expires_at < :now")
    suspend fun cleanupExpiredResources(now: Long): Int
}

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getById(id: String): AlertEntity?

    @Query("SELECT * FROM alerts WHERE expires_at > :now AND severity >= :minSeverity ORDER BY expires_at ASC LIMIT :limit")
    suspend fun getActiveAlerts(now: Long, minSeverity: Int, limit: Int): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE area_north IS NOT NULL AND :lat BETWEEN area_south AND area_north AND :lng BETWEEN area_west AND area_east AND expires_at > :now")
    suspend fun getAlertsForLocation(lat: Double, lng: Double, now: Long): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE expires_at < :now")
    suspend fun getExpiredAlerts(now: Long): List<AlertEntity>

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM alerts WHERE expires_at < :now")
    suspend fun cleanupExpiredAlerts(now: Long): Int
}