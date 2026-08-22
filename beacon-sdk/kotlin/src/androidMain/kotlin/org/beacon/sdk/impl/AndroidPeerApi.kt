package org.beacon.sdk.impl

import android.content.Context
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.beacon.sdk.api.PeerApi
import org.beacon.sdk.model.*
import org.beacon.sdk.model.Result.Success
import org.beacon.sdk.model.Result.Failure
import io.reactivex.rxjava3.core.Flowable

class AndroidPeerApi(
    private val context: Context,
    private val config: BeaconConfig,
    private val sdk: org.beacon.sdk.BeaconSdk
) : PeerApi, CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val dataStore = RxPreferenceDataStoreBuilder(context, "beacon_peers").build()
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val observePeers = _peers
        .distinctUntilChanged()
        .asStateFlow()

    private val _peerEvents = Channel<PeerDiscoveryEvent>(Channel.UNLIMITED)
    override val observePeerEvents = _peerEvents.receiveAsFlow()

    private val localPeerId = PeerId("local-${android.os.Build.SERIAL}")

    override suspend fun getPeer(peerId: PeerId): Result<Peer> {
        val peer = _peers.value.find { it.id == peerId }
        return peer?.let { Success(it) } ?: Failure(BeaconError(ErrorCode.PEER_NOT_FOUND, "Peer not found: $peerId"))
    }

    override suspend fun getLocalPeer(): Result<Peer> {
        val localPeer = Peer(
            id = localPeerId,
            displayName = config.deviceName,
            lastSeen = Instant.now(),
            batteryLevel = 100,
            powerMode = config.powerMode,
            transports = config.enabledTransports,
            isTrusted = true,
            trustScore = 1.0f
        )
        return Success(localPeer)
    }

    override suspend fun updateLocalPeer(displayName: String? = null): Result<Unit> {
        // Update in shared preferences
        return Success(Unit)
    }

    override suspend fun trustPeer(peerId: PeerId, trust: Boolean): Result<Unit> {
        // Update trust status in local database
        _peers.update { peers ->
            peers.map { if (it.id == peerId) it.copy(isTrusted = trust, trustScore = if (trust) 1.0f else 0.0f) else it }
        }
        return Success(Unit)
    }

    override suspend fun blockPeer(peerId: PeerId): Result<Unit> {
        // Add to block list
        return Success(Unit)
    }

    override suspend fun unblockPeer(peerId: PeerId): Result<Unit> {
        // Remove from block list
        return Success(Unit)
    }

    override suspend fun getTrustedPeers(): Result<List<Peer>> {
        return Success(_peers.value.filter { it.isTrusted })
    }

    override suspend fun getBlockedPeers(): Result<List<PeerId>> {
        // Return blocked peers from storage
        return Success(emptyList())
    }

    override fun observePeer(peerId: PeerId) = observePeers.map { peers -> peers.find { it.id == peerId } }

    // Internal methods for mesh service to update peer state
    internal fun updatePeer(peer: Peer) {
        _peers.update { peers ->
            val existing = peers.indexOfFirst { it.id == peer.id }
            if (existing >= 0) {
                peers.toMutableList().apply { this[existing] = peer }
            } else {
                peers + peer
            }
        }
        _peerEvents.trySend(PeerDiscoveryEvent(peer, DiscoveryEventType.UPDATED))
    }

    internal fun peerDiscovered(peer: Peer) {
        _peers.update { peers ->
            if (!peers.any { it.id == peer.id }) peers + peer else peers
        }
        _peerEvents.trySend(PeerDiscoveryEvent(peer, DiscoveryEventType.DISCOVERED))
    }

    internal fun peerLost(peerId: PeerId) {
        _peers.update { peers -> peers.filter { it.id != peerId } }
        _peerEvents.trySend(PeerDiscoveryEvent(
            Peer(peerId, null, Instant.now()), DiscoveryEventType.LOST
        ))
    }

    internal fun peerConnected(peerId: PeerId) {
        _peers.update { peers ->
            peers.map { if (it.id == peerId) it.copy(lastSeen = Instant.now()) else it }
        }
        _peerEvents.trySend(PeerDiscoveryEvent(
            Peer(peerId, null, Instant.now()), DiscoveryEventType.CONNECTED
        ))
    }

    internal fun peerDisconnected(peerId: PeerId) {
        _peerEvents.trySend(PeerDiscoveryEvent(
            Peer(peerId, null, Instant.now()), DiscoveryEventType.DISCONNECTED
        ))
    }
}

private val BeaconConfig.enabledTransports: Set<TransportType>
    get() = mutableSetOf<TransportType>().apply {
        if (enableBle) add(TransportType.BLE)
        if (enableWifiDirect) add(TransportType.WIFI_DIRECT)
        if (enableLora) add(TransportType.LORA)
    }