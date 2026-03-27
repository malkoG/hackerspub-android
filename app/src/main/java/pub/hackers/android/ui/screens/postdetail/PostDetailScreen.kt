package pub.hackers.android.ui.screens.postdetail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.MediaGrid
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.QuotedPostPreview
import pub.hackers.android.ui.components.ReactionPicker
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import android.text.Html
import androidx.compose.material.icons.outlined.Translate
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit,
    isLoggedIn: Boolean = true,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val confirmBeforeDelete by viewModel.preferencesManager.confirmBeforeDelete.collectAsState(initial = true)
    val confirmBeforeShare by viewModel.preferencesManager.confirmBeforeShare.collectAsState(initial = false)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showShareConfirmation by remember { mutableStateOf(false) }

    // Navigate back after successful deletion
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    // Reaction picker bottom sheet
    if (uiState.showReactionPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleReactionPicker() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReactionPicker(
                reactionGroups = uiState.reactionGroups,
                isSubmitting = uiState.isReacting,
                onEmojiSelect = { viewModel.toggleReaction(it) },
                onClose = { viewModel.toggleReactionPicker() }
            )
        }
    }

    // Shares bottom sheet
    if (uiState.showSharesSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSharesSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SharesSheet(
                actors = uiState.shareActors,
                isLoading = uiState.isLoadingShares,
                onProfileClick = { handle ->
                    viewModel.dismissSharesSheet()
                    onProfileClick(handle)
                },
                onClose = { viewModel.dismissSharesSheet() }
            )
        }
    }

    // Quotes bottom sheet
    if (uiState.showQuotesSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissQuotesSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            QuotesSheet(
                posts = uiState.quotePosts,
                isLoading = uiState.isLoadingQuotes,
                onPostClick = { id ->
                    viewModel.dismissQuotesSheet()
                    onPostClick(id)
                },
                onClose = { viewModel.dismissQuotesSheet() }
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_post_confirm_title)) },
            text = { Text(stringResource(R.string.delete_post_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deletePost()
                    }
                ) {
                    Text(
                        stringResource(R.string.delete_post),
                        color = colors.reaction
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showShareConfirmation) {
        val hasShared = uiState.post?.viewerHasShared == true
        AlertDialog(
            onDismissRequest = { showShareConfirmation = false },
            title = {
                Text(stringResource(if (hasShared) R.string.unshare_confirm_title else R.string.share_confirm_title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareConfirmation = false
                        if (hasShared) viewModel.unsharePost() else viewModel.sharePost()
                    }
                ) {
                    Text(stringResource(if (hasShared) R.string.unshare_confirm_action else R.string.share_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.deleteError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteError() },
            title = { Text(stringResource(R.string.action_error)) },
            text = { Text(uiState.deleteError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeleteError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = if (uiState.post?.typename == "Article") stringResource(R.string.article) else "Post",
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.accent
                        )
                    }
                },
                trailingContent = if (uiState.canDelete) {
                    {
                        PostDetailActionMenu(
                            isDeleting = uiState.isDeleting,
                            onDelete = {
                                if (confirmBeforeDelete) {
                                    showDeleteConfirmation = true
                                } else {
                                    viewModel.deletePost()
                                }
                            }
                        )
                    }
                } else null
            )
        },
        floatingActionButton = {
            if (uiState.post != null && isLoggedIn) {
                FloatingActionButton(
                    onClick = { onReplyClick(postId) },
                    containerColor = colors.composeAccent,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.reply)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    FullScreenLoading()
                }
                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error ?: stringResource(R.string.error_generic),
                        onRetry = { viewModel.loadPost(postId) }
                    )
                }
                uiState.post != null -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        PostDetailContent(
                            post = uiState.post!!,
                            reactionGroups = uiState.reactionGroups,
                            replies = uiState.replies,
                            hasMoreReplies = uiState.hasMoreReplies,
                            isLoadingMoreReplies = uiState.isLoadingMoreReplies,
                            onLoadMoreReplies = { viewModel.loadMoreReplies() },
                            onProfileClick = onProfileClick,
                            onPostClick = onPostClick,
                            onReplyClick = { onReplyClick(postId) },
                            onShareClick = {
                                if (confirmBeforeShare) {
                                    showShareConfirmation = true
                                } else {
                                    if (uiState.post!!.viewerHasShared) {
                                        viewModel.unsharePost()
                                    } else {
                                        viewModel.sharePost()
                                    }
                                }
                            },
                            onReactionClick = { emoji -> viewModel.toggleReaction(emoji) },
                            onReactionPickerClick = { viewModel.toggleReactionPicker() },
                            onQuoteClick = { onQuoteClick(postId) },
                            onSharesClick = { viewModel.showSharesSheet() },
                            onQuotesClick = { viewModel.showQuotesSheet() },
                            onExternalShareClick = {
                                val shareUrl = uiState.post?.url
                                    ?: uiState.post?.iri
                                if (shareUrl != null) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailActionMenu(
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More actions"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete_post),
                        color = colors.reaction
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = colors.reaction
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
                enabled = !isDeleting
            )
        }
    }
}

@Composable
private fun PostDetailContent(
    post: Post,
    reactionGroups: List<ReactionGroup>,
    replies: List<Post>,
    hasMoreReplies: Boolean = false,
    isLoadingMoreReplies: Boolean = false,
    onLoadMoreReplies: () -> Unit = {},
    onProfileClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onShareClick: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onReactionPickerClick: () -> Unit,
    onQuoteClick: () -> Unit,
    onSharesClick: () -> Unit,
    onQuotesClick: () -> Unit,
    onExternalShareClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val translationFailedText = stringResource(R.string.translation_failed)

    var translatedContent by remember(post.id) { mutableStateOf<String?>(null) }
    var translationError by remember(post.id) { mutableStateOf<String?>(null) }
    var isTranslating by remember(post.id) { mutableStateOf(false) }
    var showTranslated by remember(post.id) { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())

    LazyColumn {
        item {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Reply target preview
                if (post.replyTarget != null) {
                    ReplyTargetPreview(
                        post = post.replyTarget!!,
                        onClick = { onPostClick(post.replyTarget!!.id) },
                        onProfileClick = onProfileClick
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.replying_to)} @${post.replyTarget!!.actor.handle}",
                            style = typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(AppShapes.avatarTimeline)
                            .clip(CircleShape)
                            .clickable { onProfileClick(post.actor.handle) },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.clickable { onProfileClick(post.actor.handle) }
                    ) {
                        Text(
                            text = post.actor.name ?: post.actor.handle,
                            style = typography.bodyLargeSemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "@${post.actor.handle}",
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isArticle = post.typename == "Article"

                post.name?.let { title ->
                    Text(
                        text = title,
                        style = if (isArticle) typography.titleLarge else typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(if (isArticle) 12.dp else 8.dp))
                    if (isArticle) {
                        HorizontalDivider(color = colors.divider)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (showTranslated && translatedContent != null) {
                    Text(
                        text = translatedContent!!,
                        style = typography.bodyLarge,
                        color = colors.textBody,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    HtmlContent(
                        html = post.content,
                        modifier = Modifier.fillMaxWidth(),
                        onMentionClick = onProfileClick
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

                if (post.media.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MediaGrid(media = post.media)
                }

                if (post.quotedPost != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuotedPostPreview(
                        post = post.quotedPost!!,
                        onClick = { onPostClick(post.quotedPost!!.id) },
                        onProfileClick = onProfileClick
                    )
                }

                if (post.typename == "Article" && post.url != null) {
                    val uriHandler = LocalUriHandler.current
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { uriHandler.openUri(post.url) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.read_on_web))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormatter.format(post.published),
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )

                    if (post.visibility != pub.hackers.android.domain.model.PostVisibility.PUBLIC) {
                        Text(
                            text = "\u00B7",
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Icon(
                            imageVector = when (post.visibility) {
                                pub.hackers.android.domain.model.PostVisibility.UNLISTED -> Icons.Outlined.Lock
                                pub.hackers.android.domain.model.PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                                pub.hackers.android.domain.model.PostVisibility.DIRECT -> Icons.Outlined.Lock
                                else -> Icons.Filled.Public
                            },
                            contentDescription = when (post.visibility) {
                                pub.hackers.android.domain.model.PostVisibility.UNLISTED -> "Unlisted"
                                pub.hackers.android.domain.model.PostVisibility.FOLLOWERS -> "Followers only"
                                pub.hackers.android.domain.model.PostVisibility.DIRECT -> "Direct"
                                else -> "Public"
                            },
                            modifier = Modifier.size(14.dp),
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${post.engagementStats.replies} ${stringResource(R.string.replies)}",
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${post.engagementStats.shares} ${stringResource(R.string.shares)}",
                        style = typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onSharesClick() }
                    )
                    Text(
                        text = "${post.engagementStats.reactions} ${stringResource(R.string.reactions)}",
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${post.engagementStats.quotes} ${stringResource(R.string.quotes)}",
                        style = typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onQuotesClick() }
                    )
                }

                if (reactionGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        reactionGroups.forEach { group ->
                            Card(
                                onClick = {
                                    group.emoji?.let { onReactionClick(it) }
                                },
                                shape = RoundedCornerShape(AppShapes.reactionPillRadius),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (group.viewerHasReacted)
                                        colors.accent.copy(alpha = 0.2f)
                                    else
                                        colors.surface
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (group.emoji != null) {
                                        Text(text = group.emoji)
                                    } else if (group.customEmoji != null) {
                                        AsyncImage(
                                            model = group.customEmoji.imageUrl,
                                            contentDescription = group.customEmoji.name,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = group.count.toString(),
                                        style = typography.labelMedium,
                                        color = if (group.viewerHasReacted)
                                            colors.accent
                                        else
                                            colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = colors.divider)

                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    IconButton(onClick = onReplyClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = stringResource(R.string.reply),
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.share),
                            tint = if (post.viewerHasShared)
                                colors.share
                            else
                                colors.textSecondary
                        )
                    }
                    IconButton(onClick = onReactionPickerClick) {
                        Icon(
                            imageVector = Icons.Outlined.AddReaction,
                            contentDescription = stringResource(R.string.reactions),
                            tint = if (reactionGroups.any { it.viewerHasReacted })
                                colors.accent
                            else
                                colors.textSecondary
                        )
                    }
                    IconButton(onClick = onQuoteClick) {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = stringResource(R.string.quotes),
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isTranslating) return@IconButton
                            if (showTranslated) {
                                showTranslated = false
                                return@IconButton
                            }
                            translatedContent?.let {
                                showTranslated = true
                                return@IconButton
                            }
                            val targetLanguageTag = androidx.core.os.ConfigurationCompat
                                .getLocales(context.resources.configuration)
                                .get(0)?.language ?: Locale.getDefault().language
                            scope.launch {
                                isTranslating = true
                                translationError = null
                                try {
                                    val translated = translateDetailContent(
                                        html = post.content,
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
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = if (showTranslated)
                                stringResource(R.string.show_original)
                            else
                                stringResource(R.string.translate),
                            tint = if (showTranslated) colors.accent else colors.textSecondary
                        )
                    }
                    IconButton(onClick = onExternalShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = colors.divider)
            }
        }

        if (replies.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.replies),
                    style = typography.bodyLargeSemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            items(
                items = replies,
                key = { it.id }
            ) { reply ->
                PostCard(
                    post = reply,
                    onClick = { onPostClick(reply.id) },
                    onProfileClick = onProfileClick,
                    onQuotedPostClick = onPostClick
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
            }

            if (isLoadingMoreReplies) {
                item {
                    LoadingItem()
                }
            } else if (hasMoreReplies) {
                item {
                    TextButton(
                        onClick = onLoadMoreReplies,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(stringResource(R.string.load_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun SharesSheet(
    actors: List<pub.hackers.android.domain.model.Actor>,
    isLoading: Boolean,
    onProfileClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.shares),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (actors.isEmpty()) {
            Text(
                text = "No shares yet",
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            actors.forEach { actor ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick(actor.handle) }
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = actor.name ?: actor.handle,
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "@${actor.handle}",
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotesSheet(
    posts: List<Post>,
    isLoading: Boolean,
    onPostClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.quotes),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Text(
                text = "No quotes yet",
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            posts.forEach { post ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPostClick(post.id) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = post.actor.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(AppShapes.avatarQuoted)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = post.actor.name ?: post.actor.handle,
                                style = typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "@${post.actor.handle}",
                                style = typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HtmlContent(
                        html = post.content,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(
                    color = colors.divider,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ReplyTargetPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(post.actor.handle) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = post.actor.name ?: post.actor.handle,
                    style = typography.bodyLargeSemiBold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "@${post.actor.handle}",
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HtmlContent(
            html = post.content,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            onMentionClick = onProfileClick
        )

        if (post.media.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MediaGrid(media = post.media)
        }
    }
}

private suspend fun translateDetailContent(
    html: String,
    targetLanguageTag: String
): String = withContext(Dispatchers.IO) {
    val plainText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    if (plainText.isBlank()) return@withContext ""

    val languageIdentifier = LanguageIdentification.getClient()
    val detectedTag = languageIdentifier.identifyLanguage(plainText).await()
    val sourceLanguage = TranslateLanguage.fromLanguageTag(detectedTag)
    val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageTag)

    if (sourceLanguage == null || targetLanguage == null || sourceLanguage == targetLanguage) {
        languageIdentifier.close()
        return@withContext plainText
    }

    val options = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()
    val translator = Translation.getClient(options)
    try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        translator.translate(plainText).await()
    } finally {
        translator.close()
        languageIdentifier.close()
    }
}
