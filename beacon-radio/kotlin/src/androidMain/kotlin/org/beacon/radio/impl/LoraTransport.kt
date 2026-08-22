package org.beacon.radio.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.beacon.core.model.PeerId
import org.beacon.core.model.TransportType
import org.beacon.radio.api.*
import org.beacon.sdk.model.Result
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure
import java.util.UUID

class LoraTransport(
    private val context: Context,
    private val config: TransportConfig,
    private val meshEngine: org.beacon.mesh.MeshEngine
) : TransportApi, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    override val transportType = TransportType.LORA

    private val TAG = "LoraTransport"
    private val LORA_SERVICE_UUID = UUID.fromString("0000LORA-0000-1000-8000-00805F9B34FB")
    private val LORA_TX_CHAR_UUID = UUID.fromString("0000LORA-0001-1000-8000-00805F9B34FB")
    private val LORA_RX_CHAR_UUID = UUID.fromString("0000LORA-0002-1000-1000-80005F9B34FB")
    private val LORA_CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Boolean get() = _isRunning.value

    private val _linkQuality = MutableStateFlow<Map<PeerId, LinkQuality>>(emptyMap())
    override val observeLinkQuality = _linkQuality.distinctUntilChanged().asStateFlow()

    private val _peerEvents = Channel<PeerEvent>(Channel.UNLIMITED)
    override val observePeerEvents = _peerEvents.receiveAsFlow()

    private var loraGatt: BluetoothGatt? = null
    private val connectedRadios = mutableMapOf<PeerId, BluetoothGatt>()

    override fun isAvailable(): Boolean = bluetoothAdapter.isEnabled

    override suspend fun start(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        if (!bluetoothAdapter.isEnabled) {
            return@withContext Failure("Bluetooth not enabled")
        }

        // Scan for LoRa radio devices
        // In a real implementation, this would connect to ESP32/LoRa hardware via Bluetooth
        Log.w(TAG, "LoRa transport not fully implemented - requires external hardware")
        _isRunning.value = true
        Success(Unit)
    }

    override suspend fun stop(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        loraGatt?.close()
        connectedRadios.values.forEach { it.close() }
        connectedRadios.clear()
        _isRunning.value = false
        Success(Unit)
    }

    override suspend fun send(peerId: PeerId, frame: BeaconFrame): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        // Send to external LoRa radio via Bluetooth
        val gatt = connectedRadios[peerId] ?: return@withContext Failure("LoRa radio not connected")
        
        val service = gatt.getService(LORA_SERVICE_UUID) ?: return@withContext Failure("LoRa service not found")
        val characteristic = service.getCharacteristic(LORA_TX_CHAR_UUID) ?: return@withContext Failure("TX characteristic not found")
        
        characteristic.value = frame.payload
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        
        val success = gatt.writeCharacteristic(characteristic)
        if (!success) {
            return@withContext Failure("Write failed")
        }
        
        Success(Unit)
    }

    override fun receive(): ReceiveChannel<BeaconFrame> {
        return Channel(Channel.UNLIMITED)
    }

    override suspend fun getCapabilities(): Result<TransportCapabilities> = Success(TransportCapabilities(
        maxPayloadSize = 255,
        supportsBroadcast = true,
        supportsFragmentation = true,
        supportsAck = true,
        estimatedRangeMeters = 5000.0,
        typicalLatencyMs = 200,
        powerConsumptionMw = 100.0
    ))

    override suspend fun configure(config: TransportConfig): Result<Unit> {
        return Success(Unit)
    }

    // Call when external radio connects via Bluetooth
    internal fun onRadioConnected(gatt: BluetoothGatt) {
        launch {
            loraGatt = gatt
            gatt.discoverServices()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Remove disconnected radio
                connectedRadios.values.firstOrNull { it == gatt }?.let { peerId ->
                    connectedRadios.remove(peerId)
                    meshEngine.removeNeighbor(peerId)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(LORA_SERVICE_UUID)
                val txChar = service?.getCharacteristic(LORA_TX_CHAR_UUID)
                val rxChar = service?.getCharacteristic(LORA_RX_CHAR_UUID)
                
                rxChar?.let {
                    gatt.setCharacteristicNotification(it, true)
                    val cccd = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))
                    cccd?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(cccd!!)
                }
                
                // Extract peer ID from device
                val peerId = PeerId(gatt.device.address.replace(":", "").uppercase())
                connectedRadios[peerId] = gatt
                
                _peerEvents.trySend(PeerEvent(
                    peerId = peerId,
                    eventType = PeerEventType.CONNECTED,
                    transport = TransportType.LORA
                ))
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val payload = characteristic.value ?: byteArrayOf()
            val frame = BeaconFrame(
                payload = payload,
                destination = null,
                metadata = FrameMetadata()
            )
            // Deliver to mesh engine
            // TODO: Parse and deliver
        }
    }
}