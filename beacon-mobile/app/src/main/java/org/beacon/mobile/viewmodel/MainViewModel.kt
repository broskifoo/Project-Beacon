package org.beacon.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.beacon.mobile.BeaconApplication
import org.beacon.sdk.model.Message
import org.beacon.sdk.model.MessagePayload
import org.beacon.sdk.model.MessagePriority
import org.beacon.sdk.model.MessageType
import org.beacon.sdk.model.Peer
import org.beacon.sdk.model.Result

class MainViewModel : ViewModel() {

    private val TAG = "MainViewModel"

    // UI State
    private val _currentScreen = MutableStateFlow<String>("home")
    val currentScreen = _currentScreen.asStateFlow()

    private val _sosActive = MutableStateFlow<Boolean>(false)
    val sosActive = _sosActive.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Int>(0)
    val connectedPeers = _connectedPeers.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int>(100)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _powerMode = MutableStateFlow<org.beacon.sdk.model.PowerMode>(org.beacon.sdk.model.PowerMode.NORMAL)
    val powerMode = _powerMode.asStateFlow()

    // SDK reference
    private var beaconApp: BeaconApplication? = null

    fun initialize(app: BeaconApplication) {
        beaconApp = app
        observeSdk()
    }

    private fun observeSdk() {
        val sdk = beaconApp?.beaconSdk
        sdk?.let {
            // Observe peers
            it.peers.observePeers()
                .onEach { peers ->
                    _connectedPeers.value = peers.count { it.isOnline }
                }
                .launchIn(viewModelScope)

            // Observe power mode
            it.power.observePowerMode()
                .onEach { mode ->
                    _powerMode.value = mode
                }
                .launchIn(viewModelScope)

            // Observe battery
            it.power.observeBatteryLevel()
                .onEach { level ->
                    _batteryLevel.value = level
                }
                .launchIn(viewModelScope)

            // Observe incoming messages
            it.messaging.observeIncomingMessages()
                .onEach { message ->
                    handleIncomingMessage(message)
                }
                .launchIn(viewModelScope)
        }
    }

    private fun handleIncomingMessage(message: Message) {
        Log.d("MainViewModel", "Received message: ${message.id} from ${message.senderId}")
        
        // Check for SOS
        if (message.payload.type == MessageType.SOS) {
            _sosActive.value = true
            // TODO: Show SOS notification
        }
    }

    fun onSosReceived(messageId: String) {
        Log.d("MainViewModel", "SOS received via intent: $messageId")
        _currentScreen.value = "messages"
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Actions
    fun sendSos(location: org.beacon.core.model.Location, customMessage: String?) {
        beaconApp?.beaconSdk?.messaging?.sendSos(
            location = org.beacon.sdk.model.Location(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy
            ),
            customMessage = customMessage
        ).onSuccess { messageId ->
            Log.d(TAG, "SOS sent: $messageId")
            _sosActive.value = true
        }.onFailure { error ->
            Log.e(TAG, "SOS failed: ${error.message}")
        }
    }

    fun sendMessage(recipientId: String, text: String) {
        beaconApp?.beaconSdk?.messaging?.sendMessage(
            recipientId = org.beacon.sdk.model.PeerId(recipientId),
            payload = MessagePayload(
                type = MessageType.TEXT,
                text = text
            ),
            priority = MessagePriority.NORMAL
        )
    }

    fun updatePowerMode(mode: org.beacon.sdk.model.PowerMode) {
        beaconApp?.beaconSdk?.power?.setPowerMode(mode)
    }

    fun getCurrentScreen(): String = _currentScreen.value

    fun onSosReceived(messageId: String) {
        // Handle SOS from notification click
        _currentScreen.value = "messages"
    }
}