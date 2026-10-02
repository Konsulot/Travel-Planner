package com.travelplanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Палитра «морская волна + закат»: бирюзовый основной цвет и тёплый коралловый акцент.
private val Teal40 = Color(0xFF006A6A)
private val Teal80 = Color(0xFF4CDADA)
private val Teal90 = Color(0xFF6FF7F6)
private val Teal10 = Color(0xFF002020)
private val Coral40 = Color(0xFF9C4235)
private val Coral80 = Color(0xFFFFB4A8)
private val Coral90 = Color(0xFFFFDAD4)
private val Coral10 = Color(0xFF410000)
private val Sand40 = Color(0xFF6B5D2F)
private val Sand80 = Color(0xFFD8C58D)
private val Sand90 = Color(0xFFF5E1A7)

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = Coral90,
    onSecondaryContainer = Coral10,
    tertiary = Sand40,
    tertiaryContainer = Sand90,
    background = Color(0xFFF4FBFA),
    surface = Color(0xFFF4FBFA),
    surfaceVariant = Color(0xFFDAE5E4),
    surfaceContainerLow = Color(0xFFEEF5F4),
    surfaceContainer = Color(0xFFE8EFEE),
    surfaceContainerHigh = Color(0xFFE3E9E9),
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Teal90,
    secondary = Coral80,
    onSecondary = Color(0xFF5F150D),
    secondaryContainer = Color(0xFF7E2B20),
    onSecondaryContainer = Coral90,
    tertiary = Sand80,
    tertiaryContainer = Color(0xFF524619),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerHigh = Color(0xFF252B2B),
)

/** Цвета для статусов бюджета — не зависят от динамической палитры, чтобы смысл не терялся. */
object StatusColors {
    val ok = Color(0xFF2E7D32)
    val warning = Color(0xFFEF6C00)
    val danger = Color(0xFFC62828)
}

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TravelPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Динамические цвета (Material You) выключены по умолчанию, чтобы сохранить фирменную палитру.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
