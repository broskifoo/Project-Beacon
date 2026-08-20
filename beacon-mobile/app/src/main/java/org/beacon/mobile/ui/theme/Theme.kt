package org.beacon.mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fonts.Font
import androidx.compose.ui.graphics.fonts.FontFamily
import androidx.compose.ui.graphics.fonts.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamilyResolver
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006D6D),
    primaryContainer = Color(0xFFA7F0F0),
    secondary = Color(0xFF526D52),
    secondaryContainer = Color(0xFFD5E5D5),
    tertiary = Color(0xFF6D5252),
    tertiaryContainer = Color(0xFFF5DDDD),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    background = Color(0xFFFAFAFA),
    error = Color(0xFFBA1A1A),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF002121),
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFF102810),
    onTertiary = Color.White,
    onTertiaryContainer = Color(0xFF2D1515),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF404848),
    onBackground = Color(0xFF1A1A1A),
    onError = Color.White,
    onErrorContainer = Color.White,
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFFC0C8C8),
    scrim = Color.Black,
    shadow = Color.Black,
    inverseSurface = Color(0xFF2D3333),
    inverseOnSurface = Color(0xFFEFF2F2),
    inversePrimary = Color(0xFF6FE0E0),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B5B5),
    primaryContainer = Color(0xFF004D4D),
    secondary = Color(0xFFB5D5B5),
    secondaryContainer = Color(0xFF384D38),
    tertiary = Color(0xFFE5B5B5),
    tertiaryContainer = Color(0xFF523838),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    error = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF002121),
    onPrimaryContainer = Color(0xFFA7F0F0),
    onSecondary = Color(0xFF102810),
    onSecondaryContainer = Color(0xFFD5E5D5),
    onTertiary = Color(0xFF2D1515),
    onTertiaryContainer = Color(0xFFF5DDDD),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFBFC8C8),
    onBackground = Color(0xFFE0E0E0),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF404848),
    scrim = Color.Black,
    shadow = Color.Black,
    inverseSurface = Color(0xFFEFF2F2),
    inverseOnSurface = Color(0xFF2D3333),
    inversePrimary = Color(0xFF006D6D),
)

@Composable
fun Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val context = LocalContext.current
    val fontFamily = FontFamilyResolver(context).fontFamily

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            fontFamily = fontFamily,
            displayLarge = androidx.compose.ui.text.TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal),
            displayMedium = androidx.compose.ui.text.TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal),
            headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
            labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
        ),
        content = content
    )
}