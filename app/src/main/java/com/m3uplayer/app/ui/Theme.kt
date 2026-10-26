package com.m3uplayer.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.m3uplayer.app.data.Tab

val Purple = Color(0xFF6B52A3)
val PurpleLight = Color(0xFF9B83E0)
val CardBg = Color(0xB32E2540)
val Accent = Color(0xFFFFD83A)
val TextDim = Color(0xFFB8B0C8)

private val scheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = PurpleLight,
    secondaryContainer = Color(0xFF4B3A7A),
    onSecondaryContainer = Color.White,
    background = Color(0xFF0D0B2B),
    onBackground = Color.White,
    surface = Color(0xFF2A2040),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A2F52),
    onSurfaceVariant = TextDim,
    error = Color(0xFFFF6B6B),
)

val AppBackground = Brush.linearGradient(
    listOf(Color(0xFF0B0A26), Color(0xFF1E0E26), Color(0xFF3B1620), Color(0xFF3A2A1C))
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

fun Tab.label(): String = when (this) {
    Tab.HOME -> "الرئيسية"
    Tab.LIVE -> "البث الحي"
    Tab.MOVIES -> "الأفلام"
    Tab.SERIES -> "المسلسلات"
    Tab.FAVORITES -> "المفضلة"
    Tab.SETTINGS -> "الإعدادات"
}

fun Tab.icon(): ImageVector = when (this) {
    Tab.HOME -> Icons.Default.Home
    Tab.LIVE -> Icons.Default.LiveTv
    Tab.MOVIES -> Icons.Default.Movie
    Tab.SERIES -> Icons.Default.VideoLibrary
    Tab.FAVORITES -> Icons.Default.Favorite
    Tab.SETTINGS -> Icons.Default.Settings
}
