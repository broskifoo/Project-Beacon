package org.beacon.radio.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
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

class WifiDirectTransport(
    private val context: Context,
    private val config: TransportConfig,
    private val meshEngine: org.beacon.mesh.MeshEngine
) : TransportApi, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    override val transportType = TransportType.WIFI_DIRECT

    private val TAG = "WifiDirectTransport"
    private val SERVICE_NAME = "beacon"
    private val SERVICE_TYPE = "_beacon._tcp"

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val p2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val p2pChannel = p2pManager.initialize(context, context.mainLooper, null)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Boolean get() = _isRunning.value

    private val _linkQuality = MutableStateFlow<Map<PeerId, LinkQuality>>(emptyMap())
    override val observeLinkQuality = _linkQuality.distinctUntilChanged().asStateFlow()

    private val _peerEvents = Channel<PeerEvent>(Channel.UNLIMITED)
    override val observePeerEvents = _peerEvents.receiveAsFlow()

    private val discoveredDevices = mutableMapOf<String, WifiP2pDevice>()
    private val connectedDevices = mutableMapOf<String, WifiP2pInfo>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    Log.d(TAG, "Wi-Fi P2P state changed: $state")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        p2pManager.requestConnectionInfo(p2pChannel) { info ->
                            onConnectionInfoAvailable(info)
                        }
                    } else {
                        Log.d(TAG, "P2P disconnected")
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    device?.let { Log.d(TAG, "This device changed: ${it.deviceName}") }
                }
                WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                    val isDiscoveryStarted = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_CHANGED, 0) == 1
                    Log.d(TAG, "Discovery ${if (isDiscoveryStarted) "started" else "stopped"}")
                }
            }
        }
    }

    override fun isAvailable(): Boolean = wifiManager.isWifiEnabled

    override suspend fun start(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        if (!wifiManager.isWifiEnabled) {
            return@withContext Failure("Wi-Fi not enabled")
        }

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
        context.registerReceiver(broadcastReceiver, filter)

        // Register local service
        registerLocalService()

        // Start discovery
        discoverPeers()

        _isRunning.value = true
        Log.i(TAG, "Wi-Fi Direct transport started")
        Success(Unit)
    }

    override suspend fun stop(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        // Stop discovery
        p2pManager.stopPeerDiscovery(p2pChannel) { }

        // Remove service
        p2pManager.clearLocalServices(p2pChannel) { }

        // Close connections
        // TODO: Close active socket connections

        // Unregister receiver
        context.unregisterReceiver(broadcastReceiver)

        _isRunning.value = false
        Log.i(TAG, "Wi-Fi Direct transport stopped")
        Success(Unit)
    }

    private fun registerLocalService() {
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_NAME,
            SERVICE_TYPE,
            mapOf("version" to "1.0")
        )
        p2pManager.addLocalService(p2pChannel, serviceInfo) { success ->
            Log.d(TAG, "Service registration ${if (success) "successful" else "failed"}")
        }

        // Set up service request
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        p2pManager.addServiceRequest(p2pChannel, serviceRequest) { success ->
            Log.d(TAG, "Service request ${if (success) "registered" else "failed"}")
        }
    }

    private fun discoverPeers() {
        p2pManager.discoverPeers(p2pChannel) { success ->
            Log.d(TAG, "Peer discovery ${if (success) "started" else "failed"}")
        }
    }

    private fun requestPeers() {
        p2pManager.requestPeers(p2pChannel) { peerList ->
            peerList.deviceList.forEach { device ->
                if (!discoveredDevices.containsKey(device.deviceAddress)) {
                    discoveredDevices[device.deviceAddress] = device
                    val peerId = PeerId(device.deviceAddress.replace(":", "").uppercase())
                    _peerEvents.trySend(PeerEvent(
                        peerId = peerId,
                        eventType = PeerEventType.DISCOVERED,
                        transport = TransportType.WIFI_DIRECT
                    ))

                    // Auto-connect for mesh participation
                    connectToDevice(device)
                }
            }
        }
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        p2pManager.connect(p2pChannel, config) { success ->
            Log.d(TAG, "Connection to ${device.deviceName} ${if (success) "initiated" else "failed"}")
        }
    }

    private fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.groupFormed) {
            val peerId = PeerId(info.groupOwnerAddress.hostAddress)
            connectedDevices[peerId.value] = info

            _peerEvents.trySend(PeerEvent(
                peerId = peerId,
                eventType = PeerEventType.CONNECTED,
                transport = TransportType.WIFI_DIRECT
            ))

            meshEngine.updateNeighbor(NeighborInfo(
                peerId = peerId,
                displayName = info.groupOwnerAddress.hostAddress,
                lastSeen = Instant.now(),
                transports = setOf(TransportType.WIFI_DIRECT),
                linkQuality = 0.8f,
                signalStrength = -40,
                isOnline = true,
                trustScore = 0.0f
            ))

            // Start TCP server/client for data transfer
            if (info.isGroupOwner) {
                startServer()
            } else {
                connectToServer(info.groupOwnerAddress.hostAddress)
            }
        }
    }

    private fun startServer() {
        launch {
            // TODO: Start TCP server for incoming connections
            Log.d(TAG, "Starting Wi-Fi Direct server")
        }
    }

    private fun connectToServer(host: String) {
        launch {
            // TODO: Connect to group owner's TCP server
            Log.d(TAG, "Connecting to Wi-Fi Direct server at $host")
        }
    }

    override suspend fun send(peerId: PeerId, frame: BeaconFrame): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        // TODO: Send via TCP socket
        // This would use the established TCP connection to the peer
        return Success(Unit)
    }

    override fun receive(): ReceiveChannel<BeaconFrame> {
        return Channel(Channel.UNLIMITED)
    }

    override suspend fun getCapabilities(): Result<TransportCapabilities> = Success(TransportCapabilities(
        maxPayloadSize = 65536,
        supportsBroadcast = false,
        supportsFragmentation = true,
        supportsAck = true,
        estimatedRangeMeters = 200.0,
        typicalLatencyMs = 20,
        powerConsumptionMw = 500.0
    ))

    override suspend fun configure(config: TransportConfig): Result<Unit> {
        return Success(Unit)
    }

    override fun isAvailable(): Boolean = wifiManager.isWifiEnabled && p2pManager != null

    override suspend fun start(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        if (!wifiManager.isWifiEnabled) {
            return@withContext Failure("Wi-Fi not enabled")
        }

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
        context.registerReceiver(broadcastReceiver, filter)

        registerLocalService()
        discoverPeers()

        _isRunning.value = true
        Success(Unit)
    }

    override suspend fun stop(): Result<Unit> = coroutineContext.withContext(Dispatchers.IO) {
        p2pManager.stopPeerDiscovery(p2pChannel) { }
        p2pManager.clearLocalServices(p2pChannel) { }
        context.unregisterReceiver(broadcastReceiver)
        _isRunning.value = false
        Success(Unit)
    }

    override fun observeLinkQuality = _linkQuality.distinctUntilChanged().asStateFlow()

    override fun observePeerEvents = _peerEvents.receiveAsFlow()

    companion object {
        private const val TAG = "WifiDirectTransport"
    }
}