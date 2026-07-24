package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lifetrack.app.BuildConfig
import com.lifetrack.app.R

@Composable
fun AboutScreen(contentPadding: PaddingValues) {
    ScreenColumn(contentPadding) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(painterResource(R.drawable.lifetrack_mark), contentDescription = null, modifier = Modifier.size(112.dp))
            Image(
                painter = painterResource(R.drawable.lifetrack_wordmark),
                contentDescription = null,
                modifier = Modifier.width(220.dp).height(56.dp),
                contentScale = ContentScale.Fit,
            )
            Text("Pequeñas acciones, grandes cambios.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("LifeTrack", style = MaterialTheme.typography.titleLarge)
                Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text("Seguimiento personal de hábitos, hidratación, sueño y alimentación.", style = MaterialTheme.typography.bodyLarge)
                Text("LifeTrack no reemplaza el consejo de profesionales de la salud.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
