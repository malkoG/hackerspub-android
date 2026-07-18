package pub.hackers.android.ui.screens.compose

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
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
import java.util.Locale

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
    val cursorMinY = paddingPx
    val cursorMaxY = (inputs.textFieldBounds.height - paddingPx).coerceAtLeast(cursorMinY)
    val visibleCursorY = cursorYInBox.coerceIn(
        cursorMinY,
        cursorMaxY
    )

    return IntOffset(
        x = cursorXInBox.roundToInt().coerceIn(
            0,
            (inputs.textFieldBounds.width - popupWidthPx).toInt().coerceAtLeast(0)
        ),
        y = (visibleCursorY + popupYOffsetPx).roundToInt()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    replyToId: String?,
    quotedPostId: String? = null,
    initialContent: String? = null,
    editPostId: String? = null,
    onPostSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val quotePolicyLocked = uiState.visibility != PostVisibility.PUBLIC &&
        uiState.visibility != PostVisibility.UNLISTED
    val effectiveQuotePolicy = if (quotePolicyLocked) QuotePolicy.SELF else uiState.quotePolicy
    val isEditing = editPostId != null || uiState.editPostId != null

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
        onResult = { uris -> viewModel.addMediaUris(uris) }
    )
    var selectedAttachmentId by remember { mutableStateOf<String?>(null) }
    var showPollEditor by remember { mutableStateOf(false) }
    val selectedAttachment = uiState.mediaAttachments.firstOrNull { it.localId == selectedAttachmentId }

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
        if (!isEditing) {
            quotedPostId?.let { viewModel.setQuotedPost(it) }
        }
    }

    LaunchedEffect(initialContent) {
        if (!isEditing) {
            initialContent?.let { viewModel.setInitialContent(it) }
        }
    }

    LaunchedEffect(editPostId) {
        editPostId?.let { viewModel.setEditTarget(it) }
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

    val mediaReady = uiState.mediaAttachments.all { attachment ->
        !attachment.isUploading &&
            attachment.uploadedMediumId != null &&
            attachment.altText.isNotBlank() &&
            attachment.error == null
    }
    val postEnabled = uiState.content.isNotBlank() &&
        uiState.language.isNotBlank() &&
        !uiState.isPosting &&
        !uiState.isLoadingEditTarget &&
        mediaReady

    if (showPollEditor) {
        PollEditorScreen(
            title = uiState.pollTitle,
            options = uiState.pollOptions,
            multiple = uiState.pollMultiple,
            durationMinutes = uiState.pollDurationMinutes,
            editing = uiState.pollEnabled,
            enabled = !uiState.isPosting,
            onTitleChange = viewModel::updatePollTitle,
            onOptionChange = viewModel::updatePollOption,
            onAddOption = viewModel::addPollOption,
            onRemoveOption = viewModel::removePollOption,
            onMultipleChange = viewModel::setPollMultiple,
            onDurationChange = viewModel::updatePollDurationMinutes,
            onDismiss = { showPollEditor = false },
            onSave = {
                viewModel.attachPoll()
                showPollEditor = false
            },
        )
        return
    }

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
                    text = when {
                        isEditing -> stringResource(R.string.compose_edit)
                        replyToId != null -> stringResource(R.string.reply)
                        else -> stringResource(R.string.compose)
                    },
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
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                val useCompactContextPreviews = maxHeight < 420.dp

                Column(modifier = Modifier.fillMaxSize()) {
                    // Reply target preview
                    ReplyTargetSection(
                        isLoading = uiState.isLoadingReplyTarget,
                        replyTargetPost = uiState.replyTargetPost,
                        compact = useCompactContextPreviews,
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

                        // Collapsed poll floats over the text area instead of taking layout space
                        if (uiState.pollEnabled) {
                            PollSummaryChip(
                                optionCount = uiState.pollOptions.count { it.isNotBlank() },
                                durationMinutes = uiState.pollDurationMinutes,
                                onEdit = { showPollEditor = true },
                                onRemove = viewModel::removePoll,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                            )
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MediaAttachmentSection(
                            attachments = uiState.mediaAttachments,
                            onRemove = viewModel::removeMediaAttachment,
                            onAttachmentClick = { selectedAttachmentId = it },
                        )

                        // Quoted post preview
                        QuotedPostSection(
                            isLoading = uiState.isLoadingQuotedPost,
                            quotedPost = uiState.quotedPost,
                            compact = useCompactContextPreviews,
                        )
                    }
                }
            }
            // Close inner content Column

            // Composer controls pinned above keyboard
            HorizontalDivider(color = colors.divider)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
            ) {
                IconButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isEditing && !uiState.isPosting && uiState.mediaAttachments.size < 20,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = stringResource(R.string.attach_images),
                        tint = colors.textSecondary,
                    )
                }

                IconButton(
                    onClick = { showPollEditor = true },
                    enabled = !isEditing && !uiState.isPosting,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Poll,
                        contentDescription = stringResource(R.string.poll_add),
                        tint = if (uiState.pollEnabled) colors.accent else colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PostSettingsButton(
                    visibility = uiState.visibility,
                    onVisibilityChange = viewModel::updateVisibility,
                    visibilityEnabled = !isEditing,
                    quotePolicy = effectiveQuotePolicy,
                    onQuotePolicyChange = viewModel::updateQuotePolicy,
                    quotePolicyLocked = quotePolicyLocked,
                    enabled = !uiState.isPosting,
                )

                val languageOptions = remember(uiState.language, uiState.suggestedLanguages) {
                    (listOf(uiState.language) + uiState.suggestedLanguages)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                }
                TextButton(
                    onClick = { showLanguageMenu = true },
                    enabled = !uiState.isPosting && languageOptions.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = stringResource(R.string.compose_language_label),
                        tint = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.language.ifBlank {
                            stringResource(R.string.compose_language_label)
                        },
                        color = colors.textSecondary,
                        style = typography.labelMedium,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                    ) {
                        languageOptions.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(languageOptionLabel(language)) },
                                leadingIcon = {
                                    if (language == uiState.language) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.updateLanguage(language)
                                    showLanguageMenu = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                        text = stringResource(if (isEditing) R.string.save else R.string.post),
                        color = colors.composeOnAccent
                    )
                }
            }
        }
    }

    selectedAttachment?.let { attachment ->
        ModalBottomSheet(
            onDismissRequest = { selectedAttachmentId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            MediaAttachmentEditorSheet(
                attachment = attachment,
                onAltTextChange = viewModel::updateMediaAltText,
                onGenerateAltText = viewModel::generateAltText,
                onRemove = {
                    viewModel.removeMediaAttachment(attachment.localId)
                    selectedAttachmentId = null
                },
                onSave = { selectedAttachmentId = null },
            )
        }
    }

}

