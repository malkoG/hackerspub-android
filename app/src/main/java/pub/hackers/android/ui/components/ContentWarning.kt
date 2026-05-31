package pub.hackers.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

fun Post.contentWarningText(): String? {
    val trimmedSummary = summary?.trim().orEmpty()
    return when {
        typename != "Article" && trimmedSummary.isNotEmpty() -> trimmedSummary
        sensitive -> null
        else -> null
    }
}

fun Post.hasContentWarning(): Boolean = contentWarningText() != null || sensitive

@Composable
fun ContentWarningNotice(
    warningText: String?,
    revealed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = colors.textSecondary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.content_warning),
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = warningText ?: stringResource(R.string.sensitive_content),
                    style = typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
            TextButton(onClick = onToggle) {
                Text(
                    text = if (revealed) {
                        stringResource(R.string.hide)
                    } else {
                        stringResource(R.string.show)
                    }
                )
            }
        }
    }
}
