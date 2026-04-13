package pub.hackers.android.ui.screens.recommendedactors

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.RichDisplayName
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun RecommendedActorsScreen(
    onNavigateBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: RecommendedActorsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = stringResource(R.string.recommended_actors),
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.accent
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.displayedActors.isEmpty() -> {
                FullScreenLoading()
            }
            uiState.error != null && uiState.displayedActors.isEmpty() -> {
                ErrorMessage(
                    message = uiState.error ?: stringResource(R.string.error_generic),
                    onRetry = { /* ViewModel reloads on init */ }
                )
            }
            uiState.displayedActors.isEmpty() -> {
                ErrorMessage(
                    message = stringResource(R.string.recommended_actors_empty)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = uiState.displayedActors,
                        key = { it.id }
                    ) { actor ->
                        RecommendedActorCard(
                            actor = actor,
                            onProfileClick = { onProfileClick(actor.handle) },
                            onDismiss = { viewModel.dismissActor(actor.id) },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider(
                            color = colors.divider,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedActorCard(
    actor: Actor,
    onProfileClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = actor.avatarUrl,
                contentDescription = actor.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                RichDisplayName(
                    name = actor.name,
                    fallback = actor.handle,
                    style = typography.bodyLargeSemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = actor.handle,
                    style = typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = colors.textSecondary
                )
            }
        }

        if (!actor.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            HtmlContent(
                html = actor.bio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp),
                maxLines = 3
            )
        }
    }
}
