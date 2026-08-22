package org.beacon.radio.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.beacon.core.model.PeerId
import org.beacon.core.model.TransportType
import org.beacon.radio.api.*
import org.beacon.sdk.model.Result
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BleTransport(
    private val context: Context,
    private val config: TransportConfig,
    private val meshEngine: org.beacon.mesh.MeshEngine // For peer updates
) : TransportApi, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    override val transportType = TransportType.BLE

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
    private val scanner = bluetoothAdapter.bluetoothLeScanner

    // BLE Constants
    private val BEACON_SERVICE_UUID = UUID.fromString("0000BEAC-0000-1000-8000-00805F9B34FB")
    private val BEACON_CHARACTERISTIC_UUID = UUID.fromString("0000BEAC-0001-1000-8000-00805F9B34FB")
    private val BEACON_CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Connection management
    private val gattConnections = ConcurrentHashMap<PeerId, BluetoothGatt>()
    private val connectionStates = ConcurrentHashMap<PeerId, ConnectionState>()
    private val pendingWrites = ConcurrentHashMap<PeerId, MutableList<ByteArray>>()

    // State
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Boolean get() = _isRunning.value

    private val _linkQuality = MutableStateFlow<Map<PeerId, LinkQuality>>(emptyMap())
    override val observeLinkQuality = _linkQuality.distinctUntilChanged().asStateFlow()

    private val _peerEvents = Channel<PeerEvent>(Channel.UNLIMITED)
    override val observePeerEvents = _peerEvents.receiveAsFlow()

    // Scan/Advertise callbacks
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising started")
            _peerEvents.trySend(PeerEvent(
                peerId = PeerId("local"),
                eventType = PeerEventType.DISCOVERED,
                transport = TransportType.BLE
            ))
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val device = gatt.device
            val peerId = PeerId(device.address.replace(":", "").uppercase())

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to $peerId")
                connectionStates[peerId] = ConnectionState.CONNECTED
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from $peerId: $status")
                connectionStates[peerId] = ConnectionState.DISCONNECTED
                gattConnections.remove(peerId)
                _peerEvents.trySend(PeerEvent(peerId, PeerEventType.DISCONNECTED, TransportType.BLE))
                flushPendingWrites(peerId) // Will fail pending writes
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val device = gatt.device
                val peerId = PeerId(device.address.replace(":", "").uppercase())
                
                Log.d(TAG, "Services discovered for $peerId")
                connectionStates[peerId] = ConnectionState.SERVICES_DISCOVERED

                val service = gatt.getService(BEACON_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BEACON_CHARACTERISTIC_UUID)
                
                if (characteristic != null) {
                    // Enable notifications
                    gatt.setCharacteristicNotification(characteristic, true)
                    val cccd = characteristic.getDescriptor(BEACON_CCCD_UUID)
                    cccd?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(cccd!!)
                    
                    connectionStates[peerId] = ConnectionState.READY
                    _peerEvents.trySend(PeerEvent(peerId, PeerEventType.CONNECTED, TransportType.BLE))
                    meshEngine.updateNeighbor(NeighborInfo(
                        peerId = peerId,
                        displayName = device.name,
                        lastSeen = Instant.now(),
                        batteryLevel = null,
                        powerMode = null,
                        transports = setOf(TransportType.BLE),
                        linkQuality = 0.5f,
                        signalStrength = null,
                        isOnline = true,
                        trustScore = 0.0f
                    ))
                    
                    // Flush any pending writes
                    flushPendingWrites(peerId)
                } else {
                    Log.e(TAG, "Beacon service/characteristic not found")
                    gatt.disconnect()
                }
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val device = gatt.device
            val peerId = PeerId(device.address.replace(":", "").uppercase())
            
            val payload = characteristic.value ?: byteArrayOf()
            val frame = BeaconFrame(
                payload = payload,
                destination = null, // Will be parsed from frame
                metadata = FrameMetadata()
            )
            
            // Deliver to mesh engine
            // TODO: Parse frame and deliver to mesh engine
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val device = gatt.device
            val peerId = PeerId(device.address.replace(":", "").uppercase())
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful to $peerId")
            } else {
                Log.e(TAG, "Write failed to $peerId: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful")
            } else {
                Log.e(TAG, "Descriptor write failed: $status")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val peerId = PeerId(gatt.device.address.replace(":", "").uppercase())
                updateLinkQuality(peerId, rssi)
            }
        }
    }

    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val peerId = PeerId(device.address.replace(":", "").uppercase())
        val rssi = result.rssi

        // Check if it's a Beacon device by service UUID
        val scanRecord = result.scanRecord
        val serviceUuids = scanRecord?.serviceUuids
        val isBeacon = serviceUuids?.any { it.uuid == BEACON_SERVICE_UUID } ?: false

        if (isBeacon) {
            _peerEvents.trySend(PeerEvent(
                peerId = peerId,
                eventType = PeerEventType.DISCOVERED,
                transport = TransportType.BLE,
                signalStrength = rssi
            ))

            // Auto-connect for mesh participation
            connectToPeer(peerId, device)
        }
    }

    private fun connectToPeer(peerId: PeerId, device: BluetoothDevice) {
        // Check if already connected or connecting
        val currentState = connectionStates[peerId] ?: ConnectionState.DISCONNECTED
        if (currentState != ConnectionState.DISCONNECTED) return

        launch {
            connectionStates[peerId] = ConnectionState.CONNECTING
            
            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            
            gattConnections[peerId] = gatt
        }
    }

    private fun updateLinkQuality(peerId: PeerId, rssi: Int) {
        val snr = (rssi + 100).toFloat() // Rough SNR estimate
        val packetLoss = if (rssi > -70) 0.0f else if (rssi > -85) 0.1f else if (rssi > -95) 0.3f else 0.6f
        val quality = when {
            rssi > -60 -> 1.0f
            rssi > -70 -> 0.8f
            rssi > -80 -> 0.6f
            rssi > -90 -> 0.4f
            else -> 0.2f
        }

        val linkQuality = LinkQuality(
            peerId = peerId,
            transport = TransportType.BLE,
            signalStrength = rssi,
            snr = snr,
            packetLossRate = packetLoss,
            bandwidthEstimate = 1000000, // 1 Mbps BLE
            latencyMs = if (rssi > -70) 20 else if (rssi > -85) 50 else 100,
            lastUpdate = System.currentTimeMillis()
        )

        _linkQuality.update { map -> map + (peerId to linkQuality) }
        meshEngine.updateNeighbor(NeighborInfo(
            peerId = peerId,
            displayName = null,
            lastSeen = Instant.now(),
            batteryLevel = null,
            powerMode = null,
            transports = setOf(TransportType.BLE),
            linkQuality = quality,
            signalStrength = rssi,
            isOnline = true,
            trustScore = 0.0f
        ))
    }

    private fun flushPendingWrites(peerId: PeerId) {
        val writes = pendingWrites.remove(peerId) ?: return
        writes.forEach { payload ->
            // Re-queue or fail
        }
    }

    override fun isAvailable(): Boolean = bluetoothAdapter.isEnabled

    override suspend fun start(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        if (!bluetoothAdapter.isEnabled) {
            return@withContext Failure("Bluetooth not enabled")
        }

        // Start advertising
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(BEACON_SERVICE_UUID))
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)

        // Start scanning
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BEACON_SERVICE_UUID))
            .build()

        scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)

        _isRunning.value = true
        Log.i(TAG, "BLE transport started")
        Success(Unit)
    }

    override suspend fun stop(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        advertiser.stopAdvertising(advertiseCallback)
        scanner.stopScan(scanCallback)

        gattConnections.values.forEach { it.close() }
        gattConnections.clear()
        connectionStates.clear()

        _isRunning.value = false
        Log.i(TAG, "BLE transport stopped")
        Success(Unit)
    }

    override suspend fun send(peerId: PeerId, frame: BeaconFrame): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        val gatt = gattConnections[peerId] ?: return@withContext Failure("Not connected to $peerId")
        val state = connectionStates[peerId] ?: ConnectionState.DISCONNECTED
        
        if (state != ConnectionState.READY) {
            // Queue for later
            pendingWrites.getOrPut(peerId) { mutableListOf() }.add(frame.payload)
            return@withContext Success(Unit)
        }

        val service = gatt.getService(BEACON_SERVICE_UUID) ?: return@withContext Failure("Service not found")
        val characteristic = service.getCharacteristic(BEACON_CHARACTERISTIC_UUID) ?: return@withContext Failure("Characteristic not found")

        characteristic.value = frame.payload
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        val success = gatt.writeCharacteristic(characteristic)
        if (!success) {
            return@withContext Failure("Write failed")
        }

        Success(Unit)
    }

    override fun receive(): ReceiveChannel<BeaconFrame> {
        // Frames are delivered via onCharacteristicChanged callback
        // This channel would be populated by the gattCallback
        return Channel(Channel.UNLIMITED)
    }

    override suspend fun getCapabilities(): Result<TransportCapabilities> = Success(TransportCapabilities(
        maxPayloadSize = 512,
        supportsBroadcast = true,
        supportsFragmentation = true,
        supportsAck = true,
        estimatedRangeMeters = 50.0,
        typicalLatencyMs = 50,
        powerConsumptionMw = 15.0
    ))

    override suspend fun configure(config: TransportConfig): Result<Unit> {
        // Update scan/advertise intervals based on power mode
        return Success(Unit)
    }

    companion object {
        private const val TAG = "BleTransport"
    }

    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, SERVICES_DISCOVERED, READY
    }
}