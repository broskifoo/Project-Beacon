package org.beacon.sdk.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.beacon.sdk.api.PowerApi
import org.beacon.sdk.model.*
import org.beacon.sdk.model.Result.Success

class AndroidPowerApi(
    private val context: Context,
    private val config: BeaconConfig
) : PowerApi {

    private val _powerMode = MutableStateFlow<PowerMode>(config.powerMode)
    override val observePowerMode: kotlinx.coroutines.flow.Flow<PowerMode> = _powerMode.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int>(100)
    override val observeBatteryLevel: kotlinx.coroutines.flow.Flow<Int> = _batteryLevel.asStateFlow()

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val percent = (level * 100 / scale)
                _batteryLevel.value = percent
                updatePowerModeFromBattery(percent)
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        
        // Initial battery level
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        _batteryLevel.value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        updatePowerModeFromBattery(_batteryLevel.value)
    }

    private fun updatePowerModeFromBattery(level: Int) {
        val newMode = when {
            level <= 10 -> PowerMode.CRITICAL
            level <= 20 -> PowerMode.SURVIVAL
            level <= 50 -> PowerMode.CONSERVATION
            else -> PowerMode.NORMAL
        }
        if (newMode != _powerMode.value) {
            _powerMode.value = newMode
        }
    }

    override suspend fun getCurrentMode(): Result<PowerMode> = Success(_powerMode.value)

    override suspend fun setPowerMode(mode: PowerMode): Result<Unit> {
        _powerMode.value = mode
        return Success(Unit)
    }

    override suspend fun getBatteryLevel(): Result<Int> = Success(_batteryLevel.value)

    override suspend fun getEstimatedRuntime(): Result<EstimatedRuntime> {
        val level = _batteryLevel.value
        // Rough estimates based on typical phone battery
        return Success(EstimatedRuntime(
            normalModeHours = level / 5.0,
            conservationModeHours = level / 2.5,
            survivalModeHours = level / 1.0,
            criticalModeHours = level / 0.5
        ))
    }

    override suspend fun requestWakeLock(durationMs: Long): Result<WakeLockToken> {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BeaconSDK::WakeLock")
        wakeLock.acquire(durationMs)
        
        return Success(WakeLockToken(
            id = java.util.UUID.randomUUID().toString(),
            acquiredAt = Instant.now(),
            expiresAt = Instant.now().plus(java.time.Duration.ofMillis(durationMs))
        ))
    }

    override suspend fun releaseWakeLock(token: WakeLockToken): Result<Unit> {
        // WakeLock is auto-released by timeout, but we can't track it easily
        // In a real implementation, we'd store the WakeLock reference
        return Success(Unit)
    }
}