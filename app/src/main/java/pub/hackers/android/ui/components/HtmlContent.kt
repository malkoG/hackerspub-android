package pub.hackers.android.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.net.URI

private enum class LinkType {
    MENTION, HASHTAG, REGULAR
}

private val TAG_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")
private val ATTR_REGEX = Regex("""([\w-]+)=["']([^"']*)["']""")

@Composable
fun HtmlContent(
    html: String,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    onMentionClick: ((handle: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val isDark = isSystemInDarkTheme()
    val linkColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val mentionBg = linkColor.copy(alpha = 0.10f)
    val textColor = MaterialTheme.colorScheme.onSurface

    val annotatedString = remember(html, linkColor, mentionBg) {
        parseHtmlToAnnotatedString(html, linkColor, mentionBg)
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        onClick = { offset ->
            // Check mentions first
            annotatedString.getStringAnnotations("MENTION", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val handle = extractHandleFromUrl(annotation.item)
                    if (handle != null && onMentionClick != null) {
                        onMentionClick(handle)
                    } else {
                        try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
                    }
                    return@ClickableText
                }

            // Then regular links and hashtags
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    if (onLinkClick != null) {
                        onLinkClick(annotation.item)
                    } else {
                        try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
                    }
                }
        }
    )
}

/**
 * Extracts a fediverse handle from a mention URL.
 * e.g. "https://hackers.pub/@user" -> "user@hackers.pub"
 *      "https://mastodon.social/@user" -> "user@mastodon.social"
 */
private fun extractHandleFromUrl(url: String): String? {
    return try {
        val uri = URI(url)
        val host = uri.host ?: return null
        val path = uri.path ?: return null
        val username = path.trimStart('/').removePrefix("@")
        if (username.isNotEmpty()) "$username@$host" else null
    } catch (_: Exception) {
        null
    }
}

private fun parseHtmlToAnnotatedString(
    html: String,
    linkColor: Color,
    mentionBg: Color
): AnnotatedString {
    return buildAnnotatedString {
        var currentLinkType: LinkType? = null
        var hasAnnotation = false
        var insideInvisibleSpan = false
        var invisibleSpanDepth = 0
        var hasContent = false
        var isBold = false
        var isItalic = false
        var isStrikethrough = false
        var isCode = false
        var isPreformatted = false
        var listDepth = 0
        var orderedListCounter = 0
        var isInList = false
        var isBlockquote = false

        var pos = 0
        val source = html.trim()

        while (pos < source.length) {
            val tagMatch = TAG_REGEX.find(source, pos)

            // Handle text before the next tag (or remaining text if no more tags)
            if (tagMatch == null || tagMatch.range.first > pos) {
                val textEnd = tagMatch?.range?.first ?: source.length
                val rawText = source.substring(pos, textEnd)
                val decoded = decodeHtmlEntities(rawText)

                if (!insideInvisibleSpan && decoded.isNotEmpty()) {
                    val isInterBlockWhitespace = decoded.isBlank() && decoded.contains('\n')
                    if (!isInterBlockWhitespace) {
                        val styledText = decoded
                        val inlineStyle = SpanStyle(
                            fontWeight = if (isBold) FontWeight.Bold else null,
                            fontStyle = if (isItalic) FontStyle.Italic else null,
                            textDecoration = if (isStrikethrough) TextDecoration.LineThrough else null,
                            fontFamily = if (isCode || isPreformatted) FontFamily.Monospace else null,
                            background = if (isCode && !isPreformatted) Color(0x20808080) else Color.Unspecified
                        )

                        if (isBold || isItalic || isStrikethrough || isCode || isPreformatted) {
                            withStyle(inlineStyle) {
                                appendStyledText(this, styledText, currentLinkType, linkColor, mentionBg)
                            }
                        } else {
                            appendStyledText(this, styledText, currentLinkType, linkColor, mentionBg)
                        }
                        hasContent = true
                    }
                }

                pos = textEnd
            }

            if (tagMatch != null && tagMatch.range.first == pos) {
                val isClosing = tagMatch.groupValues[1] == "/"
                val tagName = tagMatch.groupValues[2].lowercase()
                val attrString = tagMatch.groupValues[3]

                if (!isClosing) {
                    val attrs = parseAttributes(attrString)

                    when (tagName) {
                        "p" -> {
                            if (hasContent) append("\n\n")
                        }
                        "br" -> {
                            append("\n")
                        }
                        "strong", "b" -> {
                            isBold = true
                        }
                        "em", "i" -> {
                            isItalic = true
                        }
                        "del", "s", "strike" -> {
                            isStrikethrough = true
                        }
                        "hr" -> {
                            if (hasContent) append("\n")
                            append("──────────")
                            append("\n")
                            hasContent = true
                        }
                        "code" -> {
                            isCode = true
                        }
                        "pre" -> {
                            if (hasContent) append("\n\n")
                            isPreformatted = true
                        }
                        "blockquote" -> {
                            if (hasContent) append("\n\n")
                            isBlockquote = true
                            append("  \u2502 ")
                        }
                        "ul" -> {
                            if (hasContent && listDepth == 0) append("\n")
                            listDepth++
                            isInList = true
                        }
                        "ol" -> {
                            if (hasContent && listDepth == 0) append("\n")
                            listDepth++
                            orderedListCounter = 0
                            isInList = true
                        }
                        "li" -> {
                            if (hasContent) append("\n")
                            val indent = "  ".repeat(listDepth)
                            if (orderedListCounter > 0 || tagName == "li") {
                                // Check parent — simple heuristic: if orderedListCounter was reset, it's unordered
                            }
                            append("${indent}\u2022 ")
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            if (hasContent) append("\n\n")
                            isBold = true
                        }
                        "a" -> {
                            val classes = attrs["class"] ?: ""
                            val href = attrs["href"] ?: ""

                            currentLinkType = when {
                                "hashtag" in classes -> LinkType.HASHTAG
                                "mention" in classes -> LinkType.MENTION
                                href.isNotEmpty() -> LinkType.REGULAR
                                else -> null
                            }

                            if (href.isNotEmpty()) {
                                val tag = if (currentLinkType == LinkType.MENTION) "MENTION" else "URL"
                                pushStringAnnotation(tag, href)
                                hasAnnotation = true
                            }
                        }
                        "span" -> {
                            val classes = attrs["class"] ?: ""
                            if ("invisible" in classes) {
                                insideInvisibleSpan = true
                                invisibleSpanDepth++
                            }
                        }
                    }
                } else {
                    when (tagName) {
                        "strong", "b" -> {
                            isBold = false
                        }
                        "em", "i" -> {
                            isItalic = false
                        }
                        "del", "s", "strike" -> {
                            isStrikethrough = false
                        }
                        "code" -> {
                            isCode = false
                        }
                        "pre" -> {
                            isPreformatted = false
                            append("\n")
                        }
                        "blockquote" -> {
                            isBlockquote = false
                        }
                        "ul", "ol" -> {
                            listDepth = maxOf(0, listDepth - 1)
                            if (listDepth == 0) {
                                isInList = false
                                orderedListCounter = 0
                            }
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            isBold = false
                            append("\n")
                        }
                        "a" -> {
                            if (hasAnnotation) {
                                pop()
                                hasAnnotation = false
                            }
                            currentLinkType = null
                        }
                        "span" -> {
                            if (insideInvisibleSpan) {
                                invisibleSpanDepth--
                                if (invisibleSpanDepth <= 0) {
                                    insideInvisibleSpan = false
                                    invisibleSpanDepth = 0
                                }
                            }
                        }
                    }
                }

                pos = tagMatch.range.last + 1
            }
        }
    }
}

private fun appendStyledText(
    builder: AnnotatedString.Builder,
    text: String,
    linkType: LinkType?,
    linkColor: Color,
    mentionBg: Color
) {
    when (linkType) {
        LinkType.MENTION -> {
            builder.withStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    background = mentionBg
                )
            ) {
                append(text)
            }
        }
        LinkType.HASHTAG -> {
            builder.withStyle(SpanStyle(color = linkColor)) {
                append(text)
            }
        }
        LinkType.REGULAR -> {
            builder.withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(text)
            }
        }
        null -> {
            builder.append(text)
        }
    }
}

private fun parseAttributes(attrString: String): Map<String, String> {
    val attrs = mutableMapOf<String, String>()
    for (match in ATTR_REGEX.findAll(attrString)) {
        attrs[match.groupValues[1]] = match.groupValues[2]
    }
    return attrs
}

private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}
