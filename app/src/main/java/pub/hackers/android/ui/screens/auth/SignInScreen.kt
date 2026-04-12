package pub.hackers.android.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun SignInScreen(
    deepLinkToken: String? = null,
    deepLinkCode: String? = null,
    onSignInSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(deepLinkToken, deepLinkCode) {
        if (deepLinkToken != null && deepLinkCode != null) {
            viewModel.verifyWithDeepLink(deepLinkToken, deepLinkCode)
        }
    }

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onSignInSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.step == SignInStep.USERNAME) {
                UsernameStep(
                    username = uiState.username,
                    onUsernameChange = viewModel::updateUsername,
                    onSubmit = {
                        keyboardController?.hide()
                        viewModel.sendVerificationCode()
                    },
                    onPasskeySignIn = if (pub.hackers.android.FeatureFlags.PASSKEY_AUTH_ENABLED) {
                        val activity = LocalContext.current as Activity
                        {
                            keyboardController?.hide()
                            viewModel.signInWithPasskey(activity)
                        }
                    } else null,
                    isLoading = uiState.isLoading
                )
            } else {
                VerificationStep(
                    code = uiState.code,
                    onCodeChange = viewModel::updateCode,
                    onSubmit = {
                        keyboardController?.hide()
                        viewModel.verifyCode()
                    },
                    onBack = viewModel::goBackToUsername,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
private fun UsernameStep(
    username: String,
    onUsernameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPasskeySignIn: (() -> Unit)? = null,
    isLoading: Boolean
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Text(
        text = stringResource(R.string.sign_in),
        style = typography.titleLarge,
        textAlign = TextAlign.Center,
        color = colors.textPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.sign_in_description),
        style = typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = colors.textSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        placeholder = {
            Text(
                text = stringResource(R.string.username),
                style = typography.bodyLarge,
                color = colors.textSecondary
            )
        },
        textStyle = typography.bodyLarge.copy(color = colors.textBody),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surface,
            unfocusedBorderColor = colors.divider,
            focusedBorderColor = colors.accent,
            cursorColor = colors.accent,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSubmit,
        enabled = username.isNotBlank() && !isLoading,
        shape = RoundedCornerShape(AppShapes.pillRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White,
            disabledContainerColor = colors.accent.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = stringResource(R.string.send_code),
                style = typography.bodyLarge,
            )
        }
    }

    if (onPasskeySignIn != null) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
            Text(
                text = stringResource(R.string.or),
                style = typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onPasskeySignIn,
            enabled = !isLoading,
            shape = RoundedCornerShape(AppShapes.pillRadius),
            border = BorderStroke(1.dp, colors.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sign_in_with_passkey),
                style = typography.bodyLarge,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun VerificationStep(
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Text(
        text = stringResource(R.string.sign_in),
        style = typography.titleLarge,
        textAlign = TextAlign.Center,
        color = colors.textPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.verification_description),
        style = typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = colors.textSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        placeholder = {
            Text(
                text = stringResource(R.string.verification_code),
                style = typography.bodyLarge,
                color = colors.textSecondary
            )
        },
        textStyle = typography.bodyLarge.copy(color = colors.textBody),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surface,
            unfocusedBorderColor = colors.divider,
            focusedBorderColor = colors.accent,
            cursorColor = colors.accent,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSubmit,
        enabled = code.isNotBlank() && !isLoading,
        shape = RoundedCornerShape(AppShapes.pillRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White,
            disabledContainerColor = colors.accent.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = stringResource(R.string.verify),
                style = typography.bodyLarge,
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = onBack, enabled = !isLoading) {
        Text(
            text = stringResource(R.string.cancel),
            style = typography.bodyMedium,
            color = colors.accent
        )
    }
}
