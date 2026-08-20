package org.beacon.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.beacon.mobile.ui.navigation.BeaconNavHost
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme {
                BeaconNavHost(viewModel)
            }
        }

        // Handle SOS intent from notification
        handleSosIntent()
    }

    private fun handleSosIntent() {
        intent?.getStringExtra("SOS_MESSAGE_ID")?.let { messageId ->
            // Navigate to SOS detail or messages
            viewModel.onSosReceived(messageId)
            intent.removeExtra("SOS_MESSAGE_ID")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSosIntent()
    }
}