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
import androidx.compose.material3.Button
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
import com.lifetrack.app.R

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    ScreenColumn(contentPadding) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(painterResource(R.drawable.lifetrack_mark), contentDescription = "Logo de LifeTrack", modifier = Modifier.size(156.dp))
            Image(
                painter = painterResource(R.drawable.lifetrack_wordmark),
                contentDescription = "LifeTrack",
                modifier = Modifier.width(220.dp).height(56.dp),
                contentScale = ContentScale.Fit,
            )
            Text("Pequenas acciones, grandes cambios.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("LifeTrack", style = MaterialTheme.typography.titleLarge)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text("Seguimiento personal local para habitos, hidratacion y sueno.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver al resumen") }
    }
}
