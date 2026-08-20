package org.beacon.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomNavigation
import androidx.compose.material3.BottomNavigationItem
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
fun BottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    sosActive: Boolean,
    onSosClick: () -> Unit
) {
    val navItems = listOf(
        NavItem("home", "Home", androidx.compose.material.icons.Icons.Default.Home),
        NavItem("map", "Map", androidx.compose.material.icons.Icons.Default.Map),
        NavItem("messages", "Messages", androidx.compose.material.icons.Icons.Default.Message),
        NavItem("network", "Network", androidx.compose.material.icons.Icons.Default.NetworkCheck),
        NavItem("resources", "Resources", androidx.compose.material.icons.Icons.Default.LocalHospital),
        NavItem("alerts", "Alerts", androidx.compose.material.icons.Icons.Default.Warning),
        NavItem("settings", "Settings", androidx.compose.material.icons.Icons.Default.Settings),
    )

    Theme {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme.colorScheme.outlineVariant)
                )

                BottomNavigation(
                    backgroundColor = Theme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.route
                        BottomNavigationItem(
                            selected = selected,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                            label = { Text(item.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) },
                            selectedContentColor = Theme.colorScheme.primary,
                            unselectedContentColor = Theme.colorScheme.onSurfaceVariant,
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.res.Painter
)