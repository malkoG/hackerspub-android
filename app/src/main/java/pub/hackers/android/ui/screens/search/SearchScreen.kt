package pub.hackers.android.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import pub.hackers.android.ui.components.CompactTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import coil.compose.AsyncImage
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.PostCard

@Composable
fun SearchScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit = {},
    onQuoteClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactTopBar(title = stringResource(R.string.nav_search))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.search()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        FullScreenLoading()
                    }
                    uiState.error != null -> {
                        ErrorMessage(
                            message = uiState.error ?: stringResource(R.string.error_generic),
                            onRetry = { viewModel.search() }
                        )
                    }
                    uiState.hasSearched && uiState.posts.isEmpty() && uiState.actors.isEmpty() && uiState.resolvedObjectUrl == null -> {
                        ErrorMessage(
                            message = stringResource(R.string.no_results) + "\n" + stringResource(R.string.no_results_description),
                            icon = Icons.Filled.Search
                        )
                    }
                    uiState.posts.isNotEmpty() || uiState.resolvedObjectUrl != null || uiState.actors.isNotEmpty() -> {
                        LazyColumn {
                            if (uiState.resolvedObjectUrl != null) {
                                item {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = uiState.resolvedObjectUrl!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Filled.OpenInBrowser,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.resolvedObjectUrl))
                                            context.startActivity(intent)
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }

                            if (uiState.actors.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.search_accounts),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(
                                    items = uiState.actors,
                                    key = { "actor-${it.id}" }
                                ) { actor ->
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = actor.name ?: actor.handle,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "@${actor.handle}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        leadingContent = {
                                            AsyncImage(
                                                model = actor.avatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        },
                                        modifier = Modifier.clickable { onProfileClick(actor.handle) }
                                    )
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }

                            if (uiState.posts.isNotEmpty() && (uiState.actors.isNotEmpty() || uiState.resolvedObjectUrl != null)) {
                                item {
                                    Text(
                                        text = stringResource(R.string.search_posts),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
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
                                    onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
                                    onReactionClick = { onPostClick(post.sharedPost?.id ?: post.id) },
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
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                    else -> {
                        if (uiState.recentSearches.isNotEmpty()) {
                            LazyColumn {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.search_recent),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                            Text(stringResource(R.string.search_clear_recent))
                                        }
                                    }
                                }
                                items(
                                    items = uiState.recentSearches,
                                    key = { "recent-$it" }
                                ) { query ->
                                    ListItem(
                                        headlineContent = { Text(query) },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { viewModel.removeRecentSearch(query) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Clear,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.clickable { viewModel.selectRecentSearch(query) }
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
