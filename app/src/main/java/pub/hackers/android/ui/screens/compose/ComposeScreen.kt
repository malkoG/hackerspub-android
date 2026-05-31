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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
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
import pub.hackers.android.domain.model.QuotePolicy
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.MentionAutocomplete
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import kotlin.math.roundToInt

private data class MentionPopupPositionInputs(
    val scrollY: Int,
    val cursorRect: Rect,
    val textFieldBounds: Rect,
)

private fun calculateMentionPopupOffset(
    inputs: MentionPopupPositionInputs,
    paddingPx: Float,
    popupWidthPx: Float,
    popupYOffsetPx: Float,
): IntOffset {
    val cursorYInBox = inputs.cursorRect.bottom - inputs.scrollY + paddingPx
    val cursorXInBox = inputs.cursorRect.left + paddingPx
    val visibleCursorY = cursorYInBox.coerceIn(
        paddingPx,
        inputs.textFieldBounds.height - paddingPx
    )

    return IntOffset(
        x = cursorXInBox.roundToInt().coerceIn(
            0,
            (inputs.textFieldBounds.width - popupWidthPx).toInt().coerceAtLeast(0)
        ),
        y = (visibleCursorY + popupYOffsetPx).roundToInt()
    )
}

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
    var showQuotePolicyMenu by remember { mutableStateOf(false) }
    val quotePolicyLocked = uiState.visibility != PostVisibility.PUBLIC &&
        uiState.visibility != PostVisibility.UNLISTED
    val effectiveQuotePolicy = if (quotePolicyLocked) QuotePolicy.SELF else uiState.quotePolicy

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    // Track TextFieldValue for cursor position
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = uiState.content))
    }

    // Track cursor and text field positions for popup placement
    var cursorRect by remember { mutableStateOf(Rect.Zero) }
    var textFieldBounds by remember { mutableStateOf(Rect.Zero) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val mentionPopupPaddingPx = with(density) { 16.dp.toPx() }
    val mentionPopupWidthPx = with(density) { 280.dp.toPx() }
    val mentionPopupYOffsetPx = with(density) { 20.dp.toPx() }
    var mentionPopupOffset by remember { mutableStateOf(IntOffset.Zero) }

    LaunchedEffect(
        scrollState,
        mentionPopupPaddingPx,
        mentionPopupWidthPx,
        mentionPopupYOffsetPx
    ) {
        snapshotFlow {
            MentionPopupPositionInputs(
                scrollY = scrollState.value,
                cursorRect = cursorRect,
                textFieldBounds = textFieldBounds,
            )
        }.collect { inputs ->
            mentionPopupOffset = calculateMentionPopupOffset(
                inputs = inputs,
                paddingPx = mentionPopupPaddingPx,
                popupWidthPx = mentionPopupWidthPx,
                popupYOffsetPx = mentionPopupYOffsetPx,
            )
        }
    }

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
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = mentionPopupOffset,
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
                        imageVector = visibilityIcon(uiState.visibility),
                        contentDescription = visibilityLabel(uiState.visibility),
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visibilityLabel(uiState.visibility),
                        color = colors.textSecondary,
                        style = typography.labelMedium
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
                        visibilityMenuItem(
                            visibility = PostVisibility.PUBLIC,
                            selectedVisibility = uiState.visibility,
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.PUBLIC)
                                showVisibilityMenu = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.UNLISTED,
                            selectedVisibility = uiState.visibility,
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.UNLISTED)
                                showVisibilityMenu = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.FOLLOWERS,
                            selectedVisibility = uiState.visibility,
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.FOLLOWERS)
                                showVisibilityMenu = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.DIRECT,
                            selectedVisibility = uiState.visibility,
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.DIRECT)
                                showVisibilityMenu = false
                            }
                        )
                    }
                }

                TextButton(
                    onClick = { showQuotePolicyMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = quotePolicyIcon(effectiveQuotePolicy),
                        contentDescription = quotePolicyLabel(effectiveQuotePolicy),
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = quotePolicyShortLabel(effectiveQuotePolicy),
                        color = colors.textSecondary,
                        style = typography.labelMedium
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    DropdownMenu(
                        expanded = showQuotePolicyMenu,
                        onDismissRequest = { showQuotePolicyMenu = false }
                    ) {
                        quotePolicyMenuItem(
                            policy = QuotePolicy.EVERYONE,
                            selectedPolicy = effectiveQuotePolicy,
                            enabled = !quotePolicyLocked,
                            onClick = {
                                viewModel.updateQuotePolicy(QuotePolicy.EVERYONE)
                                showQuotePolicyMenu = false
                            }
                        )
                        quotePolicyMenuItem(
                            policy = QuotePolicy.FOLLOWERS,
                            selectedPolicy = effectiveQuotePolicy,
                            enabled = !quotePolicyLocked,
                            onClick = {
                                viewModel.updateQuotePolicy(QuotePolicy.FOLLOWERS)
                                showQuotePolicyMenu = false
                            }
                        )
                        quotePolicyMenuItem(
                            policy = QuotePolicy.SELF,
                            selectedPolicy = effectiveQuotePolicy,
                            enabled = true,
                            onClick = {
                                viewModel.updateQuotePolicy(QuotePolicy.SELF)
                                showQuotePolicyMenu = false
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
                        contentColor = colors.composeOnAccent,
                        disabledContainerColor = colors.composeAccent,
                        disabledContentColor = colors.composeOnAccent,
                    ),
                    modifier = Modifier.alpha(if (postEnabled) 1f else 0.4f)
                ) {
                    Text(
                        text = stringResource(R.string.post),
                        color = colors.composeOnAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun visibilityMenuItem(
    visibility: PostVisibility,
    selectedVisibility: PostVisibility,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    DropdownMenuItem(
        text = {
            Text(
                text = visibilityLabel(visibility),
                color = colors.textPrimary
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                visibilityIcon(visibility),
                contentDescription = null,
                tint = colors.textSecondary
            )
        },
        trailingIcon = {
            if (selectedVisibility == visibility) {
                Icon(Icons.Filled.Check, contentDescription = null)
            }
        }
    )
}

@Composable
private fun visibilityLabel(visibility: PostVisibility): String {
    return when (visibility) {
        PostVisibility.PUBLIC -> stringResource(R.string.visibility_public)
        PostVisibility.UNLISTED -> stringResource(R.string.visibility_unlisted)
        PostVisibility.FOLLOWERS -> stringResource(R.string.visibility_followers)
        PostVisibility.DIRECT -> stringResource(R.string.visibility_direct)
        PostVisibility.NONE -> stringResource(R.string.visibility_public)
    }
}

private fun visibilityIcon(visibility: PostVisibility) = when (visibility) {
    PostVisibility.PUBLIC -> Icons.Filled.Public
    PostVisibility.UNLISTED -> Icons.Outlined.Lock
    PostVisibility.FOLLOWERS -> Icons.Outlined.Group
    PostVisibility.DIRECT -> Icons.Outlined.Lock
    PostVisibility.NONE -> Icons.Filled.Public
}

@Composable
internal fun quotePolicyMenuItem(
    policy: QuotePolicy,
    selectedPolicy: QuotePolicy,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    DropdownMenuItem(
        text = {
            Text(
                text = quotePolicyLabel(policy),
                color = if (enabled) colors.textPrimary else colors.textSecondary
            )
        },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = {
            Icon(
                quotePolicyIcon(policy),
                contentDescription = null,
                tint = colors.textSecondary
            )
        },
        trailingIcon = {
            if (selectedPolicy == policy) {
                Icon(Icons.Filled.Check, contentDescription = null)
            }
        }
    )
}

@Composable
internal fun quotePolicyLabel(policy: QuotePolicy): String {
    return when (policy) {
        QuotePolicy.EVERYONE -> stringResource(R.string.quote_policy_everyone)
        QuotePolicy.FOLLOWERS -> stringResource(R.string.quote_policy_followers)
        QuotePolicy.SELF -> stringResource(R.string.quote_policy_self)
    }
}

internal fun quotePolicyIcon(policy: QuotePolicy) = when (policy) {
    QuotePolicy.EVERYONE -> Icons.Filled.Repeat
    QuotePolicy.FOLLOWERS -> Icons.Outlined.Group
    QuotePolicy.SELF -> Icons.Outlined.Lock
}

@Composable
internal fun quotePolicyShortLabel(policy: QuotePolicy): String {
    return when (policy) {
        QuotePolicy.EVERYONE -> stringResource(R.string.quote_policy_short_everyone)
        QuotePolicy.FOLLOWERS -> stringResource(R.string.quote_policy_short_followers)
        QuotePolicy.SELF -> stringResource(R.string.quote_policy_short_self)
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
