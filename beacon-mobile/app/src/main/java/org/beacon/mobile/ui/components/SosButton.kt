package org.beacon.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import org.beacon.mobile.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosButton(
    sosActive: Boolean,
    onClick: () -> Unit
) {
    if (sosActive) {
        // Show SOS active card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 80.dp), // Account for bottom nav
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Theme.colorScheme.errorContainer
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SOS ACTIVE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Theme.colorScheme.onErrorContainer)
                        Text("Emergency broadcast sent - awaiting acknowledgment", fontSize = 12.sp, color = Theme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    }
                    Button(onClick = { /* Cancel retry */ }) {
                        Text("Cancel Retry")
                    }
                }
            }
        }
    } else {
        // Floating SOS button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 80.dp), // Account for bottom nav
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Theme.colorScheme.error,
                    contentColor = Theme.colorScheme.onError
                ),
                enabled = true
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.MedicalServices,
                            contentDescription = "SOS",
                            modifier = Modifier.size(24.dp)
                        )
                        Text("SOS", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}