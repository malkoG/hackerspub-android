package pub.hackers.android.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import pub.hackers.android.ui.components.LargeTitleHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Notification
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(title = "Notifications")
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    FullScreenLoading()
                }
                uiState.error != null && uiState.notifications.isEmpty() -> {
                    ErrorMessage(
                        message = uiState.error ?: stringResource(R.string.error_generic),
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_notifications),
                            style = typography.bodyLarge,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn(state = listState) {
                            items(
                                items = uiState.notifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onPostClick = onPostClick,
                                    onProfileClick = onProfileClick
                                )
                                HorizontalDivider(
                                    color = colors.divider,
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
private fun NotificationItem(
    notification: Notification,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val (icon, actionText) = when (notification) {
        is Notification.Follow -> Pair(
            Icons.Default.PersonAdd,
            stringResource(R.string.notification_follow)
        )
        is Notification.Mention -> Pair(
            Icons.Outlined.AlternateEmail,
            stringResource(R.string.notification_mention)
        )
        is Notification.Reply -> Pair(
            Icons.Default.Reply,
            stringResource(R.string.notification_reply)
        )
        is Notification.Quote -> Pair(
            Icons.Default.FormatQuote,
            stringResource(R.string.notification_quote)
        )
        is Notification.Share -> Pair(
            Icons.Default.Repeat,
            stringResource(R.string.notification_share)
        )
        is Notification.React -> Pair(
            Icons.Default.Favorite,
            notification.emoji?.let { "$it" } ?: stringResource(R.string.notification_react)
        )
    }

    val post = when (notification) {
        is Notification.Mention -> notification.post
        is Notification.Reply -> notification.post
        is Notification.Quote -> notification.post
        is Notification.Share -> notification.post
        is Notification.React -> notification.post
        else -> null
    }

    val actor = notification.actors.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    post != null -> onPostClick(post.id)
                    actor != null -> onProfileClick(actor.handle)
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actor != null) {
                    AsyncImage(
                        model = actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(AppShapes.avatarNotification)
                            .clip(CircleShape)
                            .clickable { onProfileClick(actor.handle) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        pub.hackers.android.ui.components.RichDisplayName(
                            name = actor?.name,
                            fallback = actor?.handle ?: "Someone",
                            style = typography.bodyLargeSemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = actionText,
                            style = typography.bodyLarge,
                            color = colors.textBody
                        )
                    }
                    Text(
                        text = formatRelativeTime(notification.created),
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }

            if (post != null) {
                Spacer(modifier = Modifier.size(8.dp))
                HtmlContent(
                    html = post.content,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    onTextClick = { onPostClick(post.id) }
                )
            }
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
        else -> "${duration.toDays() / 7}w"
    }
}
