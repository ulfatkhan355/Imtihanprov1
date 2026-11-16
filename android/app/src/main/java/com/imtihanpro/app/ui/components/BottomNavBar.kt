package com.imtihanpro.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
    NavigationBar(
        containerColor = colors.canvasDeep,
        contentColor = colors.textPrimary
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
                    Text(text = tab.label, maxLines = 1)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentBlue,
                    selectedTextColor = colors.accentBlue,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.cardElevated
                )
            )
        }
    }
}
