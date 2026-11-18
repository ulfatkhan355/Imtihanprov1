package com.imtihanpro.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.imtihanpro.app.ui.theme.LocalImtihanColors

enum class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Home),
    SUBJECTS("subjects", "Subjects", Icons.Outlined.AutoStories),
    PRACTICE("practice", "Practice", Icons.Outlined.Quiz),
    PAPERS("papers", "Papers", Icons.Outlined.Description),
    PROFILE("profile", "Profile", Icons.Outlined.Person)
}

@Composable
fun ImtihanBottomNavBar(
    currentRoute: String?,
    onSelectTab: (BottomNavTab) -> Unit
) {
    val colors = LocalImtihanColors.current
    Column {
        HorizontalDivider(thickness = 1.dp, color = colors.cardBorder.copy(alpha = 0.45f))
        NavigationBar(
            containerColor = colors.canvasDeep.copy(alpha = 0.96f),
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp
        ) {
            BottomNavTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(imageVector = tab.icon, contentDescription = tab.label)
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.cyanPulse,
                        selectedTextColor = colors.cyanPulse,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary,
                        indicatorColor = colors.cardElevated
                    )
                )
            }
        }
    }
}
