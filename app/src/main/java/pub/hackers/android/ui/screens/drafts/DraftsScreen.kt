package pub.hackers.android.ui.screens.drafts

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import pub.hackers.android.domain.model.ArticleDraft
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DraftsScreen(
    onNavigateBack: () -> Unit,
    onDraftClick: (String) -> Unit,
    viewModel: DraftsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val snackbarHostState = remember { SnackbarHostState() }
    var draftToDelete by remember { mutableStateOf<ArticleDraft?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog
    draftToDelete?.let { draft ->
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text(stringResource(R.string.delete_draft), color = colors.textPrimary) },
            text = { Text(stringResource(R.string.delete_draft_confirm), color = colors.textBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDraft(draft.id)
                        draftToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = stringResource(R.string.my_drafts),
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.drafts.isEmpty() -> {
                    FullScreenLoading()
                }
                uiState.error != null && uiState.drafts.isEmpty() -> {
                    ErrorMessage(
                        message = uiState.error ?: stringResource(R.string.error_generic),
                        onRetry = { viewModel.loadDrafts() }
                    )
                }
                uiState.drafts.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_drafts),
                        style = typography.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.drafts, key = { it.id }) { draft ->
                            DraftItem(
                                draft = draft,
                                isDeleting = uiState.deletingDraftId == draft.id,
                                onClick = { onDraftClick(draft.id) },
                                onDeleteClick = { draftToDelete = draft }
                            )
                            HorizontalDivider(color = colors.divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftItem(
    draft: ArticleDraft,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDeleting, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = draft.title.ifBlank { "(Untitled)" },
                style = typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormatter.format(draft.updated),
                style = typography.bodySmall,
                color = colors.textSecondary
            )
            if (draft.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = draft.tags.joinToString(", "),
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onDeleteClick,
            enabled = !isDeleting,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_draft),
                tint = if (isDeleting) colors.textSecondary.copy(alpha = 0.3f) else colors.textSecondary
            )
        }
    }
}
