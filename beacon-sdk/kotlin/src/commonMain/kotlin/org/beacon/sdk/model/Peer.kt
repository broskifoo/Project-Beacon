package org.beacon.sdk.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    val id: PeerId,
    val displayName: String? = null,
    val lastSeen: Instant,
    var signalStrength: Int? = null,     // dBm
    var batteryLevel: Int? = null,       // 0-100
    var powerMode: PowerMode? = null,
    var transports: Set<TransportType> = emptySet(),
    var location: Location? = null,
    var metadata: Map<String, String> = emptyMap(),
    var isTrusted: Boolean = false,
    var trustScore: Float = 0.0f         // 0.0 - 1.0
) {
    fun isOnline(maxAgeSeconds: Long = 300): Boolean =
        Instant.now().epochSeconds - lastSeen.epochSeconds <= maxAgeSeconds
}

@Serializable
enum class TransportType {
    BLE,
    WIFI_DIRECT,
    LORA,
    BLUETOOTH_CLASSIC,
    CUSTOM
}

@Serializable
data class PeerDiscoveryEvent(
    val peer: Peer,
    val eventType: DiscoveryEventType,
    val timestamp: Instant = Instant.now()
)

@Serializable
enum class DiscoveryEventType {
    DISCOVERED,
    CONNECTED,
    DISCONNECTED,
    LOST,
    UPDATED
}

@Serializable
data class NetworkTopology(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val timestamp: Instant = Instant.now()
)

@Serializable
data class TopologyNode(
    val peerId: PeerId,
    val batteryLevel: Int?,
    val powerMode: PowerMode?,
    val isLocal: Boolean
)

@Serializable
data class TopologyEdge(
    val source: PeerId,
    val target: PeerId,
    val signalStrength: Int,           // dBm
    val transport: TransportType,
    var isActive: Boolean = true
)