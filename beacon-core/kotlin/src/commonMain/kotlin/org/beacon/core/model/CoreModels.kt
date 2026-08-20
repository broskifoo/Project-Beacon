package org.beacon.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.beacon.sdk.model.MessagePriority
import org.beacon.sdk.model.MessageType
import org.beacon.sdk.model.PeerId

@Serializable
data class Bundle(
    val id: BundleId,
    val source: PeerId,
    val destination: PeerId?,
    val creationTimestamp: Instant,
    val expirationTimestamp: Instant,
    val priority: MessagePriority,
    val payload: ByteArray,           // Encrypted application payload
    val routingFlags: RoutingFlags = RoutingFlags(),
    val hopCount: Int = 0,
    val maxHops: Int = 5,
    val custodyTransfers: List<CustodyTransfer> = emptyList()
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now >= expirationTimestamp
    fun isDelivered: Boolean = destination != null && hopCount > 0
}

@Serializable
data class BundleId(
    val value: String
) {
    companion object {
        fun random(): BundleId = BundleId(java.util.UUID.randomUUID().toString())
    }
}

@Serializable
data class RoutingFlags(
    val isFragment: Boolean = false,
    val fragmentIndex: Int = 0,
    val fragmentCount: Int = 1,
    val requestAck: Boolean = true,
    val reportDelivery: Boolean = false
)

@Serializable
data class CustodyTransfer(
    val nodeId: PeerId,
    val timestamp: Instant,
    val accepted: Boolean
)

@Serializable
data class NodeInfo(
    val peerId: PeerId,
    val displayName: String?,
    val lastSeen: Instant,
    val location: Location?,
    val batteryLevel: Int?,
    val powerMode: PowerMode?,
    val transports: Set<TransportType>,
    var signalStrength: Int? = null,
    var isOnline: Boolean = true,
    var trustScore: Float = 0.0f
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Double? = null,
    val timestamp: Instant = Instant.now()
)

@Serializable
enum class PowerMode {
    NORMAL, CONSERVATION, SURVIVAL, CRITICAL
}

@Serializable
enum class TransportType {
    BLE, WIFI_DIRECT, LORA, BLUETOOTH_CLASSIC
}

@Serializable
data class NetworkTopology(
    val nodes: Map<PeerId, NodeInfo>,
    val links: List<Link>,
    val timestamp: Instant = Instant.now()
)

@Serializable
data class Link(
    val source: PeerId,
    val target: PeerId,
    val transport: TransportType,
    val signalStrength: Int,      // dBm
    val quality: Float,           // 0.0 - 1.0
    var isActive: Boolean = true,
    val lastUpdate: Instant = Instant.now()
)

@Serializable
data class RoutingTableEntry(
    val destination: PeerId,
    val nextHop: PeerId,
    val hopCount: Int,
    val quality: Float,
    val lastUpdate: Instant,
    val routeType: RouteType
)

enum class RouteType {
    DIRECT, GEOGRAPHIC, EPIDEMIC, MANUAL
}