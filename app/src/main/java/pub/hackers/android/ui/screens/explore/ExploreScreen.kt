package pub.hackers.android.ui.screens.explore

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.ReactionPicker
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit = {},
    onSignInClick: () -> Unit,
    isLoggedIn: Boolean,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val items = viewModel.posts.collectAsLazyPagingItems()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    // Scroll to top when tab changes.
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    // Reaction picker bottom sheet — look up the currently-loaded post.
    val pickerPostId = uiState.reactionPickerPostId
    if (pickerPostId != null) {
        val pickerPost = items.itemSnapshotList.items.find {
            it.id == pickerPostId || it.sharedPost?.id == pickerPostId
        }
        val targetPost = pickerPost?.sharedPost ?: pickerPost
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideReactionPicker() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReactionPicker(
                reactionGroups = targetPost?.reactionGroups ?: emptyList(),
                isSubmitting = false,
                onEmojiSelect = { emoji ->
                    pickerPost?.let { viewModel.toggleReaction(it, emoji) }
                },
                onClose = { viewModel.hideReactionPicker() }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = "Explore",
                trailingContent = if (!isLoggedIn) {
                    {
                        TextButton(onClick = onSignInClick) {
                            Text(stringResource(R.string.sign_in))
                        }
                    }
                } else null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Custom tab row
            Row(modifier = Modifier.fillMaxWidth()) {
                ExploreTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (tab) {
                                ExploreTab.LOCAL -> "Local"
                                ExploreTab.GLOBAL -> "Global"
                            },
                            style = if (isSelected) typography.bodyLargeSemiBold else typography.bodyLarge,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(colors.accent)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = colors.divider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val refresh = items.loadState.refresh
                when {
                    refresh is LoadState.Error && items.itemCount == 0 -> {
                        ErrorMessage(
                            message = refresh.error.message
                                ?: stringResource(R.string.error_generic),
                            onRetry = { items.refresh() }
                        )
                    }

                    items.itemCount == 0 && refresh is LoadState.NotLoading && refresh.endOfPaginationReached -> {
                        ErrorMessage(
                            message = stringResource(R.string.no_posts),
                            onRefresh = { items.refresh() }
                        )
                    }

                    items.itemCount == 0 -> {
                        FullScreenLoading()
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = refresh is LoadState.Loading,
                            onRefresh = { items.refresh() }
                        ) {
                            LazyColumn(state = listState) {
                                items(
                                    count = items.itemCount,
                                    key = items.itemKey { it.id }
                                ) { index ->
                                    val post = items[index] ?: return@items
                                    PostCard(
                                        post = post,
                                        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                        onProfileClick = onProfileClick,
                                        onReplyClick = if (isLoggedIn) {
                                            { onReplyClick(post.sharedPost?.id ?: post.id) }
                                        } else null,
                                        onShareClick = if (isLoggedIn) {
                                            {
                                                if (post.viewerHasShared) {
                                                    viewModel.unsharePost(post.id)
                                                } else {
                                                    viewModel.sharePost(post.id)
                                                }
                                            }
                                        } else null,
                                        onQuoteClick = if (isLoggedIn) {
                                            { onQuoteClick(post.sharedPost?.id ?: post.id) }
                                        } else null,
                                        onReactionClick = if (isLoggedIn) {
                                            { viewModel.toggleFavourite(post) }
                                        } else null,
                                        onReactionLongPress = if (isLoggedIn) {
                                            {
                                                viewModel.showReactionPicker(
                                                    post.sharedPost?.id ?: post.id
                                                )
                                            }
                                        } else null,
                                        onBookmarkClick = if (isLoggedIn) {
                                            {
                                                val displayPost = post.sharedPost ?: post
                                                Toast.makeText(
                                                    context,
                                                    context.getString(
                                                        if (displayPost.viewerHasBookmarked) {
                                                            R.string.bookmark_removed
                                                        } else {
                                                            R.string.bookmarked
                                                        }
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                viewModel.toggleBookmark(post)
                                            }
                                        } else null,
                                        onExternalShareClick = {
                                            val displayPost = post.sharedPost ?: post
                                            val shareUrl = displayPost.url ?: displayPost.iri
                                            if (shareUrl != null) {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(sendIntent, null)
                                                )
                                            }
                                        },
                                        onQuotedPostClick = onPostClick
                                    )
                                    HorizontalDivider(
                                        color = colors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                if (items.loadState.append is LoadState.Loading) {
                                    item { LoadingItem() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
