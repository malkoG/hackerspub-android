package pub.hackers.android.ui.screens.editprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditProfileEvent.Saved -> onSaved()
            }
        }
    }

    if (uiState.saveError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveError() },
            title = { Text(stringResource(R.string.action_error)) },
            text = { Text(uiState.saveError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSaveError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = stringResource(R.string.edit_profile_title),
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colors.accent
                        )
                    }
                },
                trailingContent = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving && !uiState.isLoading,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.edit_profile_save),
                                color = colors.accent,
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        EditProfileContent(
            uiState = uiState,
            contentPadding = padding,
            onNameChange = viewModel::onNameChange,
            onBioChange = viewModel::onBioChange,
            onLinkAdd = viewModel::onLinkAdd,
            onLinkChange = viewModel::onLinkChange,
            onLinkRemove = viewModel::onLinkRemove,
            onRetry = viewModel::load,
        )
    }
}

@Composable
private fun EditProfileContent(
    uiState: EditProfileUiState,
    contentPadding: PaddingValues,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onLinkAdd: () -> Unit,
    onLinkChange: (Int, String, String) -> Unit,
    onLinkRemove: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.loadError != null -> ErrorMessage(
                message = uiState.loadError,
                onRetry = onRetry,
            )
            else -> EditProfileForm(
                uiState = uiState,
                onNameChange = onNameChange,
                onBioChange = onBioChange,
                onLinkAdd = onLinkAdd,
                onLinkChange = onLinkChange,
                onLinkRemove = onLinkRemove,
            )
        }
    }
}

@Composable
private fun EditProfileForm(
    uiState: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onLinkAdd: () -> Unit,
    onLinkChange: (Int, String, String) -> Unit,
    onLinkRemove: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarSection(
            avatarUrl = uiState.pendingAvatarDataUrl ?: uiState.avatarUrl,
        )

        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.edit_profile_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChange,
            label = { Text(stringResource(R.string.edit_profile_bio_label)) },
            placeholder = { Text(stringResource(R.string.edit_profile_bio_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )

        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        Text(
            text = stringResource(R.string.edit_profile_links_section),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
        )

        uiState.links.forEachIndexed { index, link ->
            LinkRow(
                name = link.name,
                url = link.url,
                onNameChange = { onLinkChange(index, it, link.url) },
                onUrlChange = { onLinkChange(index, link.name, it) },
                onRemove = { onLinkRemove(index) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLinkAdd() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.accent,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.edit_profile_add_link),
                style = typography.bodyMedium,
                color = colors.accent,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AvatarSection(avatarUrl: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun LinkRow(
    name: String,
    url: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.edit_profile_link_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(stringResource(R.string.edit_profile_link_url)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.edit_profile_remove_link),
                tint = colors.textSecondary,
            )
        }
    }
}
