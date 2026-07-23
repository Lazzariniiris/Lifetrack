package com.lifetrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.navigation.LifeTrackNavigation
import com.lifetrack.app.presentation.theme.LifeTrackTheme
import com.lifetrack.app.presentation.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
            LifeTrackTheme(preferences.themeMode) {
                LifeTrackNavigation(appViewModel)
            }
        }
    }
}
