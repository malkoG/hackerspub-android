package pub.hackers.android.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.AccountLink
import pub.hackers.android.domain.model.ActorField
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    handle: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    onQuoteClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasNextPage && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    if (uiState.actionError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissActionError() },
            title = { Text(stringResource(R.string.action_error)) },
            text = { Text(uiState.actionError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissActionError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = "",
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.accent
                        )
                    }
                },
                trailingContent = {
                    if (uiState.actor != null) {
                        IconButton(onClick = {
                            val profileHandle = uiState.actor!!.handle
                            val normalizedHandle = if (profileHandle.startsWith("@")) profileHandle else "@$profileHandle"
                            val profileUrl = "https://hackers.pub/$normalizedHandle"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, profileUrl)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = colors.accent
                            )
                        }
                    }
                    if (!uiState.isViewer && uiState.actor != null) {
                        ProfileActionMenu(
                            followsViewer = uiState.followsViewer,
                            viewerBlocks = uiState.viewerBlocks,
                            isPerformingAction = uiState.isPerformingAction,
                            onRemoveFollower = { viewModel.removeFollower() },
                            onBlock = { viewModel.blockActor() },
                            onUnblock = { viewModel.unblockActor() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.actor == null -> {
                    FullScreenLoading()
                }
                uiState.error != null && uiState.actor == null -> {
                    ErrorMessage(
                        message = uiState.error ?: stringResource(R.string.error_generic),
                        onRetry = { viewModel.loadProfile(handle) }
                    )
                }
                uiState.actor != null -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn(state = listState) {
                            item {
                                ProfileHeader(
                                    avatarUrl = uiState.actor!!.avatarUrl,
                                    name = uiState.actor!!.name,
                                    handle = uiState.actor!!.handle,
                                    bio = uiState.bio,
                                    fields = uiState.fields,
                                    accountLinks = uiState.accountLinks,
                                    isViewer = uiState.isViewer,
                                    viewerFollows = uiState.viewerFollows,
                                    followsViewer = uiState.followsViewer,
                                    viewerBlocks = uiState.viewerBlocks,
                                    isPerformingAction = uiState.isPerformingAction,
                                    onFollowClick = { viewModel.followActor() },
                                    onUnfollowClick = { viewModel.unfollowActor() },
                                    onMentionClick = onProfileClick
                                )
                            }

                            item {
                                ProfileTabBar(
                                    selectedTab = uiState.selectedTab,
                                    onTabSelected = { viewModel.selectTab(it) }
                                )
                            }

                            items(
                                items = uiState.posts,
                                key = { it.id }
                            ) { post ->
                                PostCard(
                                    post = post,
                                    onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                    onProfileClick = onProfileClick,
                                    onReplyClick = { onReplyClick(post.sharedPost?.id ?: post.id) },
                                    onShareClick = {
                                        val targetId = post.sharedPost?.id ?: post.id
                                        if (post.viewerHasShared) {
                                            viewModel.unsharePost(targetId)
                                        } else {
                                            viewModel.sharePost(targetId)
                                        }
                                    },
                                    onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
                                    onReactionClick = null,
                                    onExternalShareClick = {
                                        val displayPost = post.sharedPost ?: post
                                        val shareUrl = displayPost.url ?: displayPost.iri
                                        if (shareUrl != null) {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareUrl)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, null))
                                        }
                                    },
                                    onQuotedPostClick = onPostClick
                                )
                                HorizontalDivider(
                                    color = LocalAppColors.current.divider,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    LoadingItem()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActionMenu(
    followsViewer: Boolean,
    viewerBlocks: Boolean,
    isPerformingAction: Boolean,
    onRemoveFollower: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More actions",
                tint = colors.accent
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (followsViewer) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.remove_follower),
                            color = colors.reaction
                        )
                    },
                    onClick = {
                        expanded = false
                        onRemoveFollower()
                    },
                    enabled = !isPerformingAction
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(if (viewerBlocks) R.string.unblock else R.string.block),
                        color = if (!viewerBlocks) colors.reaction else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    expanded = false
                    if (viewerBlocks) onUnblock() else onBlock()
                },
                enabled = !isPerformingAction
            )
        }
    }
}

@Composable
private fun ProfileTabBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(modifier = Modifier.fillMaxWidth()) {
        ProfileTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (tab) {
                        ProfileTab.POSTS -> stringResource(R.string.profile_tab_posts)
                        ProfileTab.NOTES -> stringResource(R.string.profile_tab_notes)
                        ProfileTab.ARTICLES -> stringResource(R.string.profile_tab_articles)
                    },
                    style = if (isSelected) typography.bodyLargeSemiBold else typography.bodyLarge,
                    color = if (isSelected) colors.accent else colors.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .then(
                            if (isSelected) Modifier.background(colors.accent) else Modifier
                        )
                )
            }
        }
    }

    HorizontalDivider(
        color = colors.divider,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ProfileHeader(
    avatarUrl: String,
    name: String?,
    handle: String,
    bio: String?,
    fields: List<ActorField>,
    accountLinks: List<AccountLink>,
    isViewer: Boolean,
    viewerFollows: Boolean,
    followsViewer: Boolean,
    viewerBlocks: Boolean,
    isPerformingAction: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onMentionClick: (String) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(AppShapes.avatarProfile)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        pub.hackers.android.ui.components.RichDisplayName(
            name = name,
            fallback = handle,
            style = typography.titleMedium,
            color = colors.textPrimary,
            emojiHeight = 22.dp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = handle,
            style = typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (!isViewer && (followsViewer || viewerBlocks)) {
            Spacer(modifier = Modifier.height(6.dp))
            RelationshipTags(
                followsViewer = followsViewer,
                viewerBlocks = viewerBlocks
            )
        }

        if (!isViewer) {
            Spacer(modifier = Modifier.height(12.dp))

            if (viewerFollows) {
                OutlinedButton(
                    onClick = onUnfollowClick,
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, colors.buttonOutline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.accent
                    )
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.unfollow),
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.follow),
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }
            }
        }

        if (!bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            HtmlContent(
                html = bio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                onMentionClick = onMentionClick
            )
        }

        if (accountLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileAttachments(accountLinks = accountLinks)
        } else if (fields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileFields(fields = fields)
        }
    }
}

@Composable
private fun ProfileAttachments(accountLinks: List<AccountLink>) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val uriHandler = LocalUriHandler.current
    val borderColor = colors.divider

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        accountLinks.forEachIndexed { index, link ->
            if (index > 0) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(link.url) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = link.name,
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = link.handle ?: compactUrl(link.url),
                    style = typography.bodyMedium,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (link.verified != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.profile_verified),
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF22C55E)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileFields(fields: List<ActorField>) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val borderColor = colors.divider

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        fields.forEachIndexed { index, field ->
            if (index > 0) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = field.name,
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                HtmlContent(
                    html = field.value,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

private fun compactUrl(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
}

@Composable
private fun RelationshipTags(
    followsViewer: Boolean,
    viewerBlocks: Boolean
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (followsViewer) {
            Text(
                text = stringResource(R.string.follows_you),
                style = typography.caption,
                color = colors.accentMuted,
                modifier = Modifier
                    .background(
                        colors.accentMuted.copy(alpha = 0.10f),
                        RoundedCornerShape(AppShapes.tagRadius)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (viewerBlocks) {
            Text(
                text = stringResource(R.string.blocked),
                style = typography.caption,
                color = colors.reaction,
                modifier = Modifier
                    .background(
                        colors.reaction.copy(alpha = 0.10f),
                        RoundedCornerShape(AppShapes.tagRadius)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
