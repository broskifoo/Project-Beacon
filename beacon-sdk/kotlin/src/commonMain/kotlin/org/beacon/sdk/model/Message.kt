package org.beacon.sdk.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class MessageId(
    val value: String
) {
    companion object {
        fun random(): MessageId = MessageId(java.util.UUID.randomUUID().toString())
    }
}

@Serializable
data class PeerId(
    val value: String
) {
    companion object {
        fun fromPublicKey(publicKeyHex: String): PeerId = PeerId(publicKeyHex)
    }
}

@Serializable
enum class MessagePriority {
    CRITICAL,   // SOS, medical emergency, immediate danger
    HIGH,       // Medical request, shelter request, resource shortage
    NORMAL,     // Family message, coordination message
    LOW         // News, non-critical updates, large files
}

@Serializable
enum class MessageType {
    TEXT,
    LOCATION,
    TELEMETRY,
    SOS,
    ACKNOWLEDGMENT,
    RESOURCE_REPORT,
    ALERT,
    MAP_TILE,
    VOICE_NOTE,
    IMAGE,
    CUSTOM
}

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Double? = null,
    val timestamp: Instant = Instant.now()
)

@Serializable
data class Telemetry(
    val batteryLevel: Int,           // 0-100
    val powerMode: PowerMode,
    val signalStrength: Int? = null, // dBm
    val temperature: Double? = null, // Celsius
    val customMetrics: Map<String, String> = emptyMap()
)

@Serializable
enum class PowerMode {
    NORMAL,
    CONSERVATION,
    SURVIVAL,
    CRITICAL
}

@Serializable
data class MessagePayload(
    val type: MessageType,
    val text: String? = null,
    val location: Location? = null,
    val telemetry: Telemetry? = null,
    val resourceReport: ResourceReport? = null,
    val alert: Alert? = null,
    val binaryData: ByteArray? = null,
    val custom: Map<String, String>? = null
)

@Serializable
data class ResourceReport(
    val resourceType: ResourceType,
    val location: Location,
    val description: String,
    val confidence: Float,           // 0.0 - 1.0
    val severity: Severity,
    val expiresAt: Instant,
    val reporterId: PeerId
)

@Serializable
enum class ResourceType {
    WATER,
    FOOD,
    MEDICAL,
    SHELTER,
    CHARGING,
    HAZARD,
    ROAD_CLOSED,
    EVACUATION_ROUTE,
    CUSTOM
}

@Serializable
enum class Severity {
    INFO,
    WARNING,
    CRITICAL
}

@Serializable
data class Alert(
    val alertType: AlertType,
    val title: String,
    val message: String,
    val severity: Severity,
    val area: GeoBounds? = null,
    val expiresAt: Instant,
    val issuerId: PeerId
)

@Serializable
enum class AlertType {
    EVACUATION,
    BOIL_WATER,
    ROAD_CLOSURE,
    WEATHER,
    SECURITY,
    GENERAL
}

@Serializable
data class GeoBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

@Serializable
data class Message(
    val id: MessageId,
    val senderId: PeerId,
    val recipientId: PeerId?,           // null = broadcast
    val timestamp: Instant,
    val priority: MessagePriority,
    val ttl: Int,                       // Time-to-live in hops
    val hopCount: Int = 0,
    val payload: MessagePayload,
    val signature: String,              // Ed25519 signature (hex)
    val nonce: String                   // Unique nonce for replay protection
) {
    fun isExpired(maxHops: Int = 10): Boolean = hopCount >= maxHops
    fun isCritical(): Boolean = priority == MessagePriority.CRITICAL
    fun isBroadcast(): Boolean = recipientId == null
}

@Serializable
data class MessageStatus(
    val messageId: MessageId,
    val state: DeliveryState,
    val timestamp: Instant,
    val hopCount: Int = 0,
    val lastPeerId: PeerId? = null,
    val error: String? = null
)

@Serializable
enum class DeliveryState {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    ACKNOWLEDGED,
    FAILED,
    EXPIRED
}