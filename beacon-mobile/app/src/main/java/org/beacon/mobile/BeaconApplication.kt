package org.beacon.mobile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import io.reactivex.rxjava3.core.Completable
import org.beacon.sdk.BeaconSdk
import org.beacon.sdk.BeaconSdkFactory
import org.beacon.sdk.model.BeaconConfig
import org.beacon.sdk.model.PowerMode

class BeaconApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "BeaconApplication"
        const val PREF_DEVICE_NAME = "device_name"
        const val PREF_POWER_MODE = "power_mode"
        const val PREF_ENABLE_BLE = "enable_ble"
        const val PREF_ENABLE_WIFI = "enable_wifi"
        const val PREF_ENABLE_LORA = "enable_lora"
        const val PREF_AUTO_SOS_RETRY = "auto_sos_retry"
    }

    private lateinit var beaconSdk: BeaconSdk
    private lateinit var dataStore: androidx.datastore.preferences.rxjava3.RxDataStore<Preferences>

    override fun onCreate() {
        super.onCreate()

        // Initialize DataStore
        dataStore = RxPreferenceDataStoreBuilder(this, "beacon_prefs").build()

        // Initialize WorkManager with custom configuration
        WorkManager.initialize(this, this)

        // Initialize Beacon SDK
        initializeBeaconSdk()

        // Observe app lifecycle for background/foreground transitions
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    private fun initializeBeaconSdk() {
        val config = BeaconConfig(
            deviceName = getDeviceName(),
            enableBle = getEnableBle(),
            enableWifiDirect = getEnableWifi(),
            enableLora = getEnableLora(),
            powerMode = getPowerMode()
        )

        BeaconSdkFactory.create(this, config)
            .onSuccess { sdk ->
                beaconSdk = sdk
                sdk.initialize()
                    .onSuccess {
                        Log.i(TAG, "Beacon SDK initialized successfully")
                        // Start mesh service
                        startMeshService()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Beacon SDK initialization failed: ${error.message}")
                    }
            }
            .onFailure { error ->
                Log.e(TAG, "Beacon SDK creation failed: ${error.message}")
            }
    }

    private fun startMeshService() {
        val intent = android.content.Intent(this, MeshForegroundService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
    }

    // Preferences helpers
    private val deviceNameKey = preferencesKey<String>(PREF_DEVICE_NAME)
    private val powerModeKey = preferencesKey<String>(PREF_POWER_MODE)
    private val enableBleKey = preferencesKey<Boolean>(PREF_ENABLE_BLE)
    private val enableWifiKey = preferencesKey<Boolean>(PREF_ENABLE_WIFI)
    private val enableLoraKey = preferencesKey<Boolean>(PREF_ENABLE_LORA)

    fun getDeviceName(): String = dataStore.data.firstOrNull()?.getString(deviceNameKey) ?: "Beacon-${android.os.Build.MODEL}"
    fun getPowerMode(): PowerMode = PowerMode.valueOf(dataStore.data.firstOrNull()?.getString(powerModeKey) ?: "NORMAL")
    fun getEnableBle(): Boolean = dataStore.data.firstOrNull()?.getBoolean(enableBleKey) ?: true
    fun getEnableWifi(): Boolean = dataStore.data.firstOrNull()?.getBoolean(enableWifiKey) ?: true
    fun getEnableLora(): Boolean = dataStore.data.firstOrNull()?.getBoolean(enableLoraKey) ?: false

    suspend fun setDeviceName(name: String) {
        dataStore.updateData { it.putString(deviceNameKey, name) }.subscribe()
    }

    suspend fun setPowerMode(mode: PowerMode) {
        dataStore.updateData { it.putString(powerModeKey, mode.name) }.subscribe()
        beaconSdk.power.setPowerMode(mode)
    }

    suspend fun setEnableBle(enabled: Boolean) {
        dataStore.updateData { it.putBoolean(enableBleKey, enabled) }.subscribe()
        // TODO: Restart transport
    }

    suspend fun setEnableWifi(enabled: Boolean) {
        dataStore.updateData { it.putBoolean(enableWifiKey, enabled) }.subscribe()
        // TODO: Restart transport
    }

    suspend fun setEnableLora(enabled: Boolean) {
        dataStore.updateData { it.putBoolean(enableLoraKey, enabled) }.subscribe()
        // TODO: Restart transport
    }

    fun getBeaconSdk(): BeaconSdk = beaconSdk
}