package org.beacon.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.beacon.mobile.ui.components.BottomNavBar
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    viewModel: MainViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Theme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SOS Confirmation Dialog
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        "SEND EMERGENCY SOS?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.TextAlign.Center
                    )
                    
                    Text(
                        "This will broadcast your location to all nearby Beacon nodes and rescue teams.\n\nIncludes: GPS location, battery level, your ID",
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.TextAlign.Center,
                        maxLines = 4
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo("home") },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = { 
                                // TODO: Get actual location and send SOS
                                viewModel.sendSos(
                                    org.beacon.core.model.Location(latitude = 0.0, longitude = 0.0),
                                    null
                                )
                                viewModel.navigateTo("messages")
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("SEND SOS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            BottomNavBar(
                currentScreen = currentScreen,
                onNavigate = viewModel::navigateTo,
                sosActive = false,
                onSosClick = { }
            )
        }
    }
}