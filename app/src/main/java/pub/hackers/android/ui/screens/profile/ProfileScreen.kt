package pub.hackers.android.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import pub.hackers.android.ui.components.CompactTopBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    handle: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

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
            CompactTopBar(
                title = uiState.actor?.name ?: handle,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
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
                                    isViewer = uiState.isViewer,
                                    viewerFollows = uiState.viewerFollows,
                                    followsViewer = uiState.followsViewer,
                                    viewerBlocks = uiState.viewerBlocks,
                                    isPerformingAction = uiState.isPerformingAction,
                                    onFollowClick = { viewModel.followActor() },
                                    onUnfollowClick = { viewModel.unfollowActor() }
                                )
                                HorizontalDivider()
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
                                    onQuotedPostClick = onPostClick
                                )
                                HorizontalDivider(thickness = 0.5.dp)
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
            if (followsViewer) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_follower)) },
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
                        color = if (!viewerBlocks) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
private fun ProfileHeader(
    avatarUrl: String,
    name: String?,
    handle: String,
    bio: String?,
    isViewer: Boolean,
    viewerFollows: Boolean,
    followsViewer: Boolean,
    viewerBlocks: Boolean,
    isPerformingAction: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
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
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = name ?: handle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "@$handle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isViewer && (followsViewer || viewerBlocks)) {
                Spacer(modifier = Modifier.width(8.dp))
                RelationshipTags(
                    followsViewer = followsViewer,
                    viewerBlocks = viewerBlocks
                )
            }
        }

        if (!isViewer) {
            Spacer(modifier = Modifier.height(12.dp))

            if (viewerFollows) {
                Button(
                    onClick = onUnfollowClick,
                    enabled = !isPerformingAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.width(200.dp)
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.unfollow))
                    }
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isPerformingAction,
                    modifier = Modifier.width(200.dp)
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.follow))
                    }
                }
            }
        }

        if (!bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HtmlContent(
                html = bio,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RelationshipTags(
    followsViewer: Boolean,
    viewerBlocks: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (followsViewer) {
            Text(
                text = stringResource(R.string.follows_you),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (viewerBlocks) {
            Text(
                text = stringResource(R.string.blocked),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
