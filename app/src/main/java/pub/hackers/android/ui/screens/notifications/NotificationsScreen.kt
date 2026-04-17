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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Notification
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LargeTitleHeader
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
    val items = viewModel.notifications.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val colors = LocalAppColors.current

    // Mark as seen once the initial refresh finishes successfully with items.
    LaunchedEffect(items.loadState.refresh, items.itemCount) {
        if (items.loadState.refresh is LoadState.NotLoading && items.itemCount > 0) {
            viewModel.markAsSeen()
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
            val refresh = items.loadState.refresh
            when {
                refresh is LoadState.Error && items.itemCount == 0 -> {
                    ErrorMessage(
                        message = refresh.error.message ?: stringResource(R.string.error_generic),
                        onRetry = { items.refresh() }
                    )
                }

                items.itemCount == 0 && refresh is LoadState.NotLoading && refresh.endOfPaginationReached -> {
                    ErrorMessage(
                        message = stringResource(R.string.no_notifications),
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
                                val notification = items[index] ?: return@items
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

        is Notification.React -> {
            val othersCount = notification.actors.size - 1
            val prefix = if (othersCount > 0) {
                pluralStringResource(
                    R.plurals.notification_and_others,
                    othersCount,
                    othersCount
                ) + " "
            } else ""
            val actionStr = if (notification.emoji != null) {
                prefix + stringResource(R.string.notification_react_with_emoji, notification.emoji)
            } else {
                prefix + stringResource(R.string.notification_react)
            }
            Pair(Icons.Default.Favorite, actionStr)
        }
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
                    val relativeTime = remember(notification.created) {
                        formatRelativeTime(notification.created)
                    }
                    Text(
                        text = relativeTime,
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
