@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package pub.hackers.android.ui.components

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Duration
import java.time.Instant
import java.util.Locale

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

    if (displayPost.typename == "Article") {
        ArticleCard(
            post = post,
            onClick = onClick,
            onProfileClick = onProfileClick,
            modifier = modifier
        )
    } else {
        NoteCard(
            post = post,
            onClick = onClick,
            onProfileClick = onProfileClick,
            onReplyClick = onReplyClick,
            onShareClick = onShareClick,
            onQuoteClick = onQuoteClick,
            onReactionClick = onReactionClick,
            onExternalShareClick = onExternalShareClick,
            onQuotedPostClick = onQuotedPostClick,
            contentMaxLength = contentMaxLength,
            modifier = modifier
        )
    }
}

@Composable
private fun NoteCard(
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
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val translationFailedText = stringResource(R.string.translation_failed)
    val scope = rememberCoroutineScope()

    var translatedContent by remember(displayPost.id) { mutableStateOf<String?>(null) }
    var translationError by remember(displayPost.id) { mutableStateOf<String?>(null) }
    var isTranslating by remember(displayPost.id) { mutableStateOf(false) }
    var showTranslated by remember(displayPost.id) { mutableStateOf(false) }

    // Step 1: Replace Card with plain Column
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Step 7: Repost indicator
        if (isRepost) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 54.dp, bottom = 8.dp)
            ) {
                AsyncImage(
                    model = post.actor.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(AppShapes.avatarRepost)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
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

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Step 2: Avatar 42dp
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
                // Author row — name left, visibility icon + timestamp right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
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
                            text = displayPost.actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = when (displayPost.visibility) {
                            PostVisibility.PUBLIC -> Icons.Filled.Public
                            PostVisibility.UNLISTED -> Icons.Outlined.Lock
                            PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                            PostVisibility.DIRECT -> Icons.Outlined.Lock
                            else -> Icons.Filled.Public
                        },
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatRelativeTime(displayPost.published),
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
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

                // Step 3: Body text color
                val truncatedContent = if (contentMaxLength > 0 && displayPost.content.length > contentMaxLength) {
                    displayPost.content.take(contentMaxLength)
                } else {
                    displayPost.content
                }
                val isTruncated = contentMaxLength > 0 && displayPost.content.length > contentMaxLength

                if (showTranslated && translatedContent != null) {
                    Text(
                        text = translatedContent!!,
                        style = typography.bodyLarge,
                        color = colors.textBody,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    HtmlContent(
                        html = truncatedContent,
                        maxLines = if (contentMaxLength > 0) Int.MAX_VALUE else 10,
                        modifier = Modifier.fillMaxWidth(),
                        onMentionClick = onProfileClick,
                        onTextClick = onClick
                    )
                }

                if (isTranslating) {
                    Text(
                        text = stringResource(R.string.translating),
                        style = typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (translationError != null) {
                    Text(
                        text = translationError!!,
                        style = typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isTruncated || (contentMaxLength == 0 && displayPost.content.length > 500)) {
                    Text(
                        text = stringResource(R.string.read_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Step 6: Media grid
                if (displayPost.media.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaGrid(media = displayPost.media)
                }

                // Quoted post
                if (displayPost.quotedPost != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuotedPostPreview(
                        post = displayPost.quotedPost!!,
                        onClick = { onQuotedPostClick?.invoke(displayPost.quotedPost!!.id) },
                        onProfileClick = onProfileClick
                    )
                }

                // Reaction groups
                if (displayPost.reactionGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayPost.reactionGroups.forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(AppShapes.reactionPillRadius),
                                color = if (group.viewerHasReacted)
                                    colors.accent.copy(alpha = 0.15f)
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
                                            colors.accent
                                        else
                                            colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step 4: Engagement bar
                EngagementBar(
                    post = displayPost,
                    onReplyClick = onReplyClick,
                    onShareClick = onShareClick,
                    onQuoteClick = onQuoteClick,
                    onReactionClick = onReactionClick,
                    onExternalShareClick = onExternalShareClick,
                    onTranslateClick = {
                        if (isTranslating) return@EngagementBar

                        if (showTranslated) {
                            showTranslated = false
                            return@EngagementBar
                        }

                        translatedContent?.let {
                            showTranslated = true
                            return@EngagementBar
                        }

                        val targetLanguageTag = androidx.core.os.ConfigurationCompat
                            .getLocales(context.resources.configuration)
                            .get(0)?.language ?: Locale.getDefault().language
                        scope.launch {
                            isTranslating = true
                            translationError = null
                            try {
                                val translated = translateHtmlContent(
                                    html = displayPost.content,
                                    targetLanguageTag = targetLanguageTag
                                )
                                translatedContent = translated
                                showTranslated = true
                            } catch (_: Exception) {
                                translationError = translationFailedText
                            } finally {
                                isTranslating = false
                            }
                        }
                    },
                    isTranslating = isTranslating,
                    isTranslated = showTranslated
                )
            }
        }
    }
}

// Step 4: Restyled EngagementBar
@Composable
private fun EngagementBar(
    post: Post,
    onReplyClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?,
    onQuoteClick: (() -> Unit)? = null,
    onReactionClick: (() -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null,
    onTranslateClick: (() -> Unit)? = null,
    isTranslating: Boolean = false,
    isTranslated: Boolean = false
) {
    val colors = LocalAppColors.current
    val isReplied = post.engagementStats.replies > 0 && post.replyTarget != null
    val isShared = post.viewerHasShared
    val isReacted = post.reactionGroups.any { it.viewerHasReacted }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Reply
        EngagementButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = post.engagementStats.replies,
            contentDescription = stringResource(R.string.replies),
            onClick = onReplyClick,
            activeColor = colors.accent,
            isActive = isReplied
        )

        // Share/Repost
        EngagementButton(
            icon = if (isShared) Icons.Filled.Repeat else Icons.Outlined.Repeat,
            count = post.engagementStats.shares,
            contentDescription = stringResource(R.string.shares),
            onClick = onShareClick,
            activeColor = colors.share,
            isActive = isShared
        )

        // Heart/React
        EngagementButton(
            icon = if (isReacted) Icons.Filled.Favorite else Icons.Outlined.Favorite,
            count = post.engagementStats.reactions,
            contentDescription = stringResource(R.string.reactions),
            onClick = onReactionClick,
            activeColor = colors.reaction,
            isActive = isReacted
        )

        // Quote
        EngagementButton(
            icon = Icons.Outlined.FormatQuote,
            count = post.engagementStats.quotes,
            contentDescription = stringResource(R.string.quotes),
            onClick = onQuoteClick,
            activeColor = colors.accent,
            isActive = false
        )

        // Translate
        EngagementButton(
            icon = Icons.Outlined.Translate,
            count = 0,
            contentDescription = if (isTranslated) {
                stringResource(R.string.show_original)
            } else {
                stringResource(R.string.translate)
            },
            onClick = if (isTranslating) null else onTranslateClick,
            activeColor = colors.accent,
            isActive = isTranslated
        )

        // External share — always textSecondary
        if (onExternalShareClick != null) {
            IconButton(
                onClick = onExternalShareClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
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
    isActive: Boolean = false,
    activeColor: Color = LocalAppColors.current.accent
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val tint = if (isActive) activeColor else colors.textSecondary

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
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        if (count > 0) {
            Text(
                text = formatCount(count),
                style = typography.labelMedium,
                color = tint
            )
        }
    }
}

// Step 5: Restyled QuotedPostPreview
@Composable
fun QuotedPostPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colors.divider,
                shape = RoundedCornerShape(AppShapes.quotedPostRadius)
            )
            .clip(RoundedCornerShape(AppShapes.quotedPostRadius))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(AppShapes.avatarQuoted)
                    .clip(CircleShape)
                    .clickable { onProfileClick(post.actor.handle) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            RichDisplayName(
                name = post.actor.name,
                fallback = post.actor.handle,
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onProfileClick(post.actor.handle) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = post.actor.handle,
                style = typography.labelMedium,
                color = colors.textSecondary,
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
                    .clip(RoundedCornerShape(AppShapes.mediaRadius)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Step 6: MediaGrid with 8dp radius
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
                    .clip(RoundedCornerShape(AppShapes.mediaRadius))
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
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
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
                                .clip(RoundedCornerShape(AppShapes.mediaRadius))
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
                                    .clip(RoundedCornerShape(AppShapes.mediaRadius))
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

internal fun formatRelativeTime(instant: Instant): String {
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

private suspend fun translateHtmlContent(
    html: String,
    targetLanguageTag: String
): String = withContext(Dispatchers.IO) {
    val plainText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()

    if (plainText.isBlank()) {
        return@withContext ""
    }

    val languageIdentifier = LanguageIdentification.getClient()
    val detectedLanguageTag = languageIdentifier.identifyLanguage(plainText).await()
    val sourceLanguage = TranslateLanguage.fromLanguageTag(detectedLanguageTag)
    val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageTag)

    if (sourceLanguage == null || targetLanguage == null || sourceLanguage == targetLanguage) {
        languageIdentifier.close()
        return@withContext plainText
    }

    val translatorOptions = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()

    val translator = Translation.getClient(translatorOptions)

    try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        translator.translate(plainText).await()
    } finally {
        translator.close()
        languageIdentifier.close()
    }
}
