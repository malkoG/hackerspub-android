package pub.hackers.android.ui.screens.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun ComposeArticleScreen(
    draftId: String? = null,
    onSaveSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ComposeArticleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(draftId) {
        draftId?.let { viewModel.loadDraft(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Draft saved")
            viewModel.clearSavedFlag()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        if (draftId == null) {
            titleFocusRequester.requestFocus()
        }
    }

    val saveEnabled = uiState.title.isNotBlank() && !uiState.isSaving

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
                    text = stringResource(R.string.compose_article),
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester),
                    enabled = !uiState.isSaving,
                    textStyle = typography.headlineMedium.copy(
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.composeAccent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.article_title_hint),
                                    style = typography.headlineMedium,
                                    color = colors.textSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags field
                BasicTextField(
                    value = uiState.tags,
                    onValueChange = { viewModel.updateTags(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    textStyle = typography.bodyMedium.copy(
                        color = colors.textSecondary
                    ),
                    cursorBrush = SolidColor(colors.composeAccent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.tags.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.article_tags_hint),
                                    style = typography.bodyMedium,
                                    color = colors.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(modifier = Modifier.height(16.dp))

                // Content field
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                contentFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = uiState.content,
                            onValueChange = { viewModel.updateContent(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(contentFocusRequester),
                            enabled = !uiState.isSaving,
                            textStyle = typography.bodyLarge.copy(
                                color = colors.textBody
                            ),
                            cursorBrush = SolidColor(colors.composeAccent),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.content.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.article_content_hint),
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
            }

            // Bottom bar with Save Draft button
            HorizontalDivider(color = colors.divider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = { viewModel.saveDraft() },
                    enabled = saveEnabled,
                    shape = RoundedCornerShape(AppShapes.pillRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.composeAccent,
                        contentColor = Color.White,
                        disabledContainerColor = colors.composeAccent,
                        disabledContentColor = Color.White,
                    ),
                    modifier = Modifier.alpha(if (saveEnabled) 1f else 0.4f)
                ) {
                    Text(
                        text = if (uiState.isSaving) {
                            stringResource(R.string.saving_draft)
                        } else {
                            stringResource(R.string.save_draft)
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}
