package org.beacon.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import org.beacon.mobile.ui.components.SosButton
import org.beacon.mobile.ui.theme.Theme
import org.beacon.mobile.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    viewModel: MainViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()

    Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Resources list placeholder
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(0) { }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Text("No resources reported", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text("Community resources will appear here", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }

            // FAB for reporting
            FloatingActionButton(
                modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd),
                onClick = { /* Show report dialog */ }
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Report Resource"
                )
            }

            SosButton(
                sosActive = sosActive,
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