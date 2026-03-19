@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package pub.hackers.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import java.time.Duration
import java.time.Instant

@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onQuoteClick: (() -> Unit)? = null,
    onReactionClick: (() -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null,
    onQuotedPostClick: ((String) -> Unit)? = null,
    contentMaxLength: Int = 0,
    modifier: Modifier = Modifier
) {
    val displayPost = post.sharedPost ?: post
    val isRepost = post.sharedPost != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.actor.name ?: post.actor.handle} ${stringResource(R.string.share)}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = displayPost.actor.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(displayPost.actor.handle) },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayPost.actor.name ?: displayPost.actor.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { onProfileClick(displayPost.actor.handle) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatRelativeTime(displayPost.published),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "@${displayPost.actor.handle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayPost.typename == "Article") {
                Text(
                    text = "Article",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            displayPost.name?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            val truncatedContent = if (contentMaxLength > 0 && displayPost.content.length > contentMaxLength) {
                displayPost.content.take(contentMaxLength)
            } else {
                displayPost.content
            }
            val isTruncated = contentMaxLength > 0 && displayPost.content.length > contentMaxLength

            HtmlContent(
                html = truncatedContent,
                maxLines = if (contentMaxLength > 0) Int.MAX_VALUE else 10,
                modifier = Modifier.fillMaxWidth(),
                onMentionClick = onProfileClick
            )

            if (isTruncated || (contentMaxLength == 0 && displayPost.content.length > 500)) {
                Text(
                    text = stringResource(R.string.read_more),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (displayPost.media.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MediaGrid(media = displayPost.media)
            }

            if (displayPost.quotedPost != null) {
                Spacer(modifier = Modifier.height(8.dp))
                QuotedPostPreview(
                    post = displayPost.quotedPost!!,
                    onClick = { onQuotedPostClick?.invoke(displayPost.quotedPost!!.id) },
                    onProfileClick = onProfileClick
                )
            }

            if (displayPost.reactionGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayPost.reactionGroups.forEach { group ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (group.viewerHasReacted)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (group.emoji != null) {
                                    Text(
                                        text = group.emoji,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else if (group.customEmoji != null) {
                                    AsyncImage(
                                        model = group.customEmoji.imageUrl,
                                        contentDescription = group.customEmoji.name,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = group.count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (group.viewerHasReacted)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EngagementBar(
                post = displayPost,
                onReplyClick = onReplyClick,
                onShareClick = onShareClick,
                onQuoteClick = onQuoteClick,
                onReactionClick = onReactionClick,
                onExternalShareClick = onExternalShareClick
            )
        }
    }
}

@Composable
private fun EngagementBar(
    post: Post,
    onReplyClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?,
    onQuoteClick: (() -> Unit)? = null,
    onReactionClick: (() -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EngagementButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = post.engagementStats.replies,
            contentDescription = stringResource(R.string.replies),
            onClick = onReplyClick
        )

        EngagementButton(
            icon = if (post.viewerHasShared) Icons.Filled.Repeat else Icons.Outlined.Repeat,
            count = post.engagementStats.shares,
            contentDescription = stringResource(R.string.shares),
            onClick = onShareClick,
            isActive = post.viewerHasShared
        )

        EngagementButton(
            icon = if (post.reactionGroups.any { it.viewerHasReacted }) Icons.Filled.Favorite else Icons.Outlined.Favorite,
            count = post.engagementStats.reactions,
            contentDescription = stringResource(R.string.reactions),
            onClick = onReactionClick,
            isActive = post.reactionGroups.any { it.viewerHasReacted }
        )

        EngagementButton(
            icon = Icons.Outlined.FormatQuote,
            count = post.engagementStats.quotes,
            contentDescription = stringResource(R.string.quotes),
            onClick = onQuoteClick
        )

        if (onExternalShareClick != null) {
            IconButton(
                onClick = onExternalShareClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    isActive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onClick?.invoke() },
            enabled = onClick != null
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        if (count > 0) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuotedPostPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.actor.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(post.actor.handle) },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = post.actor.name ?: post.actor.handle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onProfileClick(post.actor.handle) }
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "@${post.actor.handle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            post.name?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            HtmlContent(
                html = post.content,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                onMentionClick = onProfileClick
            )

            if (post.media.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = post.media.first().url,
                    contentDescription = post.media.first().alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun MediaGrid(media: List<pub.hackers.android.domain.model.Media>) {
    when (media.size) {
        1 -> {
            MediaImage(
                url = media[0].url,
                alt = media[0].alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                media.forEach { item ->
                    MediaImage(
                        url = item.url,
                        alt = item.alt,
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    media.take(2).forEach { item ->
                        MediaImage(
                            url = item.url,
                            alt = item.alt,
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                if (media.size > 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        media.drop(2).take(2).forEach { item ->
                            MediaImage(
                                url = item.url,
                                alt = item.alt,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaImage(
    url: String,
    alt: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        AsyncImage(
            model = url,
            contentDescription = alt,
            modifier = modifier.combinedClickable(
                onClick = {
                    // Open image in browser/viewer
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                onLongClick = { showMenu = true }
            ),
            contentScale = ContentScale.Crop
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                onClick = {
                    showMenu = false
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, url)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.open_in_browser)) },
                onClick = {
                    showMenu = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }
    }
}

private fun formatRelativeTime(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
        duration.toHours() < 24 -> "${duration.toHours()}h"
        duration.toDays() < 7 -> "${duration.toDays()}d"
        duration.toDays() < 30 -> "${duration.toDays() / 7}w"
        duration.toDays() < 365 -> "${duration.toDays() / 30}mo"
        else -> "${duration.toDays() / 365}y"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1_000_000.0)
    }
}
