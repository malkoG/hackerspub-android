package pub.hackers.android.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun SearchScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit = {},
    onQuoteClick: (String) -> Unit = {},
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    LaunchedEffect(initialQuery) {
        if (initialQuery != null && !uiState.hasSearched) {
            viewModel.updateQuery(initialQuery)
            viewModel.search()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(title = stringResource(R.string.nav_search))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Custom styled search bar
            BasicTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                singleLine = true,
                textStyle = typography.bodyLarge.copy(color = colors.textBody),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.search()
                        keyboardController?.hide()
                    }
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = colors.surface,
                                shape = RoundedCornerShape(AppShapes.searchBarRadius)
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = typography.bodyLarge,
                                    color = colors.textSecondary
                                )
                            }
                            innerTextField()
                        }

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
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
                    uiState.hasSearched && uiState.posts.isEmpty() -> {
                        ErrorMessage(message = stringResource(R.string.no_results))
                    }
                    uiState.posts.isNotEmpty() -> {
                        LazyColumn {
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
                                    color = colors.divider,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = typography.bodyLarge,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
