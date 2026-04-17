package pub.hackers.android.ui.screens.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.MentionAutocomplete
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import kotlin.math.roundToInt

@Composable
fun ComposeScreen(
    replyToId: String?,
    quotedPostId: String? = null,
    onPostSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showVisibilityMenu by remember { mutableStateOf(false) }

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    // Track TextFieldValue for cursor position
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = uiState.content))
    }

    // Track cursor and text field positions for popup placement
    var cursorRect by remember { mutableStateOf(Rect.Zero) }
    var textFieldBounds by remember { mutableStateOf(Rect.Zero) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val popupHeight = with(density) { 200.dp.toPx() } // Estimated popup height
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to keep cursor visible when text overflows
    LaunchedEffect(cursorRect) {
        if (cursorRect != Rect.Zero) {
            val cursorBottom = cursorRect.bottom.toInt()
            val cursorTop = cursorRect.top.toInt()
            val viewportTop = scrollState.value
            val viewportBottom = viewportTop + scrollState.viewportSize

            if (cursorBottom > viewportBottom) {
                scrollState.animateScrollTo(cursorBottom - scrollState.viewportSize)
            } else if (cursorTop < viewportTop) {
                scrollState.animateScrollTo(cursorTop)
            }
        }
    }

    // Sync TextFieldValue with ViewModel state changes (e.g., when mention is selected)
    LaunchedEffect(uiState.content, uiState.cursorPosition) {
        if (textFieldValue.text != uiState.content) {
            textFieldValue = textFieldValue.copy(
                text = uiState.content,
                selection = TextRange(uiState.cursorPosition)
            )
        }
    }

    LaunchedEffect(replyToId) {
        replyToId?.let { viewModel.setReplyTarget(it) }
    }

    LaunchedEffect(quotedPostId) {
        quotedPostId?.let { viewModel.setQuotedPost(it) }
    }

    LaunchedEffect(uiState.isPosted) {
        if (uiState.isPosted) {
            onPostSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val postEnabled = uiState.content.isNotBlank() && !uiState.isPosting

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = if (replyToId != null) stringResource(R.string.reply) else stringResource(
                        R.string.compose
                    ),
                    style = typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Reply target preview
                ReplyTargetSection(
                    isLoading = uiState.isLoadingReplyTarget,
                    replyTargetPost = uiState.replyTargetPost,
                )

                Box(modifier = Modifier.weight(1f)) {
                    // Custom text field with cursor position tracking
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                textFieldBounds = coordinates.boundsInWindow()
                            },
                        shape = RoundedCornerShape(4.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.divider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                .padding(16.dp)
                                .verticalScroll(scrollState)
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue: TextFieldValue ->
                                    textFieldValue = newValue
                                    viewModel.updateContent(
                                        content = newValue.text,
                                        cursorPosition = newValue.selection.start
                                    )
                                },
                                onTextLayout = { result: TextLayoutResult ->
                                    textLayoutResult = result
                                    // Update cursor position
                                    val cursorPos = textFieldValue.selection.start
                                        .coerceIn(0, textFieldValue.text.length)
                                    cursorRect =
                                        if (textFieldValue.text.isNotEmpty() || cursorPos == 0) {
                                            result.getCursorRect(cursorPos)
                                        } else {
                                            Rect.Zero
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                enabled = !uiState.isPosting,
                                textStyle = typography.bodyLarge.copy(
                                    color = colors.textBody
                                ),
                                cursorBrush = SolidColor(colors.composeAccent),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (textFieldValue.text.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.compose_hint),
                                                style = typography.bodyLarge,
                                                color = colors.textSecondary
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    // Mention autocomplete popup
                    if (uiState.mentionSuggestions.isNotEmpty() || uiState.isLoadingMentions) {
                        // Calculate cursor position accounting for padding and scroll
                        val paddingPx = with(density) { 16.dp.toPx() }
                        val cursorYInBox = cursorRect.bottom - scrollState.value + paddingPx
                        val cursorXInBox = cursorRect.left + paddingPx

                        // Clamp to visible area
                        val visibleCursorY =
                            cursorYInBox.coerceIn(paddingPx, textFieldBounds.height - paddingPx)

                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(
                                x = cursorXInBox.roundToInt().coerceIn(
                                    0,
                                    (textFieldBounds.width - with(density) { 280.dp.toPx() }).toInt()
                                        .coerceAtLeast(0)
                                ),
                                y = (visibleCursorY + with(density) { 20.dp.toPx() }).roundToInt()
                            ),
                            properties = PopupProperties(focusable = false)
                        ) {
                            MentionAutocomplete(
                                suggestions = uiState.mentionSuggestions,
                                isLoading = uiState.isLoadingMentions,
                                onSuggestionSelected = { actor ->
                                    val (newContent, newCursor) = viewModel.selectMention(actor)
                                    textFieldValue = TextFieldValue(
                                        text = newContent,
                                        selection = TextRange(newCursor)
                                    )
                                },
                                modifier = Modifier.width(280.dp)
                            )
                        }
                    }
                }

                // Quoted post preview
                QuotedPostSection(
                    isLoading = uiState.isLoadingQuotedPost,
                    quotedPost = uiState.quotedPost,
                )
            }
            // Close inner content Column

            // Visibility toolbar pinned above keyboard
            HorizontalDivider(color = colors.divider)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { showVisibilityMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = when (uiState.visibility) {
                            PostVisibility.PUBLIC -> Icons.Filled.Public
                            PostVisibility.UNLISTED -> Icons.Outlined.Lock
                            PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                            else -> Icons.Filled.Public
                        },
                        contentDescription = when (uiState.visibility) {
                            PostVisibility.PUBLIC -> stringResource(R.string.visibility_public)
                            PostVisibility.UNLISTED -> stringResource(R.string.visibility_unlisted)
                            PostVisibility.FOLLOWERS -> stringResource(R.string.visibility_followers)
                            else -> stringResource(R.string.visibility_public)
                        },
                        tint = colors.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    DropdownMenu(
                        expanded = showVisibilityMenu,
                        onDismissRequest = { showVisibilityMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.visibility_public),
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.PUBLIC)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = colors.textSecondary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.visibility_unlisted),
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.UNLISTED)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = colors.textSecondary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.visibility_followers),
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.FOLLOWERS)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = colors.textSecondary
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.post() },
                    enabled = postEnabled,
                    shape = RoundedCornerShape(AppShapes.pillRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.composeAccent,
                        contentColor = Color.White,
                        disabledContainerColor = colors.composeAccent,
                        disabledContentColor = Color.White,
                    ),
                    modifier = Modifier.alpha(if (postEnabled) 1f else 0.4f)
                ) {
                    Text(
                        text = stringResource(R.string.post),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyTargetPreview(
    post: Post,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                pub.hackers.android.ui.components.RichDisplayName(
                    name = post.actor.name,
                    fallback = post.actor.handle,
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    emojiHeight = 14.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                HtmlContent(
                    html = post.content,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuotedPostPreview(
    post: Post,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = post.actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    pub.hackers.android.ui.components.RichDisplayName(
                        name = post.actor.name,
                        fallback = post.actor.handle,
                        style = typography.labelMedium,
                        color = colors.textSecondary,
                        emojiHeight = 14.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HtmlContent(
                    html = post.content,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun ColumnScope.ReplyTargetSection(
    isLoading: Boolean,
    replyTargetPost: Post?,
) {
    when {
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        replyTargetPost != null -> {
            ReplyTargetPreview(
                post = replyTargetPost,
                modifier = Modifier.alpha(0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun ColumnScope.QuotedPostSection(
    isLoading: Boolean,
    quotedPost: Post?,
) {
    when {
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        quotedPost != null -> {
            Spacer(modifier = Modifier.height(8.dp))
            QuotedPostPreview(post = quotedPost)
        }
    }
}
