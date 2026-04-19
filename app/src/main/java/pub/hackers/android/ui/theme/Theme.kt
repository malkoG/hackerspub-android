package pub.hackers.android.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private fun lightColorSchemeFrom(colors: AppColorScheme) = lightColorScheme(
    primary = colors.accent,
    onPrimary = colors.background,
    primaryContainer = colors.surface,
    onPrimaryContainer = colors.textPrimary,
    secondary = colors.surface,
    onSecondary = colors.textPrimary,
    secondaryContainer = colors.surface,
    onSecondaryContainer = colors.textSecondary,
    tertiary = colors.textSecondary,
    onTertiary = colors.background,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.background,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surface,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.divider,
    outlineVariant = colors.divider,
    error = colors.reaction,
    onError = colors.background,
)

private fun darkColorSchemeFrom(colors: AppColorScheme) = darkColorScheme(
    primary = colors.accent,
    onPrimary = colors.background,
    primaryContainer = colors.surface,
    onPrimaryContainer = colors.textPrimary,
    secondary = colors.surface,
    onSecondary = colors.textPrimary,
    secondaryContainer = colors.surface,
    onSecondaryContainer = colors.textSecondary,
    tertiary = colors.textSecondary,
    onTertiary = colors.background,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.background,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surface,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.divider,
    outlineVariant = colors.divider,
    error = colors.reaction,
    onError = colors.background,
)

// Map a Material You ColorScheme onto AppColorScheme so that custom widgets
// (which read LocalAppColors instead of MaterialTheme.colorScheme) also pick
// up the user's wallpaper-derived palette.
//
// Use surface tonal layers (surface / surfaceContainerHigh) instead of the
// near-neutral background/surfaceVariant — Material You intentionally keeps
// background/surfaceVariant close to white/black, so mapping to those makes
// wallpaper changes barely visible. Tonal containers carry the wallpaper hue.
//
// Semantic accents (reaction/share/hashtag/composeAccent) stay branded so
// destructive and link affordances don't blend into the dynamic primary.
private fun dynamicAppColors(scheme: ColorScheme, dark: Boolean) = AppColorScheme(
    background = scheme.surface,
    surface = scheme.surfaceContainerHigh,
    textPrimary = scheme.onSurface,
    textBody = scheme.onSurface,
    textSecondary = scheme.onSurfaceVariant,
    accent = scheme.primary,
    accentMuted = scheme.secondaryContainer,
    // Brand convention: divider == surface so separators are nearly invisible
    // on cards and only show as subtle strips against the (lighter) background.
    // Material You's outlineVariant is intentionally a visible line color, which
    // is too strong for this layout.
    divider = scheme.surfaceContainerHigh,
    buttonOutline = scheme.outline,
    // Keep the main compose CTA obviously dynamic; leaving it branded red makes
    // Material You feel inactive because the FAB is one of the most prominent
    // colored elements in the app.
    composeAccent = scheme.primaryContainer,
    composeOnAccent = scheme.onPrimaryContainer,
    reaction = if (dark) DarkAppColors.reaction else LightAppColors.reaction,
    share = if (dark) DarkAppColors.share else LightAppColors.share,
    hashtag = if (dark) DarkAppColors.hashtag else LightAppColors.hashtag,
)

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("NewApi")
private fun dynamicColorSchemeFor(context: Context, darkTheme: Boolean): ColorScheme =
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

@Composable
fun HackersPubTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val dynamicAvailable = themeMode == ThemeMode.DYNAMIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicAvailable -> dynamicColorSchemeFor(context, darkTheme)
        darkTheme -> darkColorSchemeFrom(DarkAppColors)
        else -> lightColorSchemeFrom(LightAppColors)
    }
    val appColors = when {
        dynamicAvailable -> dynamicAppColors(colorScheme, darkTheme)
        darkTheme -> DarkAppColors
        else -> LightAppColors
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides AppTypographyDefaults,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
