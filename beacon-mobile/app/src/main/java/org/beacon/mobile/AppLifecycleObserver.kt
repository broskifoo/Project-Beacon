package org.beacon.mobile

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val TAG = "AppLifecycleObserver"

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "App moved to foreground")
        // App is visible - can increase scan rates
        (owner as? BeaconApplication)?.beaconSdk?.power?.setPowerMode(
            (owner as BeaconApplication).getPowerMode()
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "App moved to background")
        // App is hidden - can reduce scan rates if in conservation mode
        val app = owner as BeaconApplication
        if (app.getPowerMode() != org.beacon.sdk.model.PowerMode.NORMAL) {
            // Keep current power mode, but mesh service continues
        }
    }
}