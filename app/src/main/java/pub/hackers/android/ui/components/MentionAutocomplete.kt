package pub.hackers.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun MentionAutocomplete(
    suggestions: List<Actor>,
    isLoading: Boolean,
    onSuggestionSelected: (Actor) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty() && !isLoading) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .border(1.dp, LocalAppColors.current.divider, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = LocalAppColors.current.background
    ) {
        if (isLoading && suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            LazyColumn {
                items(
                    items = suggestions,
                    key = { it.id }
                ) { actor ->
                    MentionSuggestionItem(
                        actor = actor,
                        onClick = { onSuggestionSelected(actor) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MentionSuggestionItem(
    actor: Actor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = actor.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            RichDisplayName(
                name = actor.name,
                fallback = actor.handle.substringBefore("@"),
                style = LocalAppTypography.current.bodyLargeSemiBold,
                color = LocalAppColors.current.textPrimary
            )
            Text(
                text = actor.handle,
                style = LocalAppTypography.current.labelMedium,
                color = LocalAppColors.current.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
