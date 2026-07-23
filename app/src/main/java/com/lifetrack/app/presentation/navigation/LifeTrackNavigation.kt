package com.lifetrack.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifetrack.app.presentation.screen.CalendarScreen
import com.lifetrack.app.presentation.screen.AboutScreen
import com.lifetrack.app.presentation.screen.AccountScreen
import com.lifetrack.app.presentation.screen.MealCameraScreen
import com.lifetrack.app.presentation.screen.HabitsScreen
import com.lifetrack.app.presentation.screen.HomeScreen
import com.lifetrack.app.presentation.screen.SleepScreen
import com.lifetrack.app.presentation.screen.StatisticsScreen
import com.lifetrack.app.presentation.screen.WaterScreen
import com.lifetrack.app.presentation.viewmodel.AppViewModel

private data class NavigationItem(val route: String, val label: String, val icon: @Composable () -> Unit)

private val navigationItems = listOf(
    NavigationItem("home", "Inicio") { Icon(Icons.Default.Home, contentDescription = null) },
    NavigationItem("habits", "Habitos") { Icon(Icons.Default.CheckCircle, contentDescription = null) },
    NavigationItem("water", "Agua") { Icon(Icons.Default.WaterDrop, contentDescription = null) },
    NavigationItem("sleep", "Sueno") { Icon(Icons.Default.Bedtime, contentDescription = null) },
    NavigationItem("statistics", "Resumen") { Icon(Icons.Default.Assessment, contentDescription = null) },
)

@Composable
fun LifeTrackNavigation(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val route = backStackEntry?.destination?.route
    Scaffold(
        topBar = { LifeTrackTopBar(onAccount = { navController.navigate("account") }) },
        bottomBar = {
            if (route !in setOf("calendar", "about", "account", "meal")) {
                NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", modifier = Modifier) {
            composable("home") { HomeScreen(innerPadding, onNavigate = navController::navigate) }
            composable("habits") { HabitsScreen(innerPadding) }
            composable("water") { WaterScreen(innerPadding) }
            composable("sleep") { SleepScreen(innerPadding) }
            composable("statistics") {
                StatisticsScreen(
                    innerPadding,
                    appViewModel,
                    onOpenCalendar = { navController.navigate("calendar") },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable("calendar") { CalendarScreen(innerPadding, onBack = { navController.popBackStack() }) }
            composable("about") { AboutScreen(innerPadding, onBack = { navController.popBackStack() }) }
            composable("account") { AccountScreen(innerPadding, onBack = { navController.popBackStack() }) }
            composable("meal") { MealCameraScreen(innerPadding, onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun LifeTrackTopBar(onAccount: () -> Unit) {
    Surface(tonalElevation = 2.dp, color = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(com.lifetrack.app.R.drawable.lifetrack_mark),
                contentDescription = "LifeTrack",
                modifier = Modifier.size(38.dp),
            )
            Text("LifeTrack", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onAccount) { Text("Cuenta") }
        }
    }
}
