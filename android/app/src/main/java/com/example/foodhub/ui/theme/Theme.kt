package com.example.foodhub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhub.data.model.DietaryRegime
import com.example.foodhub.viewmodel.PreferencesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LightColorScheme = lightColorScheme(
    primary = ArancioneVibrante,   // Colore Principale (bottoni)
    onPrimary = Color.White,       // Colore del testo sopra il colore principale
    secondary = NavyProfondo,      // Colore secondario
    background = GrigioChiarissimo,// Sfondo generale dell'app
    surface = Color.White,         // Sfondo per i campi di testo o le Card
    onBackground = NavyProfondo,   // Colore del testo sopra lo sfondo generale
    onSurface = NavyProfondo       // Colore del testo sopra le Card
)
private val DarkColorScheme = darkColorScheme(
    primary = ArancioneVibrante,
    onPrimary = Color.White,
    secondary = ArancioneTenue,
    background = SfondoNavyScuro,
    surface = NavyProfondo,
    onBackground = GrigioChiarissimo,
    onSurface = GrigioChiarissimo
)

@Composable
fun FoodHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
