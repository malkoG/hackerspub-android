package pub.hackers.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun ArticleCard(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayPost = post.sharedPost ?: post
    val isRepost = post.sharedPost != null
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (isRepost) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        RichDisplayName(
                            name = post.actor.name?.let { "$it ${stringResource(R.string.share)}d" },
                            fallback = "${post.actor.handle} ${stringResource(R.string.share)}d",
                            style = typography.caption,
                            color = colors.textSecondary,
                            emojiHeight = 14.dp
                        )
                    }
                }

                // Author row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = displayPost.actor.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(AppShapes.avatarTimeline)
                            .clip(CircleShape)
                            .clickable { onProfileClick(displayPost.actor.handle) },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RichDisplayName(
                                name = displayPost.actor.name,
                                fallback = displayPost.actor.handle,
                                style = typography.bodyLargeSemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { onProfileClick(displayPost.actor.handle) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatRelativeTime(displayPost.published),
                                style = typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }

                        Text(
                            text = displayPost.actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Article title
                displayPost.name?.let { title ->
                    Text(
                        text = title,
                        style = typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Summary or excerpt
                val summaryText = displayPost.summary ?: displayPost.excerpt
                if (summaryText.isNotBlank()) {
                    Text(
                        text = summaryText,
                        style = typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // "Read full article" footer
            HorizontalDivider(color = colors.divider)
            Text(
                text = stringResource(R.string.read_full_article),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
