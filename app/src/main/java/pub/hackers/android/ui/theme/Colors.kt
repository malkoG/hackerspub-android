package pub.hackers.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentMuted: Color,
    val divider: Color,
    val buttonOutline: Color,
    val composeAccent: Color,
    val composeOnAccent: Color,
    val reaction: Color,
    val bookmark: Color,
    val share: Color,
    val hashtag: Color,
)

val LightAppColors = AppColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F7),
    textPrimary = Color(0xFF1C1C1E),
    textBody = Color(0xFF3A3A3C),
    textSecondary = Color(0xFFAEAEB2),
    accent = Color(0xFF44403C),
    accentMuted = Color(0xFF78716C),
    divider = Color(0xFFF2F2F7),
    buttonOutline = Color(0xFFD6D3D1),
    composeAccent = Color(0xFFEF4444),
    composeOnAccent = Color(0xFFFFFFFF),
    reaction = Color(0xFFE8453C),
    bookmark = Color(0xFFF59E0B),
    share = Color(0xFF34D399),
    hashtag = Color(0xFF0891B2),
)

val DarkAppColors = AppColorScheme(
    background = Color(0xFF171717),
    surface = Color(0xFF262626),
    textPrimary = Color(0xFFFAFAFA),
    textBody = Color(0xFFD4D4D4),
    textSecondary = Color(0xFF737373),
    accent = Color(0xFFD6D3D1),
    accentMuted = Color(0xFFA8A29E),
    divider = Color(0xFF262626),
    buttonOutline = Color(0xFF525252),
    composeAccent = Color(0xFFF87171),
    composeOnAccent = Color(0xFFFFFFFF),
    reaction = Color(0xFFE8453C),
    bookmark = Color(0xFFFBBF24),
    share = Color(0xFF34D399),
    hashtag = Color(0xFF22D3EE),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
