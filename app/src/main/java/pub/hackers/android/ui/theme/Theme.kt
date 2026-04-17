package pub.hackers.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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

@Composable
fun HackersPubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) darkColorSchemeFrom(appColors) else lightColorSchemeFrom(appColors)

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
