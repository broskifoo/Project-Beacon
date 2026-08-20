package org.beacon.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.beacon.mobile.ui.MainActivity
import org.beacon.sdk.model.PowerMode
import org.beacon.sdk.model.Result

class MeshForegroundService : LifecycleService() {

    private val TAG = "MeshForegroundService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "beacon_mesh_channel"

    private var scope = CoroutineScope(Dispatchers.IO)
    private var wakeLockJob: Job? = null
    private var beaconJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Mesh service started")
        
        // Start mesh networking
        startMeshNetworking()

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or 
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or 
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Beacon Mesh Network",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background mesh networking service"
                setShowBadge(false)
                enableVibration(false)
                sound = null
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_beacon_notification)
            .setContentTitle("Beacon Mesh Active")
            .setContentText("Mesh networking running in background")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    private fun startMeshNetworking() {
        beaconJob = scope.launch {
            val app = applicationContext as BeaconApplication
            val sdk = app.beaconSdk

            // Initialize mesh components
            sdk.lifecycle.start()

            // Observe power mode changes
            sdk.power.observePowerMode()
                .onEach { mode ->
                    Log.d(TAG, "Power mode changed: $mode")
                    adjustMeshBehavior(mode)
                }
                .launchIn(scope)

            // Observe peer events
            sdk.peers.observePeerEvents()
                .onEach { event ->
                    Log.d(TAG, "Peer event: ${event.eventType} - ${event.peer.id}")
                    handlePeerEvent(event)
                }
                .launchIn(scope)

            // Observe incoming messages
            sdk.messaging.observeIncomingMessages()
                .onEach { message ->
                    Log.d(TAG, "Incoming message: ${message.id} from ${message.senderId}")
                    handleIncomingMessage(message)
                }
                .launchIn(scope)

            // Periodic status update
            while (true) {
                delay(30000) // 30 seconds
                updateNotification()
                performMaintenance()
            }
        }
    }

    private fun adjustMeshBehavior(mode: PowerMode) {
        // TODO: Adjust scan intervals, advertising intervals based on power mode
        when (mode) {
            PowerMode.NORMAL -> {
                // Full scanning
            }
            PowerMode.CONSERVATION -> {
                // Reduced scanning
            }
            PowerMode.SURVIVAL -> {
                // Minimal scanning
            }
            PowerMode.CRITICAL -> {
                // Only SOS/identity beacon
            }
        }
    }

    private fun handlePeerEvent(event: org.beacon.sdk.model.PeerDiscoveryEvent) {
        // Update notification with peer count
        updateNotification()
        
        // TODO: Trigger UI update if app is in foreground
    }

    private fun handleIncomingMessage(message: org.beacon.sdk.model.Message) {
        // Check if it's an SOS
        if (message.payload.type == org.beacon.sdk.model.MessageType.SOS) {
            showSosNotification(message)
        }

        // TODO: Update UI if in foreground
    }

    private fun showSosNotification(message: org.beacon.sdk.model.Message) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SOS_MESSAGE_ID", message.id.value)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sos_notification)
            .setContentTitle("🚨 EMERGENCY SOS Received")
            .setContentText("From ${message.senderId.value.substring(0, 8)} - ${message.payload.text ?: "No message"}")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun updateNotification() {
        val app = applicationContext as BeaconApplication
        val peerCount = app.beaconSdk.peers.observePeers().firstOrNull()?.size ?: 0
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_beacon_notification)
            .setContentTitle("Beacon Mesh Active")
            .setContentText("$peerCount peers connected")
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun performMaintenance() {
        // Clean up old messages, expired peers, etc.
        val app = applicationContext as BeaconApplication
        // TODO: Implement maintenance tasks
    }

    private fun acquireWakeLock() {
        wakeLockJob = scope.launch {
            val powerManager = getSystemService(PowerManager::class.java)
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Beacon::MeshWakeLock"
            )
            wakeLock.acquire()
            
            // Release on service stop
            while (true) {
                delay(60000)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLockJob?.cancel()
        beaconJob?.cancel()
        
        val app = applicationContext as BeaconApplication
        app.beaconSdk.lifecycle.stop()
        
        super.onDestroy()
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}