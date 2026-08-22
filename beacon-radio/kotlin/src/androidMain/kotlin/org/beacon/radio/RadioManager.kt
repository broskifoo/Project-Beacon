package org.beacon.radio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.beacon.mesh.MeshEngine
import org.beacon.radio.api.*
import org.beacon.radio.impl.BleTransport
import org.beacon.radio.impl.WifiDirectTransport
import org.beacon.sdk.model.Result
import org.beacon.sdk.model.Result.Success

class RadioManager(
    private val context: Context,
    private val config: RadioConfig,
    private val meshEngine: MeshEngine
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val transports = mutableMapOf<TransportType, TransportApi>()
    private val _activeTransports = MutableStateFlow<Set<TransportType>>(emptySet())
    val activeTransports = _activeTransports.asStateFlow()

    init {
        // Initialize transports
        if (config.enableBle) {
            transports[TransportType.BLE] = BleTransport(context, config.bleConfig, meshEngine)
        }
        if (config.enableWifiDirect) {
            transports[TransportType.WIFI_DIRECT] = WifiDirectTransport(context, config.wifiConfig, meshEngine)
        }
        // LoRa transport would be added here when hardware is available
    }

    suspend fun start(): Result<Unit> {
        var allSuccess = true
        val startedTransports = mutableSetOf<TransportType>()

        for ((type, transport) in transports) {
            if (transport.isAvailable()) {
                transport.start().onSuccess {
                    startedTransports.add(type)
                }.onFailure { error ->
                    allSuccess = false
                    android.util.Log.e("RadioManager", "Failed to start $type: ${error.message}")
                }
            }
        }

        _activeTransports.value = startedTransports
        return if (allSuccess) Success(Unit) else Success(Unit) // Partial success is OK
    }

    suspend fun stop(): Result<Unit> {
        for (transport in transports.values) {
            transport.stop()
        }
        _activeTransports.value = emptySet()
        return Success(Unit)
    }

    fun getTransport(type: TransportType): TransportApi? = transports[type]

    fun getAllTransports(): List<TransportApi> = transports.values.toList()

    suspend fun sendToAll(frame: BeaconFrame): Result<Unit> {
        val results = transports.values.map { it.send(frame.destination ?: PeerId("broadcast"), frame) }
        return if (results.all { it is Result.Success }) Success(Unit) else Success(Unit)
    }
}

data class RadioConfig(
    val enableBle: Boolean = true,
    val enableWifiDirect: Boolean = true,
    val enableLora: Boolean = false,
    val bleConfig: TransportConfig = TransportConfig(),
    val wifiConfig: TransportConfig = TransportConfig()
)

data class TransportConfig(
    val enabled: Boolean = true,
    val scanIntervalMs: Int = 2000,
    val scanWindowMs: Int = 200,
    val advertiseIntervalMs: Int = 500,
    val txPowerLevel: Int = 0,
    val priority: TransportPriority = TransportPriority.NORMAL
)

enum class TransportPriority {
    LOW, NORMAL, HIGH
}