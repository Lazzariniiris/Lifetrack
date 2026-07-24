package com.lifetrack.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.navigation.LifeTrackNavigation
import com.lifetrack.app.presentation.theme.LifeTrackTheme
import com.lifetrack.app.presentation.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var recoveryLink by mutableStateOf<String?>(null)
    private var openMeals by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        recoveryLink = intent?.data?.takeIf(::isRecoveryLink)?.toString()
        openMeals = intent?.getBooleanExtra("open_meals", false) == true
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
            LifeTrackTheme(preferences.themeMode) {
                LifeTrackNavigation(
                    appViewModel = appViewModel,
                    recoveryLink = recoveryLink,
                    onRecoveryLinkConsumed = {
                        recoveryLink = null
                        intent?.data = null
                    },
                    openMeals = openMeals,
                    onMealsOpened = {
                        openMeals = false
                        intent?.removeExtra("open_meals")
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recoveryLink = intent.data?.takeIf(::isRecoveryLink)?.toString()
        openMeals = intent.getBooleanExtra("open_meals", false)
    }

    private fun isRecoveryLink(uri: Uri): Boolean =
        uri.scheme == "lifetrack" && uri.host == "auth" && uri.path == "/recovery"
}
