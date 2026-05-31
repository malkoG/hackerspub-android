package pub.hackers.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pub.hackers.android.R
import pub.hackers.android.ui.theme.LocalAppColors
import java.util.Locale

@Composable
fun LanguageFilterRow(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val uiLocale = remember(configuration) {
        if (configuration.locales.size() > 0) {
            configuration.locales[0]
        } else {
            Locale.getDefault()
        }
    }
    val displayLanguages = remember(languages, selectedLanguage) {
        if (selectedLanguage != null && selectedLanguage !in languages) {
            listOf(selectedLanguage) + languages
        } else {
            languages
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        item(key = "all") {
            LanguageFilterChip(
                label = stringResource(R.string.all_languages),
                selected = selectedLanguage == null,
                onClick = { onLanguageSelected(null) },
            )
        }
        items(
            items = displayLanguages,
            key = { it },
        ) { language ->
            LanguageFilterChip(
                label = languageFilterLabel(language, uiLocale),
                selected = selectedLanguage == language,
                onClick = { onLanguageSelected(language) },
            )
        }
    }
}

@Composable
private fun LanguageFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.background,
            labelColor = colors.textBody,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.background,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.buttonOutline,
            selectedBorderColor = colors.accent,
        ),
    )
}

@Composable
private fun languageFilterLabel(language: String, uiLocale: Locale): String {
    return remember(language, uiLocale) {
        val locale = Locale.forLanguageTag(language)
        val nativeName = locale.getDisplayLanguage(locale).ifBlank { language }
        val uiName = locale.getDisplayLanguage(uiLocale).ifBlank { language }
        if (nativeName.equals(uiName, ignoreCase = true)) {
            nativeName
        } else {
            "$nativeName ($uiName)"
        }
    }
}
