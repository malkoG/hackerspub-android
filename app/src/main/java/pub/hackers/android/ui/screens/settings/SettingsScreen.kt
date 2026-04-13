package pub.hackers.android.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun SettingsScreen(
    onSignInClick: () -> Unit,
    onSignOutComplete: () -> Unit,
    onProfileClick: (String) -> Unit,
    isLoggedIn: Boolean,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddPasskeyDialog by remember { mutableStateOf(false) }
    var showRevokePasskeyId by remember { mutableStateOf<String?>(null) }
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val passkeyEnabled = pub.hackers.android.FeatureFlags.PASSKEY_AUTH_ENABLED

    LaunchedEffect(isLoggedIn, passkeyEnabled) {
        if (isLoggedIn && passkeyEnabled) viewModel.loadPasskeys()
    }

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignOutComplete()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    }
                ) {
                    Text(
                        stringResource(R.string.sign_out),
                        color = colors.accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        stringResource(R.string.cancel),
                        color = colors.accent
                    )
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(title = stringResource(R.string.settings))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.userHandle?.let { onProfileClick(it) }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = uiState.userAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.userName ?: "",
                            style = typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = uiState.userHandle ?: "",
                            style = typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }

            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = colors.reaction
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.sign_out),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                if (passkeyEnabled) {
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)

                    // Passkeys section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.passkeys),
                                style = typography.bodyLarge,
                                color = colors.textPrimary
                            )
                        }
                        IconButton(onClick = { showAddPasskeyDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_passkey),
                                tint = colors.accent
                            )
                        }
                    }

                    if (uiState.isLoadingPasskeys) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (uiState.passkeys.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_passkeys),
                            style = typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 56.dp, vertical = 8.dp)
                        )
                    } else {
                        uiState.passkeys.forEach { passkey ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = passkey.name,
                                        style = typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = passkey.created,
                                        style = typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(onClick = { showRevokePasskeyId = passkey.id }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = colors.reaction
                                    )
                                }
                            }
                        }
                    }
                } // passkeyEnabled
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignInClick)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearCache() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.clear_cache),
                    style = typography.bodyLarge,
                    color = colors.textPrimary
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.about),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.version)} ${uiState.appVersion}",
                        style = typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    // Add passkey dialog
    if (passkeyEnabled && showAddPasskeyDialog) {
        var passkeyName by remember { mutableStateOf("") }
        val activity = LocalContext.current as Activity

        AlertDialog(
            onDismissRequest = { showAddPasskeyDialog = false },
            title = { Text(stringResource(R.string.add_passkey)) },
            text = {
                OutlinedTextField(
                    value = passkeyName,
                    onValueChange = { passkeyName = it },
                    placeholder = { Text(stringResource(R.string.passkey_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        cursorColor = colors.accent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddPasskeyDialog = false
                        viewModel.registerPasskey(passkeyName.ifBlank { "Android" }, activity)
                    },
                    enabled = !uiState.isRegisteringPasskey
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPasskeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Revoke passkey confirmation dialog
    val passkeyIdToRevoke = showRevokePasskeyId
    if (passkeyEnabled && passkeyIdToRevoke != null) {
        RevokePasskeyDialog(
            passkeyId = passkeyIdToRevoke,
            onConfirm = { id ->
                viewModel.revokePasskey(id)
                showRevokePasskeyId = null
            },
            onDismiss = { showRevokePasskeyId = null }
        )
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun RevokePasskeyDialog(
    passkeyId: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_passkey)) },
        text = { Text(stringResource(R.string.remove_passkey_confirm)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(passkeyId) }) {
                Text(stringResource(R.string.remove), color = colors.reaction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