@Composable
private fun languageOptionLabel(language: String): String {
    val configuration = LocalConfiguration.current
    val uiLocale = remember(configuration) {
        if (configuration.locales.size() > 0) {
            configuration.locales[0]
        } else {
            Locale.getDefault()
        }
    }
    return remember(language, uiLocale) {
        val locale = Locale.forLanguageTag(language)
        val nativeName = locale.getDisplayLanguage(locale).ifBlank { language }
        val uiName = locale.getDisplayLanguage(uiLocale).ifBlank { language }
        if (nativeName.equals(uiName, ignoreCase = true)) {
            "$nativeName ($language)"
        } else {
            "$nativeName ($uiName, $language)"
        }
    }
}

private fun pollDurationLabelRes(minutes: Long): Int = when (minutes) {
    5L -> R.string.poll_duration_5_minutes
    30L -> R.string.poll_duration_30_minutes
    60L -> R.string.poll_duration_1_hour
    360L -> R.string.poll_duration_6_hours
    1440L -> R.string.poll_duration_1_day
    4320L -> R.string.poll_duration_3_days
    10080L -> R.string.poll_duration_7_days
    else -> R.string.poll_duration_1_day
}

@Composable
private fun PollSummaryChip(
    optionCount: Int,
    durationMinutes: Long,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(AppShapes.pillRadius),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.divider),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Poll,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.poll_options_count,
                    optionCount,
                    optionCount,
                ) + " · " + stringResource(pollDurationLabelRes(durationMinutes)),
                color = colors.textPrimary,
                style = typography.labelMedium,
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.poll_remove),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun PostSettingsButton(
    visibility: PostVisibility,
    onVisibilityChange: (PostVisibility) -> Unit,
    visibilityEnabled: Boolean,
    quotePolicy: QuotePolicy,
    onQuotePolicyChange: (QuotePolicy) -> Unit,
    quotePolicyLocked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableStateOf(0) }
    val customized = visibility != PostVisibility.PUBLIC || quotePolicy != QuotePolicy.EVERYONE

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.onGloballyPositioned { anchorHeightPx = it.size.height },
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.compose_post_settings),
                tint = if (customized) colors.accent else colors.textSecondary,
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset(
                    x = 0,
                    y = -(anchorHeightPx + with(density) { 6.dp.roundToPx() }),
                ),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(AppShapes.pillRadius),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.divider),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(220.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = stringResource(R.string.visibility),
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.PUBLIC,
                            selectedVisibility = visibility,
                            enabled = visibilityEnabled,
                            onClick = {
                                onVisibilityChange(PostVisibility.PUBLIC)
                                expanded = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.UNLISTED,
                            selectedVisibility = visibility,
                            enabled = visibilityEnabled,
                            onClick = {
                                onVisibilityChange(PostVisibility.UNLISTED)
                                expanded = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.FOLLOWERS,
                            selectedVisibility = visibility,
                            enabled = visibilityEnabled,
                            onClick = {
                                onVisibilityChange(PostVisibility.FOLLOWERS)
                                expanded = false
                            }
                        )
                        visibilityMenuItem(
                            visibility = PostVisibility.DIRECT,
                            selectedVisibility = visibility,
                            enabled = visibilityEnabled,
                            onClick = {
                                onVisibilityChange(PostVisibility.DIRECT)
                                expanded = false
                            }
                        )

                        HorizontalDivider(color = colors.divider)

                        Text(
                            text = stringResource(R.string.quote_permission),
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        quotePolicyMenuItem(
                            policy = QuotePolicy.EVERYONE,
                            selectedPolicy = quotePolicy,
                            enabled = !quotePolicyLocked,
                            onClick = {
                                onQuotePolicyChange(QuotePolicy.EVERYONE)
                                expanded = false
                            }
                        )
                        quotePolicyMenuItem(
                            policy = QuotePolicy.FOLLOWERS,
                            selectedPolicy = quotePolicy,
                            enabled = !quotePolicyLocked,
                            onClick = {
                                onQuotePolicyChange(QuotePolicy.FOLLOWERS)
                                expanded = false
                            }
                        )
                        quotePolicyMenuItem(
                            policy = QuotePolicy.SELF,
                            selectedPolicy = quotePolicy,
                            enabled = true,
                            onClick = {
                                onQuotePolicyChange(QuotePolicy.SELF)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PollEditorScreen(
    title: String,
    options: List<String>,
    multiple: Boolean,
    durationMinutes: Long,
    editing: Boolean,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onMultipleChange: (Boolean) -> Unit,
    onDurationChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    var durationMenuOpen by remember { mutableStateOf(false) }

    val durationOptions = listOf(
        R.string.poll_duration_5_minutes to 5L,
        R.string.poll_duration_30_minutes to 30L,
        R.string.poll_duration_1_hour to 60L,
        R.string.poll_duration_6_hours to 360L,
        R.string.poll_duration_1_day to 1440L,
        R.string.poll_duration_3_days to 4320L,
        R.string.poll_duration_7_days to 10080L,
    )
    val currentDurationLabel = pollDurationLabelRes(durationMinutes)

    val trimmedOptions = options.map { it.trim() }.filter { it.isNotEmpty() }
    val canSave = enabled &&
        title.trim().isNotEmpty() &&
        trimmedOptions.size >= 2 &&
        trimmedOptions.size == trimmedOptions.distinct().size

    BackHandler(onBack = onDismiss)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(if (editing) R.string.poll_edit else R.string.poll_new),
                    style = typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                TextButton(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        color = if (canSave) colors.accent else colors.textSecondary,
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.poll_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            options.forEachIndexed { index, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { onOptionChange(index, it) },
                        enabled = enabled,
                        singleLine = true,
                        label = { Text(stringResource(R.string.poll_option_hint, index + 1)) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemoveOption(index) },
                        enabled = enabled && options.size > 2,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.poll_remove_option),
                            tint = colors.textSecondary,
                        )
                    }
                }
            }

            if (options.size < 20) {
                TextButton(onClick = onAddOption, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.poll_add_option))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.poll_duration_label),
                    color = colors.textPrimary,
                    style = typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { durationMenuOpen = true }, enabled = enabled) {
                        Text(
                            text = stringResource(currentDurationLabel),
                            color = colors.textSecondary,
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = durationMenuOpen,
                        onDismissRequest = { durationMenuOpen = false },
                    ) {
                        durationOptions.forEach { (labelRes, minutes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    onDurationChange(minutes)
                                    durationMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Switch(checked = multiple, onCheckedChange = onMultipleChange, enabled = enabled)
                Text(
                    text = stringResource(R.string.poll_multiple_label),
                    color = colors.textPrimary,
                    style = typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MediaAttachmentSection(
    attachments: List<ComposeMediaAttachment>,
    onRemove: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        HorizontalDivider(color = LocalAppColors.current.divider)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            itemsIndexed(
                items = attachments,
                key = { _, attachment -> attachment.localId },
            ) { index, attachment ->
                MediaAttachmentThumbnailCard(
                    attachment = attachment,
                    index = index,
                    onRemove = onRemove,
                    onClick = { onAttachmentClick(attachment.localId) },
                )
            }
        }
    }
}

@Composable
private fun MediaAttachmentThumbnailCard(
    attachment: ComposeMediaAttachment,
    index: Int,
    onRemove: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val requiresAltText = !attachment.isUploading &&
        attachment.uploadedMediumId != null &&
        attachment.altText.isBlank() &&
        attachment.error == null
    val hasWarning = requiresAltText || attachment.error != null
    val warningColor = MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier
            .width(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, if (hasWarning) warningColor else colors.divider),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(AppShapes.mediaRadius))
            ) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = attachment.altText.ifBlank { null },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (attachment.isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.65f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { attachment.uploadProgress / 100f },
                            modifier = Modifier.size(32.dp),
                            color = colors.composeAccent,
                        )
                    }
                }
                if (requiresAltText) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(24.dp),
                        shape = CircleShape,
                        color = warningColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "!",
                                style = typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onRemove(attachment.localId) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_image),
                        tint = colors.composeOnAccent,
                    )
                }
            }

            if (attachment.isUploading) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { attachment.uploadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.composeAccent,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (requiresAltText) {
                    "! ${stringResource(R.string.attached_image_number, index + 1)}"
                } else {
                    stringResource(R.string.attached_image_number, index + 1)
                },
                style = typography.labelSmall,
                color = if (hasWarning) warningColor else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MediaAttachmentEditorSheet(
    attachment: ComposeMediaAttachment,
    onAltTextChange: (String, String) -> Unit,
    onGenerateAltText: (String) -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.alt_text_required),
                style = typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove_image),
                    tint = colors.textSecondary,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(AppShapes.mediaRadius))
        ) {
            AsyncImage(
                model = attachment.uri,
                contentDescription = attachment.altText.ifBlank { null },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (attachment.isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.65f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { attachment.uploadProgress / 100f },
                        modifier = Modifier.size(40.dp),
                        color = colors.composeAccent,
                    )
                }
            }
        }

        if (attachment.isUploading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { attachment.uploadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.composeAccent,
            )
        }

        attachment.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = attachment.altText,
            onValueChange = { onAltTextChange(attachment.localId, it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !attachment.isUploading,
            textStyle = typography.bodyMedium,
            label = { Text(stringResource(R.string.alt_text_required)) },
            singleLine = false,
            minLines = 3,
            supportingText = {
                Text(
                    text = stringResource(R.string.alt_text_required_supporting),
                    style = typography.labelSmall,
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = { onGenerateAltText(attachment.localId) },
                enabled = !attachment.isUploading &&
                    attachment.uploadedMediumRelayId != null &&
                    !attachment.isGeneratingAltText,
            ) {
                if (attachment.isGeneratingAltText) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(stringResource(R.string.auto_fill_alt_text))
            }

            Spacer(modifier = Modifier.weight(1f))

            if (attachment.altText.isNotBlank()) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.alt_text_saved),
                    style = typography.labelSmall,
                    color = colors.accent,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.composeAccent,
                    contentColor = colors.composeOnAccent,
                ),
            ) {
                Text(stringResource(R.string.save))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun visibilityMenuItem(
    visibility: PostVisibility,
    selectedVisibility: PostVisibility,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    DropdownMenuItem(
        text = {
            Text(
                text = visibilityLabel(visibility),
                color = if (enabled) colors.textPrimary else colors.textSecondary
            )
        },
        onClick = onClick,
        enabled = enabled,
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
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val avatarSize = if (compact) 24.dp else 32.dp
    val contentMaxLines = if (compact) 1 else 3
    val verticalPadding = if (compact) 8.dp else 12.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(avatarSize)
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
                    maxLines = contentMaxLines,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuotedPostPreview(
    post: Post,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val contentMaxLines = if (compact) 1 else 3
    val verticalPadding = if (compact) 8.dp else 12.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = verticalPadding),
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
                    maxLines = contentMaxLines,
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
    compact: Boolean = false,
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
                modifier = Modifier.alpha(0.6f),
                compact = compact,
            )
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
        }
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun ColumnScope.QuotedPostSection(
    isLoading: Boolean,
    quotedPost: Post?,
    compact: Boolean = false,
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
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
            QuotedPostPreview(post = quotedPost, compact = compact)
        }
    }
}
