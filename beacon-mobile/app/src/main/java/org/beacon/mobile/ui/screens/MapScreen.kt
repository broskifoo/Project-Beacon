package org.beacon.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.beacon.mobile.ui.components.BottomNavBar
import org.beacon.mobile.ui.components.SosButton
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()

    Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Map placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text("Offline Map View", fontSize = 20.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("MapLibre integration pending", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            // SOS Button overlay
            SosButton(
                sosActive = sosActive,
                onClick = { /* Handle SOS */ }
            )

            BottomNavBar(
                currentScreen = currentScreen,
                onNavigate = viewModel::navigateTo,
                sosActive = sosActive,
                onSosClick = { /* Handle SOS */ }
            )
        }
    }
}