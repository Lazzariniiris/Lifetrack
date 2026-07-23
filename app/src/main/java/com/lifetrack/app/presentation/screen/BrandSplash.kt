package com.lifetrack.app.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lifetrack.app.R
import kotlinx.coroutines.delay

@Composable
fun BrandSplash(onFinished: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (revealed) 1f else 0f, tween(420), label = "logo_alpha")
    val scale by animateFloatAsState(if (revealed) 1f else 0.92f, tween(520), label = "logo_scale")
    LaunchedEffect(Unit) {
        revealed = true
        delay(1_000)
        onFinished()
    }
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color(0xFF090E1E),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().alpha(alpha).scale(scale),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.lifetrack_mark),
                contentDescription = "Logo de LifeTrack",
                modifier = Modifier.size(184.dp),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(R.drawable.lifetrack_wordmark),
                contentDescription = "LifeTrack",
                modifier = Modifier.width(208.dp).height(54.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
