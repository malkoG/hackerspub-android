package pub.hackers.android.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pub.hackers.android.ui.components.CompactTopBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R

@Composable
fun SettingsScreen(
    onSignInClick: () -> Unit,
    onSignOutComplete: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    isLoggedIn: Boolean,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

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

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearCache()
                    }
                ) {
                    Text(stringResource(R.string.clear_cache_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactTopBar(title = stringResource(R.string.settings))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "@${uiState.userHandle ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }

            if (isLoggedIn) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sign_out)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { showSignOutDialog = true }
                )
            } else {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sign_in)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Login,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable(onClick = onSignInClick)
                )
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.clear_cache)) },
                supportingContent = {
                    if (uiState.cacheSize.isNotEmpty()) {
                        Text(uiState.cacheSize)
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showClearCacheDialog = true }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = { Text("${stringResource(R.string.version)} ${uiState.appVersion}") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null
                    )
                }
            )

            HorizontalDivider()

            // Typography section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_typography),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_font_size),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.fontSizePercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingContent = {
                    Slider(
                        value = uiState.fontSizePercent.toFloat(),
                        onValueChange = { viewModel.setFontSizePercent(it.toInt()) },
                        valueRange = 75f..200f,
                        steps = 24
                    )
                }
            )

            if (uiState.fontSizePercent != 100) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_font_size_reset),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setFontSizePercent(100) }
                )
            }

            HorizontalDivider()

            // Engagement section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_engagement),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_confirm_before_delete)) },
                trailingContent = {
                    Switch(
                        checked = uiState.confirmBeforeDelete,
                        onCheckedChange = { viewModel.setConfirmBeforeDelete(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_confirm_before_share)) },
                trailingContent = {
                    Switch(
                        checked = uiState.confirmBeforeShare,
                        onCheckedChange = { viewModel.setConfirmBeforeShare(it) }
                    )
                }
            )

            HorizontalDivider()

            // Links section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_links),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_use_in_app_browser)) },
                trailingContent = {
                    Switch(
                        checked = uiState.useInAppBrowser,
                        onCheckedChange = { viewModel.setUseInAppBrowser(it) }
                    )
                }
            )

            HorizontalDivider()

            // Timeline section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_timeline),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TimelineMaxLengthSetting(
                currentValue = uiState.timelineMaxLength,
                onValueChange = { viewModel.setTimelineMaxLength(it) }
            )
        }
    }
}

@Composable
private fun TimelineMaxLengthSetting(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(300, 500, 700, 1000, 0)

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_timeline_max_length)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (currentValue == 0) stringResource(R.string.settings_timeline_unlimited) else currentValue.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { expanded = true }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { value ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (value == 0) stringResource(R.string.settings_timeline_unlimited) else value.toString()
                                )
                            },
                            onClick = {
                                onValueChange(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}
