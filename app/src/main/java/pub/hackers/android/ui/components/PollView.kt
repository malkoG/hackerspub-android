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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.hackers.android.R
import pub.hackers.android.domain.model.Poll
import pub.hackers.android.domain.model.PollOption
import pub.hackers.android.ui.theme.LocalAppColors
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Renders a poll's options with result bars and, when [onVote] is provided and the poll is still
 * open and unvoted, lets the viewer select options and submit a vote. The displayed poll state is
 * replaced with the server's response after a successful vote.
 */
@Composable
fun PollView(
    poll: Poll,
    modifier: Modifier = Modifier,
    onVote: (suspend (optionIndices: List<Int>) -> Result<Poll>)? = null
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var displayPoll by remember(poll) { mutableStateOf(poll) }
    var selected by remember(poll) { mutableStateOf<Set<Int>>(emptySet()) }
    var submitting by remember(poll) { mutableStateOf(false) }
    var errorMessage by remember(poll) { mutableStateOf<String?>(null) }

    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(displayPoll.ends) {
        while (true) {
            now = Instant.now()
            delay(1000)
        }
    }

    val closed = displayPoll.closed || !displayPoll.ends.isAfter(now)
    val votable = onVote != null && !closed && !displayPoll.viewerHasVoted
    val totalVotes = displayPoll.options.sumOf { it.votesCount }

    Column(modifier = modifier.fillMaxWidth()) {
        displayPoll.options.forEach { option ->
            PollOptionRow(
                option = option,
                fraction = if (totalVotes > 0) option.votesCount.toFloat() / totalVotes else 0f,
                votedByViewer = displayPoll.viewerHasVoted && option.viewerHasVoted,
                selectable = votable && !submitting,
                checked = selected.contains(option.index),
                multiple = displayPoll.multiple,
                onToggle = {
                    selected = when {
                        displayPoll.multiple && selected.contains(option.index) ->
                            selected - option.index
                        displayPoll.multiple -> selected + option.index
                        else -> setOf(option.index)
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val statusText = when {
                closed -> stringResource(R.string.poll_closed)
                displayPoll.viewerHasVoted -> stringResource(R.string.poll_voted)
                else -> stringResource(R.string.poll_time_left, pollTimeLeft(displayPoll.ends, now))
            }
            PollMeta(
                text = pluralStringResource(
                    R.plurals.poll_voters,
                    displayPoll.votersCount,
                    displayPoll.votersCount
                )
            )
            PollMeta(text = "·")
            PollMeta(
                text = if (displayPoll.multiple) {
                    stringResource(R.string.poll_multiple_choice)
                } else {
                    stringResource(R.string.poll_single_choice)
                }
            )
            PollMeta(text = "·")
            PollMeta(text = statusText)
        }

        if (votable) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val cb = onVote ?: return@Button
                    val indices = selected.sorted()
                    scope.launch {
                        submitting = true
                        errorMessage = null
                        cb(indices)
                            .onSuccess {
                                displayPoll = it
                                selected = emptySet()
                            }
                            .onFailure { errorMessage = it.message }
                        submitting = false
                    }
                },
                enabled = selected.isNotEmpty() && !submitting
            ) {
                Text(
                    text = if (submitting) {
                        stringResource(R.string.poll_voting)
                    } else {
                        stringResource(R.string.poll_vote)
                    }
                )
            }
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PollMeta(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalAppColors.current.textSecondary
    )
}

@Composable
private fun PollOptionRow(
    option: PollOption,
    fraction: Float,
    votedByViewer: Boolean,
    selectable: Boolean,
    checked: Boolean,
    multiple: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalAppColors.current
    val barColor = colors.accent.copy(alpha = if (votedByViewer || checked) 0.28f else 0.15f)
    val percent = (fraction * 100).roundToInt()

    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(colors.surface)
        .drawBehind {
            drawRect(
                color = barColor,
                size = Size(width = size.width * fraction, height = size.height)
            )
        }
        .let { base ->
            if (selectable) {
                base.selectable(selected = checked, onClick = onToggle)
            } else {
                base
            }
        }

    Box(modifier = rowModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                selectable && multiple -> {
                    Checkbox(checked = checked, onCheckedChange = { onToggle() })
                    Spacer(modifier = Modifier.width(6.dp))
                }
                selectable -> {
                    RadioButton(selected = checked, onClick = onToggle)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                votedByViewer -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = if (votedByViewer) FontWeight.SemiBold else FontWeight.Normal,
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
    return when {
        duration.toDays() >= 1 -> "${duration.toDays()}d"
        duration.toHours() >= 1 -> "${duration.toHours()}h"
        duration.toMinutes() >= 1 -> "${duration.toMinutes()}m"
        else -> "${duration.seconds.coerceAtLeast(0)}s"
    }
}
