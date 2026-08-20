package org.beacon.core.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import org.beacon.core.model.Bundle
import org.beacon.core.model.BundleId
import org.beacon.core.model.MessagePriority
import org.beacon.core.model.PeerId
import org.beacon.core.model.TransportType

@Entity(
    tableName = "bundles",
    indices = [
        Index(value = ["destination"]),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["source"]),
    ]
)
data class BundleEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "destination") val destination: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "payload") val payload: ByteArray,
    @ColumnInfo(name = "hop_count") val hopCount: Int = 0,
    @ColumnInfo(name = "max_hops") val maxHops: Int = 5,
    @ColumnInfo(name = "status") val status: Int = BundleStatus.OUTBOX.ordinal,
    @ColumnInfo(name = "custody_holder") val custodyHolder: String? = null,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "last_retry") val lastRetry: Long = 0
) {
    fun toBundle(): Bundle {
        return Bundle(
            id = BundleId(id),
            source = PeerId(source),
            destination = destination?.let { PeerId(it) },
            creationTimestamp = Instant.ofEpochMillisecond(createdAt),
            expirationTimestamp = Instant.ofEpochMillisecond(expiresAt),
            priority = MessagePriority.values()[priority],
            payload = payload,
            hopCount = hopCount,
            maxHops = maxHops
        )
    }

    companion object {
        fun fromBundle(bundle: Bundle): BundleEntity {
            return BundleEntity(
                id = bundle.id.value,
                source = bundle.source.value,
                destination = bundle.destination?.value,
                createdAt = bundle.creationTimestamp.toEpochMilliseconds(),
                expiresAt = bundle.expirationTimestamp.toEpochMilliseconds(),
                priority = bundle.priority.ordinal,
                payload = bundle.payload,
                hopCount = bundle.hopCount,
                maxHops = bundle.maxHops,
                status = bundle.status.ordinal
            )
        }
    }
}

enum class BundleStatus {
    OUTBOX, IN_TRANSIT, INBOX, DELIVERED, ACKNOWLEDGED, EXPIRED, FAILED
}

@Entity(
    tableName = "peers",
    indices = [Index(value = ["last_seen"]), Index(value = ["is_online"])]
)
data class PeerEntity(
    @PrimaryKey @ColumnInfo(name = "peer_id") val peerId: String,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    @ColumnInfo(name = "location_x") val locationX: Double? = null,
    @ColumnInfo(name = "location_y") val locationY: Double? = null,
    @ColumnInfo(name = "battery_level") val batteryLevel: Int? = null,
    @ColumnInfo(name = "power_mode") val powerMode: Int? = null,
    @ColumnInfo(name = "transports") val transports: String? = null, // JSON
    @ColumnInfo(name = "signal_strength") val signalStrength: Int? = null,
    @ColumnInfo(name = "is_online") val isOnline: Boolean = false,
    @ColumnInfo(name = "trust_score") val trustScore: Float = 0.0f,
    @ColumnInfo(name = "is_trusted") val isTrusted: Boolean = false,
    @ColumnInfo(name = "is_blocked") val isBlocked: Boolean = false
)

@Entity(
    tableName = "routes",
    indices = [Index(value = ["destination"]), Index(value = ["next_hop"])]
)
data class RouteEntity(
    @PrimaryKey @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "next_hop") val nextHop: String,
    @ColumnInfo(name = "hop_count") val hopCount: Int,
    @ColumnInfo(name = "quality") val quality: Float,
    @ColumnInfo(name = "route_type") val routeType: Int,
    @ColumnInfo(name = "last_update") val lastUpdate: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "success_count") val successCount: Long = 0,
    @ColumnInfo(name = "failure_count") val failureCount: Long = 0,
    @ColumnInfo(name = "avg_latency_ms") val avgLatencyMs: Double = 0.0,
    @ColumnInfo(name = "avg_hop_count") val avgHopCount: Float = 0.0f
)

@Entity(
    tableName = "pois",
    indices = [
        Index(value = ["category"]),
        Index(value = ["location_x", "location_y"]),
        Index(value = ["expires_at"]),
        Index(value = ["source"]),
    ]
)
data class PoiEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "location_x") val locationX: Double,
    @ColumnInfo(name = "location_y") val locationY: Double,
    @ColumnInfo(name = "properties") val properties: String? = null, // JSON
    @ColumnInfo(name = "confidence") val confidence: Float = 1.0f,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long? = null,
    @ColumnInfo(name = "reporter_pubkey") val reporterPubkey: String? = null,
    @ColumnInfo(name = "signature") val signature: ByteArray? = null
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["sender_id"]),
        Index(value = ["recipient_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
    ]
)
data class MessageEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "recipient_id") val recipientId: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "ttl") val ttl: Int,
    @ColumnInfo(name = "hop_count") val hopCount: Int = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "text") val text: String? = null,
    @ColumnInfo(name = "location_x") val locationX: Double? = null,
    @ColumnInfo(name = "location_y") val locationY: Double? = null,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "signature") val signature: ByteArray? = null,
    @ColumnInfo(name = "nonce") val nonce: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "delivered_at") val deliveredAt: Long? = null,
    @ColumnInfo(name = "acked_at") val ackedAt: Long? = null
)

@Entity(
    tableName = "resources",
    indices = [
        Index(value = ["type"]),
        Index(value = ["location_x", "location_y"]),
        Index(value = ["expires_at"]),
    ]
)
data class ResourceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "location_x") val locationX: Double,
    @ColumnInfo(name = "location_y") val locationY: Double,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "severity") val severity: Int,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "reporter_pubkey") val reporterPubkey: String? = null,
    @ColumnInfo(name = "signature") val signature: ByteArray? = null
)

@Entity(
    tableName = "alerts",
    indices = [
        Index(value = ["type"]),
        Index(value = ["severity"]),
        Index(value = ["expires_at"]),
        Index(value = ["area_north", "area_south", "area_east", "area_west"]),
    ]
)
data class AlertEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "severity") val severity: Int,
    @ColumnInfo(name = "area_north") val areaNorth: Double? = null,
    @ColumnInfo(name = "area_south") val areaSouth: Double? = null,
    @ColumnInfo(name = "area_east") val areaEast: Double? = null,
    @ColumnInfo(name = "area_west") val areaWest: Double? = null,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "issuer_pubkey") val issuerPubkey: String? = null,
    @ColumnInfo(name = "signature") val signature: ByteArray? = null
)