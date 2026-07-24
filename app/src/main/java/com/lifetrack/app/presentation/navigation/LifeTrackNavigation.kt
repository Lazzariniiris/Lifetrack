package com.lifetrack.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifetrack.app.presentation.screen.AboutScreen
import com.lifetrack.app.presentation.screen.CalendarScreen
import com.lifetrack.app.presentation.screen.HabitsScreen
import com.lifetrack.app.presentation.screen.HomeScreen
import com.lifetrack.app.presentation.screen.MealCameraScreen
import com.lifetrack.app.presentation.screen.MealsScreen
import com.lifetrack.app.presentation.screen.ProfileScreen
import com.lifetrack.app.presentation.screen.SettingsScreen
import com.lifetrack.app.presentation.screen.SleepScreen
import com.lifetrack.app.presentation.screen.StatisticsScreen
import com.lifetrack.app.presentation.screen.WaterScreen
import com.lifetrack.app.presentation.viewmodel.AppViewModel

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val mainDestinations = listOf(
    Destination("home", "Inicio", Icons.Rounded.Home),
    Destination("habits", "Hábitos", Icons.Rounded.CheckCircle),
    Destination("statistics", "Estadísticas", Icons.Rounded.BarChart),
    Destination("profile", "Perfil", Icons.Rounded.Person),
)

private val routeTitles = mapOf(
    "habits" to "Hábitos",
    "statistics" to "Estadísticas",
    "profile" to "Perfil",
    "water" to "Agua",
    "sleep" to "Sueño",
    "meals" to "Comidas",
    "meal-camera" to "Cámara",
    "calendar" to "Calendario",
    "settings" to "Configuración",
    "about" to "Acerca de",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeTrackNavigation(
    appViewModel: AppViewModel,
    recoveryLink: String? = null,
    onRecoveryLinkConsumed: () -> Unit = {},
    openMeals: Boolean = false,
    onMealsOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val isMainRoute = mainDestinations.any { it.route == route }
    var showRegisterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(recoveryLink) {
        if (recoveryLink != null) navController.navigate("profile") { launchSingleTop = true }
    }
    LaunchedEffect(openMeals) {
        if (openMeals) {
            navController.navigate("meals") { launchSingleTop = true }
            onMealsOpened()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isMainRoute && route != "meal-camera") {
                CenterAlignedTopAppBar(
                    title = { Text(routeTitles[route].orEmpty()) },
                    navigationIcon = {
                        if (!isMainRoute) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = isMainRoute, enter = fadeIn(), exit = fadeOut()) {
                FloatingActionButton(onClick = { showRegisterSheet = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Registrar")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isMainRoute, enter = fadeIn(), exit = fadeOut()) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    mainDestinations.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 12 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 12 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
        ) {
            composable("home") { HomeScreen(innerPadding, navController::navigate) }
            composable("habits") { HabitsScreen(innerPadding) }
            composable("statistics") {
                StatisticsScreen(innerPadding, onOpenCalendar = { navController.navigate("calendar") })
            }
            composable("profile") {
                ProfileScreen(
                    innerPadding,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenAbout = { navController.navigate("about") },
                    recoveryLink = recoveryLink,
                    onRecoveryLinkConsumed = onRecoveryLinkConsumed,
                )
            }
            composable("water") { WaterScreen(innerPadding) }
            composable("sleep") { SleepScreen(innerPadding) }
            composable("meals") { MealsScreen(innerPadding, onOpenCamera = { navController.navigate("meal-camera") }) }
            composable("meal-camera") { MealCameraScreen(innerPadding, onBack = { navController.popBackStack() }) }
            composable("calendar") { CalendarScreen(innerPadding) }
            composable("settings") { SettingsScreen(innerPadding, appViewModel) }
            composable("about") { AboutScreen(innerPadding) }
        }
    }

    if (showRegisterSheet) {
        RegisterSheet(
            onDismiss = { showRegisterSheet = false },
            onNavigate = {
                showRegisterSheet = false
                navController.navigate(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterSheet(onDismiss: () -> Unit, onNavigate: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Registrar",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RegisterAction("Agua", Icons.Rounded.WaterDrop) { onNavigate("water") }
                RegisterAction("Sueño", Icons.Rounded.Bedtime) { onNavigate("sleep") }
                RegisterAction("Comida", Icons.Rounded.Restaurant) { onNavigate("meals") }
                RegisterAction("Cámara", Icons.Rounded.CameraAlt) { onNavigate("meal-camera") }
            }
        }
    }
}

@Composable
private fun RowScope.RegisterAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
