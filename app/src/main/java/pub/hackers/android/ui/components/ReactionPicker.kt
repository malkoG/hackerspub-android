package pub.hackers.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

val SUPPORTED_REACTION_EMOJIS = listOf("❤️", "🎉", "😂", "😲", "🤔", "😢", "👀")

@Composable
fun ReactionPicker(
    reactionGroups: List<ReactionGroup>,
    isSubmitting: Boolean,
    onEmojiSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val groupsByEmoji = remember(reactionGroups) {
        reactionGroups
            .filter { it.emoji != null }
            .associateBy { it.emoji!! }
    }

    val selectedGroups = remember(reactionGroups) {
        reactionGroups
            .filter { it.viewerHasReacted }
            .sortedWith(compareBy { group ->
                group.emoji?.let {
                    SUPPORTED_REACTION_EMOJIS.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE
                }
                    ?: Int.MAX_VALUE
            })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reactions),
                style = LocalAppTypography.current.bodyLargeSemiBold,
                color = LocalAppColors.current.textPrimary
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(LocalAppColors.current.surface)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(80.dp)
        ) {
            items(SUPPORTED_REACTION_EMOJIS) { emoji ->
                val existingGroup = groupsByEmoji[emoji]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (existingGroup?.viewerHasReacted == true)
                                LocalAppColors.current.accent.copy(alpha = 0.15f)
                            else
                                LocalAppColors.current.surface
                        )
                        .clickable(enabled = !isSubmitting) { onEmojiSelect(emoji) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = (existingGroup?.count ?: 0).toString(),
                        style = LocalAppTypography.current.labelSmall,
                        color = LocalAppColors.current.textSecondary
                    )
                }
            }
        }

        if (selectedGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = LocalAppColors.current.divider)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedGroups.forEach { group ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(LocalAppColors.current.accent.copy(alpha = 0.15f))
                            .clickable(enabled = !isSubmitting && group.emoji != null) {
                                group.emoji?.let { onEmojiSelect(it) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (group.emoji != null) {
                            Text(text = group.emoji)
                        } else if (group.customEmoji != null) {
                            AsyncImage(
                                model = group.customEmoji.imageUrl,
                                contentDescription = group.customEmoji.name,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = group.count.toString(),
                            style = LocalAppTypography.current.labelSmall,
                            color = LocalAppColors.current.textSecondary
                        )
                    }
                }
            }
        }
    }
}
