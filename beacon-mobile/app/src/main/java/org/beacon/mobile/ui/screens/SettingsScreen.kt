package org.beacon.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.beacon.mobile.ui.components.BottomNavBar
import org.beacon.mobile.ui.components.SosButton
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()
    val powerMode by viewModel.powerMode.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    var enableBle by remember { mutableStateOf(true) }
    var enableWifi by remember { mutableStateOf(true) }
    var enableLora by remember { mutableStateOf(false) }
    var encryptStorage by remember { mutableStateOf(true) }
    var autoRotateKeys by remember { mutableStateOf(true) }
    var notifySos by remember { mutableStateOf(true) }
    var notifyHigh by remember { mutableStateOf(true) }
    var notifyNormal by remember { mutableStateOf(false) }

    Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Network section
                SettingsSection(title = "Network", icon = androidx.compose.material.icons.Icons.Default.SettingsInputComponent) {
                    SettingsRow(
                        title = "Bluetooth Low Energy",
                        subtitle = "Primary discovery & messaging",
                        trailing = { Switch(checked = enableBle, onCheckedChange = { enableBle = it }) }
                    )
                    SettingsRow(
                        title = "Wi-Fi Direct",
                        subtitle = "High-bandwidth transfers",
                        trailing = { Switch(checked = enableWifi, onCheckedChange = { enableWifi = it }) }
                    )
                    SettingsRow(
                        title = "LoRa (External Radio)",
                        subtitle = "Long-range communication",
                        trailing = { Switch(checked = enableLora, onCheckedChange = { enableLora = it }) }
                    )
                }

                // Power section
                SettingsSection(title = "Power Management", icon = androidx.compose.material.icons.Icons.Default.BatteryStd) {
                    SettingsRow(
                        title = "Power Mode",
                        subtitle = "Current: ${powerMode.name}",
                        trailing = { 
                            Text(powerMode.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        }
                    )
                    SettingsRow(
                        title = "Battery Level",
                        subtitle = "$batteryLevel%",
                        trailing = {
                            Text("$batteryLevel%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    )
                    SettingsRow(
                        title = "Encrypt Storage",
                        subtitle = "AES-256 encryption for all local data",
                        trailing = { Switch(checked = encryptStorage, onCheckedChange = { encryptStorage = it }) }
                    )
                }

                // Security section
                SettingsSection(title = "Security", icon = androidx.compose.material.icons.Icons.Default.Security) {
                    SettingsRow(
                        title = "Auto-rotate Identity Keys",
                        subtitle = "Generate new keys every 90 days",
                        trailing = { Switch(checked = autoRotateKeys, onCheckedChange = { autoRotateKeys = it }) }
                    )
                    SettingsRow(
                        title = "View Identity Fingerprint",
                        subtitle = "Verify with contacts out-of-band",
                        trailing = {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    )
                    SettingsRow(
                        title = "Trusted Peers",
                        subtitle = "Manage peer trust relationships",
                        trailing = {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                // Notifications section
                SettingsSection(title = "Notifications", icon = androidx.compose.material.icons.Icons.Default.NotificationsActive) {
                    SettingsRow(
                        title = "SOS Alerts",
                        subtitle = "Always notify for emergencies",
                        trailing = { Switch(checked = notifySos, onCheckedChange = { notifySos = it }) }
                    )
                    SettingsRow(
                        title = "High Priority Messages",
                        subtitle = "Urgent messages",
                        trailing = { Switch(checked = notifyHigh, onCheckedChange = { notifyHigh = it }) }
                    )
                    SettingsRow(
                        title = "Normal Priority Messages",
                        subtitle = "Regular messages",
                        trailing = { Switch(checked = notifyNormal, onCheckedChange = { notifyNormal = it }) }
                    )
                }

                // About section
                SettingsSection(title = "About", icon = androidx.compose.material.icons.Icons.Default.Info) {
                    SettingsRow(
                        title = "Version",
                        subtitle = "0.1.0-alpha",
                        trailing = { Text("0.1.0-alpha", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    SettingsRow(
                        title = "Build Date",
                        subtitle = "2026-08-20",
                        trailing = { Text("2026-08-20", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    SettingsRow(
                        title = "License",
                        subtitle = "MIT License",
                        trailing = { Icon(androidx.compose.material.icons.Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    SettingsRow(
                        title = "Report Issue",
                        subtitle = "GitHub Issues",
                        trailing = { Icon(androidx.compose.material.icons.Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }

            SosButton(
                sosActive = false,
                onClick = { }
            )

            BottomNavBar(
                currentScreen = currentScreen,
                onNavigate = viewModel::navigateTo,
                sosActive = sosActive,
                onSosClick = { }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.res.Painter,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp).padding(end = 12.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
            )

            content()
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}