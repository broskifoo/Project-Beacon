package org.beacon.radio.api

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import org.beacon.core.model.PeerId
import org.beacon.core.model.TransportType
import org.beacon.sdk.model.Result

interface TransportApi {
    val transportType: TransportType
    
    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    
    fun isAvailable(): Boolean
    fun isRunning(): Boolean
    
    suspend fun send(peerId: PeerId, frame: BeaconFrame): Result<Unit>
    fun receive(): ReceiveChannel<BeaconFrame>
    
    fun observeLinkQuality(peerId: PeerId): Flow<LinkQuality>
    fun observePeerEvents(): Flow<PeerEvent>
    
    suspend fun getCapabilities(): Result<TransportCapabilities>
    suspend fun configure(config: TransportConfig): Result<Unit>
}

@Serializable
data class BeaconFrame(
    val payload: ByteArray,
    val destination: PeerId?,
    val priority: FramePriority = FramePriority.NORMAL,
    val metadata: FrameMetadata = FrameMetadata()
)

enum class FramePriority {
    LOW, NORMAL, HIGH, CRITICAL
}

@Serializable
data class FrameMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val fragmentIndex: Int = 0,
    val fragmentCount: Int = 1,
    val requiresAck: Boolean = true
)

@Serializable
data class LinkQuality(
    val peerId: PeerId,
    val transport: TransportType,
    val signalStrength: Int,      // dBm
    val snr: Float,               // dB
    val packetLossRate: Float,    // 0.0 - 1.0
    var bandwidthEstimate: Long = 0, // bps
    var latencyMs: Long = 0,
    var lastUpdate: Long = System.currentTimeMillis()
) {
    fun qualityScore(): Float {
        val signalNorm = (signalStrength + 100).coerceIn(0, 70) / 70f
        val lossScore = 1f - packetLossRate
        return (signalNorm + lossScore) / 2f
    }
}

@Serializable
enum class PeerEventType {
    DISCOVERED, CONNECTED, DISCONNECTED, LOST, QUALITY_CHANGED
}

@Serializable
data class PeerEvent(
    val peerId: PeerId,
    val eventType: PeerEventType,
    val transport: TransportType,
    val signalStrength: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TransportCapabilities(
    val maxPayloadSize: Int,
    val supportsBroadcast: Boolean,
    val supportsFragmentation: Boolean,
    val supportsAck: Boolean,
    val estimatedRangeMeters: Double,
    val typicalLatencyMs: Long,
    val powerConsumptionMw: Double
)

@Serializable
data class TransportConfig(
    val enabled: Boolean = true,
    val scanIntervalMs: Int = 2000,
    val scanWindowMs: Int = 200,
    val advertiseIntervalMs: Int = 500,
    val txPowerLevel: Int = 0, // dBm, 0 = default
    val priority: TransportPriority = TransportPriority.NORMAL
)

enum class TransportPriority {
    LOW, NORMAL, HIGH
}

interface RadioDriver {
    val transportType: TransportType
    
    suspend fun initialize(context: Any): Result<Unit>
    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    
    suspend fun send(peerId: PeerId, data: ByteArray): Result<Unit>
    fun receive(): ReceiveChannel<ByteArray>
    
    fun observeEvents(): Flow<DriverEvent>
    
    suspend fun getCapabilities(): Result<DriverCapabilities>
    suspend fun setConfig(config: DriverConfig): Result<Unit>
}

@Serializable
data class DriverEvent(
    val eventType: DriverEventType,
    val peerId: PeerId?,
    val data: ByteArray? = null,
    val signalStrength: Int? = null,
    val error: String? = null
)

enum class DriverEventType {
    PEER_DISCOVERED, PEER_CONNECTED, PEER_DISCONNECTED,
    DATA_RECEIVED, DATA_SENT, ERROR, STATE_CHANGED
}

@Serializable
data class DriverCapabilities(
    val maxPayloadSize: Int,
    val supportsBroadcast: Boolean,
    val supportsMulticast: Boolean,
    val supportsEncryption: Boolean,
    val supportedDataRates: List<Long>
)

@Serializable
data class DriverConfig(
    val enabled: Boolean = true,
    val powerMode: DriverPowerMode = DriverPowerMode.NORMAL,
    val customParams: Map<String, String> = emptyMap()
)

enum class DriverPowerMode {
    LOW, NORMAL, HIGH
}