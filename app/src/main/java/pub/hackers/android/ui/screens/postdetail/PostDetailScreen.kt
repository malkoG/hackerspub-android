package pub.hackers.android.ui.screens.postdetail

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pub.hackers.android.ui.components.CompactTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.MediaGrid
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.QuotedPostPreview
import pub.hackers.android.ui.components.ReactionPicker
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                        color = MaterialTheme.colorScheme.error
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
            CompactTopBar(
                title = "Post",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.canDelete) {
                        PostDetailActionMenu(
                            isDeleting = uiState.isDeleting,
                            onDelete = { showDeleteConfirmation = true }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.post != null && isLoggedIn) {
                FloatingActionButton(
                    onClick = { onReplyClick(postId) }
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
                                showShareConfirmation = true
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
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.replying_to)} @${post.replyTarget!!.actor.handle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@${post.actor.handle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                post.name?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HtmlContent(
                    html = post.content,
                    modifier = Modifier.fillMaxWidth(),
                    onMentionClick = onProfileClick
                )

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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormatter.format(post.published),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (post.visibility != pub.hackers.android.domain.model.PostVisibility.PUBLIC) {
                        Text(
                            text = "\u00B7",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${post.engagementStats.shares} ${stringResource(R.string.shares)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onSharesClick() }
                    )
                    Text(
                        text = "${post.engagementStats.reactions} ${stringResource(R.string.reactions)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${post.engagementStats.quotes} ${stringResource(R.string.quotes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
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
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (group.viewerHasReacted)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (group.viewerHasReacted)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    IconButton(onClick = onReplyClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = stringResource(R.string.reply),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.share),
                            tint = if (post.viewerHasShared)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onReactionPickerClick) {
                        Icon(
                            imageVector = Icons.Outlined.AddReaction,
                            contentDescription = stringResource(R.string.reactions),
                            tint = if (reactionGroups.any { it.viewerHasReacted })
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onQuoteClick) {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = stringResource(R.string.quotes),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onExternalShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()
        }

        if (replies.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.replies),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
                HorizontalDivider(thickness = 0.5.dp)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.shares),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@${actor.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.quotes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = post.actor.name ?: post.actor.handle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${post.actor.handle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "@${post.actor.handle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
