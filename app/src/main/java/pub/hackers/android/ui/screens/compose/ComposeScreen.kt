package pub.hackers.android.ui.screens.compose

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pub.hackers.android.ui.components.LargeTitleHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
    val density = LocalDensity.current
    val popupHeight = with(density) { 200.dp.toPx() } // Estimated popup height

    // Sync TextFieldValue with ViewModel state changes (e.g., when mention is selected)
    LaunchedEffect(uiState.content, uiState.cursorPosition) {
        if (textFieldValue.text != uiState.content) {
            textFieldValue = TextFieldValue(
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

    val postEnabled = uiState.content.isNotBlank() && !uiState.isPosting

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = if (replyToId != null) stringResource(R.string.reply) else stringResource(R.string.compose),
                leadingContent = {
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = typography.bodyLarge,
                            color = colors.accent
                        )
                    }
                },
                trailingContent = {
                    Button(
                        onClick = { viewModel.post() },
                        enabled = postEnabled,
                        shape = RoundedCornerShape(AppShapes.pillRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White,
                            disabledContainerColor = colors.accent,
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
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Reply target preview
            if (uiState.isLoadingReplyTarget) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (uiState.replyTargetPost != null) {
                ReplyTargetPreview(
                    post = uiState.replyTargetPost!!,
                    modifier = Modifier.alpha(0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                            ) { focusRequester.requestFocus() }
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
                                cursorRect = if (textFieldValue.text.isNotEmpty() || cursorPos == 0) {
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
                            cursorBrush = SolidColor(colors.accent),
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
                    val visibleCursorY = cursorYInBox.coerceIn(paddingPx, textFieldBounds.height - paddingPx)

                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(
                            x = cursorXInBox.roundToInt().coerceIn(0, (textFieldBounds.width - with(density) { 280.dp.toPx() }).toInt().coerceAtLeast(0)),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showVisibilityMenu = true }
                ) {
                    Icon(
                        imageVector = when (uiState.visibility) {
                            PostVisibility.PUBLIC -> Icons.Filled.Public
                            PostVisibility.UNLISTED -> Icons.Outlined.Lock
                            PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                            else -> Icons.Filled.Public
                        },
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (uiState.visibility) {
                            PostVisibility.PUBLIC -> stringResource(R.string.visibility_public)
                            PostVisibility.UNLISTED -> stringResource(R.string.visibility_unlisted)
                            PostVisibility.FOLLOWERS -> stringResource(R.string.visibility_followers)
                            else -> stringResource(R.string.visibility_public)
                        },
                        color = colors.textSecondary
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
                Text(
                    text = post.actor.name ?: post.actor.handle,
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
