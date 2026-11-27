package com.example.sanskarstream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvColorScheme = darkColorScheme(
    primary = Saffron,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = PanelColor,
    onSurface = Color.White,
)

/** TV app hamesha dark theme me (website jaisa black background). */
@Composable
fun SanskarStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = Typography,
        content = content
    )
}
