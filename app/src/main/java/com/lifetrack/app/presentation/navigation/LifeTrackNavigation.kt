package com.lifetrack.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifetrack.app.presentation.screen.CalendarScreen
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
        bottomBar = {
            if (route != "calendar") {
                NavigationBar {
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
                StatisticsScreen(innerPadding, appViewModel, onOpenCalendar = { navController.navigate("calendar") })
            }
            composable("calendar") { CalendarScreen(innerPadding, onBack = { navController.popBackStack() }) }
        }
    }
}
