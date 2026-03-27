package pub.hackers.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A segment of a display name: either plain text or an inline image (custom emoji).
 */
private sealed class DisplayNameSegment {
    data class TextSegment(val text: String) : DisplayNameSegment()
    data class ImageSegment(val src: String, val alt: String) : DisplayNameSegment()
}

/**
 * Parses a display name string that may contain <img> tags (custom emojis)
 * into a list of text and image segments.
 */
private fun parseDisplayName(name: String): List<DisplayNameSegment> {
    val segments = mutableListOf<DisplayNameSegment>()
    val imgPattern = Regex("""<img\s+[^>]*src=["']([^"']+)["'][^>]*(?:alt=["']([^"']*)["'])?[^>]*/?\s*>|<img\s+[^>]*alt=["']([^"']*)["'][^>]*(?:src=["']([^"']+)["'])[^>]*/?\s*>""")
    var lastIndex = 0

    for (match in imgPattern.findAll(name)) {
        // Add text before this img tag
        if (match.range.first > lastIndex) {
            val text = name.substring(lastIndex, match.range.first).trim()
            if (text.isNotEmpty()) {
                segments.add(DisplayNameSegment.TextSegment(text))
            }
        }

        val src = match.groupValues[1].ifEmpty { match.groupValues[4] }
        val alt = match.groupValues[2].ifEmpty { match.groupValues[3] }
        if (src.isNotEmpty()) {
            segments.add(DisplayNameSegment.ImageSegment(src, alt))
        }

        lastIndex = match.range.last + 1
    }

    // Add remaining text after last img tag
    if (lastIndex < name.length) {
        val text = name.substring(lastIndex).trim()
        if (text.isNotEmpty()) {
            segments.add(DisplayNameSegment.TextSegment(text))
        }
    }

    return segments
}

/**
 * Renders a display name that may contain inline <img> tags (custom emojis).
 * Images are resized to match the text height. Text is truncated by character
 * limit to handle overflow.
 *
 * @param name The raw display name string, possibly containing <img> tags
 * @param fallback Fallback text if name is null
 * @param style Text style for the name
 * @param color Text color
 * @param maxChars Maximum number of text characters before truncation (0 = no limit)
 * @param emojiHeight Height to render inline emojis, should match text line height
 */
@Composable
fun RichDisplayName(
    name: String?,
    fallback: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    maxChars: Int = 0,
    emojiHeight: Dp = 18.dp
) {
    val displayName = name ?: fallback

    val segments = remember(displayName) { parseDisplayName(displayName) }

    // If no images, render as simple text (fast path)
    val hasImages = segments.any { it is DisplayNameSegment.ImageSegment }
    if (!hasImages) {
        val text = if (maxChars > 0 && displayName.length > maxChars) {
            displayName.take(maxChars) + "\u2026"
        } else {
            displayName
        }
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    // Render mixed text + image segments
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        var charCount = 0
        var truncated = false

        for (segment in segments) {
            if (truncated) break

            when (segment) {
                is DisplayNameSegment.TextSegment -> {
                    val remaining = if (maxChars > 0) maxChars - charCount else Int.MAX_VALUE
                    if (remaining <= 0) {
                        truncated = true
                        break
                    }
                    val text = if (maxChars > 0 && segment.text.length > remaining) {
                        truncated = true
                        segment.text.take(remaining) + "\u2026"
                    } else {
                        segment.text
                    }
                    charCount += segment.text.length
                    Text(
                        text = text,
                        style = style,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                is DisplayNameSegment.ImageSegment -> {
                    AsyncImage(
                        model = segment.src,
                        contentDescription = segment.alt,
                        modifier = Modifier
                            .height(emojiHeight)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.FillHeight
                    )
                    // Count emoji as 1 character for truncation purposes
                    charCount += 1
                }
            }
        }
    }
}
