package pub.hackers.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import pub.hackers.android.R
import pub.hackers.android.domain.model.Poll
import pub.hackers.android.domain.model.PollOption
import pub.hackers.android.ui.theme.LocalAppColors
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun PollView(
    poll: Poll,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(poll.ends) {
        while (true) {
            now = Instant.now()
            delay(1000)
        }
    }

    val closed = poll.closed || !poll.ends.isAfter(now)
    val totalVotes = poll.options.sumOf { it.votesCount }

    Column(modifier = modifier.fillMaxWidth()) {
        poll.options.forEach { option ->
            PollOptionRow(
                option = option,
                fraction = if (totalVotes > 0) option.votesCount.toFloat() / totalVotes else 0f,
                selected = poll.viewerHasVoted && option.viewerHasVoted
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val statusText = if (closed) {
                stringResource(R.string.poll_closed)
            } else {
                stringResource(R.string.poll_time_left, pollTimeLeft(poll.ends, now))
            }
            Text(
                text = pluralStringResource(
                    R.plurals.poll_voters,
                    poll.votersCount,
                    poll.votersCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = if (poll.multiple) {
                    stringResource(R.string.poll_multiple_choice)
                } else {
                    stringResource(R.string.poll_single_choice)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PollOption,
    fraction: Float,
    selected: Boolean
) {
    val colors = LocalAppColors.current
    val barColor = colors.accent.copy(alpha = if (selected) 0.28f else 0.15f)
    val percent = (fraction * 100).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .drawBehind {
                drawRect(
                    color = barColor,
                    size = Size(width = size.width * fraction, height = size.height)
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }
}

private fun pollTimeLeft(ends: Instant, now: Instant): String {
    val duration = Duration.between(now, ends)
    val remaining = when {
        duration.toDays() >= 1 -> "${duration.toDays()}d"
        duration.toHours() >= 1 -> "${duration.toHours()}h"
        duration.toMinutes() >= 1 -> "${duration.toMinutes()}m"
        else -> "${duration.seconds.coerceAtLeast(0)}s"
    }
    return remaining
}
