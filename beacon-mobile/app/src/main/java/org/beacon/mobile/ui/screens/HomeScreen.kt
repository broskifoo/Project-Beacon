package org.beacon.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import org.beacon.mobile.ui.components.BottomNavBar
import org.beacon.mobile.ui.components.SosButton
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val powerMode by viewModel.powerMode.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()

    Theme {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$batteryLevel%", fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(powerMode.name, fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // App title
                    Text("BEACON", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)

                    // Peer count
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$connectedPeers", fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("peers", fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Main action buttons
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        title = "Map",
                        subtitle = "View nearby peers & resources",
                        icon = androidx.compose.material.icons.Icons.Default.Map,
                        onClick = { onNavigate("map") }
                    )
                    NavCard(
                        title = "Messages",
                        subtitle = "Send & receive messages",
                        icon = androidx.compose.material.icons.Icons.Default.Message,
                        onClick = { onNavigate("messages") }
                    )
                    NavCard(
                        title = "Network",
                        subtitle = "Mesh topology & peers",
                        icon = androidx.compose.material.icons.Icons.Default.NetworkCheck,
                        onClick = { onNavigate("network") }
                    )
                    NavCard(
                        title = "Resources",
                        subtitle = "Water, medical, shelter locations",
                        icon = androidx.compose.material.icons.Icons.Default.LocalHospital,
                        onClick = { onNavigate("resources") }
                    )
                    NavCard(
                        title = "Alerts",
                        subtitle = "Emergency broadcasts",
                        icon = androidx.compose.material.icons.Icons.Default.Warning,
                        onClick = { onNavigate("alerts") }
                    )
                }

                // SOS Button
                if (sosActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("SOS ACTIVE", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Emergency broadcast sent - awaiting acknowledgment", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                }
                                Button(onClick = { /* cancel SOS */ }) {
                                    Text("Cancel Retry")
                                }
                            }
                        }
                    }
                }
            }

            // Bottom navigation
            BottomNavBar(
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                sosActive = sosActive,
                onSosClick = { /* handled by SOS button */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.res.Painter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp).padding(end = 16.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Icon(androidx.compose.material.icons.Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(24.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}