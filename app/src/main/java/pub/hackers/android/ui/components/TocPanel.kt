package pub.hackers.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pub.hackers.android.R
import pub.hackers.android.domain.model.TocItem
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun TocPanel(
    items: List<TocItem>,
    onAnchorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    var expanded by remember { mutableStateOf(false) }
    val baseLevel = remember(items) { items.minOf { it.level } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colors.divider,
                shape = RoundedCornerShape(8.dp),
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.table_of_contents),
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items.forEach { item ->
                    TocEntry(item = item, baseLevel = baseLevel, onClick = onAnchorClick)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun TocEntry(
    item: TocItem,
    baseLevel: Int,
    onClick: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val indent = ((item.level - baseLevel).coerceAtLeast(0) * 12).dp

    Text(
        text = item.title,
        style = typography.bodyMedium.copy(
            color = colors.accent,
            fontWeight = if (item.level <= baseLevel) FontWeight.SemiBold else FontWeight.Normal,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.id) }
            .padding(start = indent, top = 6.dp, bottom = 6.dp),
    )

    item.children.forEach { child ->
        TocEntry(item = child, baseLevel = baseLevel, onClick = onClick)
    }
}
