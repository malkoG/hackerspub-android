package pub.hackers.android.ui.components

import android.util.LruCache
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import pub.hackers.android.ui.theme.UbuntuMonoFontFamily
import java.net.URI

val LocalFontScale = compositionLocalOf { 1f }

private enum class LinkType {
    MENTION, HASHTAG, REGULAR
}

private data class ListContext(val ordered: Boolean, var itemIndex: Int = 0)

enum class HtmlContentStyle {
    Compact,
    Prose
}

internal sealed class ContentBlock {
    data class Text(val html: String) : ContentBlock()
    data class Code(val codeHtml: String) : ContentBlock()
    data class List(val html: String) : ContentBlock()
}

@VisibleForTesting
internal data class ParsedListBlock(
    val ordered: Boolean,
    val items: List<ParsedListItem>,
)

@VisibleForTesting
internal data class ParsedListItem(
    val contentHtml: String,
    val children: List<ParsedListBlock>,
)

private val TAG_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")
private val ATTR_REGEX = Regex("""([\w-]+)=["']([^"']*)["']""")
private val PRE_CODE_REGEX = Regex(
    """<pre[^>]*>\s*<code[^>]*>([\s\S]*?)</code>\s*</pre>""",
    RegexOption.IGNORE_CASE
)
private val EMPTY_PARAGRAPH_REGEX = Regex(
    """<p>\s*(?:<br\s*/?>\s*)*</p>""",
    setOf(RegexOption.IGNORE_CASE)
)
private val LIST_BOUNDARY_BREAK_REGEX = Regex(
    """(</?(?:ul|ol|li)[^>]*>)\s*(?:<br\s*/?>\s*)+""",
    setOf(RegexOption.IGNORE_CASE)
)
private val LIST_BREAK_BEFORE_TAG_REGEX = Regex(
    """(?:<br\s*/?>\s*)+(</?(?:ul|ol|li)[^>]*>)""",
    setOf(RegexOption.IGNORE_CASE)
)
private val BLOCK_TAGS = setOf(
    "p", "div", "blockquote", "ul", "ol", "li", "pre", "h1", "h2", "h3", "h4", "h5", "h6", "hr"
)

// Process-level cache for parsed HTML. Sized to comfortably cover the active
// set across Timeline + Explore + Profile tabs + PostDetail without evicting
// on revisits. Entries are typically 2-5 KB.
private const val HTML_CACHE_MAX_ENTRIES = 256

// Below this html length, parsing runs synchronously during composition; above
// it, parsing is dispatched to Dispatchers.Default via produceState so the
// frame callback can keep rendering smoothly. Tune if placeholder flash is
// visible for intermediate-length posts.
private const val HTML_SYNC_PARSE_THRESHOLD = 500

private data class HtmlCacheKey(
    val html: String,
    val linkColor: ULong,
    val hashtagColor: ULong,
    val mentionBg: ULong,
    val codeBg: ULong,
    val contentStyle: HtmlContentStyle,
)

private val htmlCache = LruCache<HtmlCacheKey, AnnotatedString>(HTML_CACHE_MAX_ENTRIES)

private fun parseAndCacheHtml(
    key: HtmlCacheKey,
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
): AnnotatedString {
    val parsed = parseHtmlToAnnotatedString(
        html = html,
        linkColor = linkColor,
        hashtagColor = hashtagColor,
        mentionBg = mentionBg,
        codeBg = codeBg,
        contentStyle = contentStyle,
    )
    htmlCache.put(key, parsed)
    return parsed
}

/**
 * Returns an [AnnotatedString] for [html], using a process-level LRU cache.
 *
 * - Cache hit or short HTML: parsed synchronously, returned immediately.
 *   No placeholder flash; matches the original behavior.
 * - Long HTML on a cache miss: parsed on [Dispatchers.Default] via
 *   [produceState]. Composition completes immediately with an empty
 *   placeholder; the Text updates on the next frame once parsing finishes.
 *   This prevents the scroll-induced Main-thread stalls previously caused
 *   by [parseHtmlToAnnotatedString] running inline for every newly-composed
 *   LazyColumn item.
 */
@Composable
private fun rememberParsedHtml(
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
): AnnotatedString {
    val cacheKey = remember(html, linkColor, hashtagColor, mentionBg, codeBg, contentStyle) {
        HtmlCacheKey(
            html = html,
            linkColor = linkColor.value,
            hashtagColor = hashtagColor.value,
            mentionBg = mentionBg.value,
            codeBg = codeBg.value,
            contentStyle = contentStyle,
        )
    }

    // Fast path: cache hit or short enough to parse inline.
    val syncValue: AnnotatedString? = remember(cacheKey) {
        htmlCache.get(cacheKey) ?: if (html.length < HTML_SYNC_PARSE_THRESHOLD) {
            parseAndCacheHtml(cacheKey, html, linkColor, hashtagColor, mentionBg, codeBg, contentStyle)
        } else {
            null
        }
    }

    if (syncValue != null) return syncValue

    // Slow path: long HTML, cache miss. Parse off-Main.
    val asyncValue by produceState(initialValue = AnnotatedString(""), cacheKey) {
        value = withContext(Dispatchers.Default) {
            htmlCache.get(cacheKey) ?: parseAndCacheHtml(
                cacheKey,
                html,
                linkColor,
                hashtagColor,
                mentionBg,
                codeBg,
                contentStyle,
            )
        }
    }
    return asyncValue
}

@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    fontScale: Float = 1f,
    contentStyle: HtmlContentStyle = HtmlContentStyle.Compact,
    onMentionClick: ((handle: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null,
    onTextClick: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val colors = LocalAppColors.current
    val linkColor = colors.accent
    val hashtagColor = colors.hashtag
    val mentionBg = colors.accent.copy(alpha = 0.10f)
    val codeBg = colors.surface
    val textColor = colors.textBody

    val effectiveFontScale = if (fontScale != 1f) fontScale else LocalFontScale.current
    val normalizedHtml = remember(html) { normalizeHtmlForRendering(html) }
    val baseStyle = LocalAppTypography.current.bodyLarge.copy(color = textColor)
    val scaledBodyStyle = if (effectiveFontScale != 1f) {
        baseStyle.copy(fontSize = baseStyle.fontSize * effectiveFontScale)
    } else baseStyle
    val bodyStyle = scaledBodyStyle.withContentStyle(contentStyle)
    val listStyle = scaledBodyStyle.withListContentStyle(contentStyle)

    if (maxLines < Int.MAX_VALUE) {
        // Preview mode: flat AnnotatedString (no block code highlighting)
        val annotatedString = rememberParsedHtml(
            normalizedHtml,
            linkColor,
            hashtagColor,
            mentionBg,
            codeBg,
            contentStyle,
        )

        ClickableText(
            text = annotatedString,
            style = bodyStyle,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            onClick = { offset ->
                handleClick(annotatedString, offset, uriHandler, onMentionClick, onLinkClick, onTextClick)
            }
        )
    } else {
        // Full mode: block-based rendering with syntax-highlighted code blocks
        val blocks = remember(normalizedHtml) { splitIntoBlocks(normalizedHtml) }

        Column(modifier = modifier) {
            blocks.forEachIndexed { index, block ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(blockSpacing(blocks[index - 1], block)))
                }
                when (block) {
                    is ContentBlock.Text -> {
                        if (block.html.isNotBlank()) {
                            val annotatedString = rememberParsedHtml(
                                block.html,
                                linkColor,
                                hashtagColor,
                                mentionBg,
                                codeBg,
                                contentStyle,
                            )
                            if (annotatedString.isNotEmpty()) {
                                ClickableText(
                                    text = annotatedString,
                                    style = bodyStyle,
                                    onClick = { offset ->
                                        handleClick(annotatedString, offset, uriHandler, onMentionClick, onLinkClick, onTextClick)
                                    }
                                )
                            }
                        }
                    }
                    is ContentBlock.List -> {
                        val parsedList = remember(block.html) { parseListHtml(block.html) }
                        if (parsedList != null) {
                            RenderListBlock(
                                block = parsedList,
                                level = 0,
                                textStyle = listStyle,
                                linkColor = linkColor,
                                hashtagColor = hashtagColor,
                                mentionBg = mentionBg,
                                codeBg = codeBg,
                                contentStyle = contentStyle,
                                uriHandler = uriHandler,
                                onMentionClick = onMentionClick,
                                onLinkClick = onLinkClick,
                                onTextClick = onTextClick,
                            )
                        }
                    }
                    is ContentBlock.Code -> {
                        CodeBlockView(
                            codeHtml = block.codeHtml
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderListBlock(
    block: ParsedListBlock,
    level: Int,
    textStyle: TextStyle,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    Column {
        block.items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (level == 0) 9.dp else 7.5.dp))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width((level * 8).dp))
                Text(
                    text = if (block.ordered) "${index + 1}." else "\u2022",
                    style = textStyle,
                    modifier = Modifier.width(if (block.ordered) 24.dp else 14.dp)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    val normalizedItemHtml = remember(item.contentHtml) { normalizeListItemHtml(item.contentHtml) }
                    if (normalizedItemHtml.isNotBlank()) {
                        val annotatedString = rememberParsedHtml(
                            normalizedItemHtml,
                            linkColor,
                            hashtagColor,
                            mentionBg,
                            codeBg,
                            contentStyle,
                        )
                        if (annotatedString.isNotEmpty()) {
                            ClickableText(
                                text = annotatedString,
                                style = textStyle,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { offset ->
                                    handleClick(annotatedString, offset, uriHandler, onMentionClick, onLinkClick, onTextClick)
                                }
                            )
                        }
                    }

                    item.children.forEachIndexed { childIndex, child ->
                        Spacer(modifier = Modifier.height(if (childIndex == 0 && normalizedItemHtml.isNotBlank()) 6.5.dp else 5.dp))
                        RenderListBlock(
                            block = child,
                            level = level + 1,
                            textStyle = textStyle,
                            linkColor = linkColor,
                            hashtagColor = hashtagColor,
                            mentionBg = mentionBg,
                            codeBg = codeBg,
                            contentStyle = contentStyle,
                            uriHandler = uriHandler,
                            onMentionClick = onMentionClick,
                            onLinkClick = onLinkClick,
                            onTextClick = onTextClick,
                        )
                    }
                }
            }
        }
    }
}

private fun handleClick(
    annotatedString: AnnotatedString,
    offset: Int,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)? = null
) {
    annotatedString.getStringAnnotations("MENTION", offset, offset)
        .firstOrNull()?.let { annotation ->
            val handle = extractHandleFromUrl(annotation.item)
            if (handle != null && onMentionClick != null) {
                onMentionClick(handle)
            } else {
                try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
            }
            return
        }

    annotatedString.getStringAnnotations("URL", offset, offset)
        .firstOrNull()?.let { annotation ->
            if (onLinkClick != null) {
                onLinkClick(annotation.item)
            } else {
                try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
            }
            return
        }

    // No link or mention tapped — propagate to parent
    onTextClick?.invoke()
}

@VisibleForTesting
internal fun splitIntoBlocks(html: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var lastEnd = 0
    val source = html.trim()

    for (match in PRE_CODE_REGEX.findAll(source)) {
        val before = source.substring(lastEnd, match.range.first)
        if (before.isNotBlank()) {
            blocks.addAll(splitTextAndListBlocks(before))
        }

        val codeHtml = match.groupValues[1]
        blocks.add(ContentBlock.Code(codeHtml))

        lastEnd = match.range.last + 1
    }

    val after = source.substring(lastEnd)
    if (after.isNotBlank()) {
        blocks.addAll(splitTextAndListBlocks(after))
    }

    // If no code blocks found, return the whole thing as text
    if (blocks.isEmpty()) {
        blocks.addAll(splitTextAndListBlocks(source))
    }

    return blocks
}

@VisibleForTesting
internal fun extractHandleFromUrl(url: String): String? {
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

@VisibleForTesting
internal fun parseHtmlToAnnotatedString(
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle = HtmlContentStyle.Compact,
): AnnotatedString {
    return buildAnnotatedString {
        val isProse = contentStyle == HtmlContentStyle.Prose

        // Link state
        var currentLinkType: LinkType? = null
        var hasAnnotation = false

        // Invisible span state
        var insideInvisibleSpan = false
        var invisibleSpanDepth = 0

        // Inline style depths (for push/pop tracking)
        var boldDepth = 0
        var italicDepth = 0
        var codeDepth = 0
        var strikeDepth = 0

        // Block state
        var preDepth = 0
        var headingLevel = 0
        var blockquoteDepth = 0

        // List state
        val listStack = mutableListOf<ListContext>()
        var insideListItem = false
        var listItemParagraphOpen = false
        var listItemJustOpened = false

        // Ruby state
        var insideRt = false
        var insideRp = false

        var hasContent = false
        var pos = 0
        val source = html.trim()

        while (pos < source.length) {
            val tagMatch = TAG_REGEX.find(source, pos)

            // Text before the next tag (or remaining text)
            if (tagMatch == null || tagMatch.range.first > pos) {
                val textEnd = tagMatch?.range?.first ?: source.length
                val rawText = source.substring(pos, textEnd)
                val decoded = decodeHtmlEntities(rawText)

                if (!insideInvisibleSpan && !insideRp && decoded.isNotEmpty()) {
                    if (preDepth > 0) {
                        // Preserve all whitespace in preformatted blocks
                        appendStyledText(this, decoded, currentLinkType, linkColor, hashtagColor, mentionBg)
                        hasContent = true
                    } else {
                        val normalizedText = normalizeInlineWhitespace(
                            text = decoded,
                            builder = this,
                            hasContent = hasContent,
                            nextTagName = tagMatch?.groupValues?.getOrNull(2)?.lowercase()
                        )
                        if (normalizedText.isNotEmpty()) {
                            appendStyledText(this, normalizedText, currentLinkType, linkColor, hashtagColor, mentionBg)
                            if (!normalizedText.isBlank()) {
                                listItemJustOpened = false
                            }
                            hasContent = true
                        }
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
                        // Block elements
                        "p" -> {
                            if (!(insideListItem && listItemJustOpened) && hasContent) {
                                ensureTrailingNewlines(this, if (insideListItem) 1 else 2)
                            }
                        }
                        "br" -> {
                            append("\n")
                        }

                        // Headings
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            headingLevel = tagName[1].digitToInt()
                            if (hasContent) append("\n\n")
                            val fontSize = when (headingLevel) {
                                1 -> 1.5.em
                                2 -> 1.3.em
                                3 -> 1.15.em
                                else -> 1.0.em
                            }
                            pushStyle(SpanStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold
                            ))
                        }

                        // Preformatted / code blocks
                        "pre" -> {
                            if (hasContent) append("\n\n")
                            preDepth++
                            pushStyle(SpanStyle(
                                fontFamily = UbuntuMonoFontFamily,
                                background = codeBg,
                                fontSize = 0.875.em
                            ))
                        }

                        // Inline code
                        "code" -> {
                            codeDepth++
                            if (preDepth == 0) {
                                // Only style inline <code>, not <pre><code>
                                pushStyle(SpanStyle(
                                    fontFamily = UbuntuMonoFontFamily,
                                    background = codeBg,
                                    fontSize = 0.875.em
                                ))
                            }
                        }

                        // Bold
                        "strong", "b" -> {
                            boldDepth++
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        }

                        // Italic
                        "em", "i" -> {
                            italicDepth++
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        }

                        // Strikethrough
                        "del", "s" -> {
                            strikeDepth++
                            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        }

                        // Horizontal rule
                        "hr" -> {
                            if (hasContent) append("\n")
                            append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500")
                            append("\n")
                            hasContent = true
                        }

                        // Blockquote
                        "blockquote" -> {
                            if (hasContent) append("\n\n")
                            blockquoteDepth++
                            pushStyle(SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF6B7280)
                            ))
                        }

                        // Lists
                        "ul" -> {
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            listStack.add(ListContext(ordered = false))
                        }
                        "ol" -> {
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            listStack.add(ListContext(ordered = true))
                        }
                        "li" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            val ctx = listStack.lastOrNull()
                            if (ctx != null) {
                                val nestingLevel = (listStack.size - 1).coerceAtLeast(0)
                                val marker = if (ctx.ordered) {
                                    ctx.itemIndex++
                                    "${ctx.itemIndex}."
                                } else {
                                    "\u2022"
                                }
                                val baseIndent = nestingLevel * 1.2f
                                val markerIndent = if (ctx.ordered) {
                                    0.95f + (marker.length * 0.33f)
                                } else {
                                    1.0f
                                }
                                val firstLineIndent = baseIndent.em
                                val hangingIndent = (baseIndent + markerIndent).em
                                pushStyle(
                                    ParagraphStyle(
                                        textIndent = TextIndent(
                                            firstLine = firstLineIndent,
                                            restLine = hangingIndent,
                                        )
                                    )
                                )
                                listItemParagraphOpen = true
                                if (ctx.ordered) {
                                    append("$marker ")
                                } else {
                                    append("$marker ")
                                }
                            }
                            insideListItem = true
                            listItemJustOpened = true
                            hasContent = true
                        }

                        // Links
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

                        // Ruby annotations
                        "ruby" -> {
                            // No special action needed for opening <ruby>
                        }
                        "rt" -> {
                            insideRt = true
                            pushStyle(SpanStyle(
                                fontSize = 0.6.em,
                                baselineShift = BaselineShift.Superscript
                            ))
                        }
                        "rp" -> {
                            insideRp = true
                        }

                        // Invisible spans
                        "span" -> {
                            val classes = attrs["class"] ?: ""
                            if ("invisible" in classes) {
                                insideInvisibleSpan = true
                                invisibleSpanDepth++
                            }
                        }
                    }
                } else {
                    // Closing tags
                    when (tagName) {
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            if (headingLevel > 0) {
                                pop()
                                headingLevel = 0
                            }
                        }

                        "pre" -> {
                            if (preDepth > 0) {
                                preDepth--
                                pop()
                            }
                        }

                        "code" -> {
                            if (codeDepth > 0) {
                                codeDepth--
                                if (preDepth == 0) {
                                    // Only pop if we pushed for inline code
                                    pop()
                                }
                            }
                        }

                        "strong", "b" -> {
                            if (boldDepth > 0) {
                                boldDepth--
                                pop()
                            }
                        }

                        "em", "i" -> {
                            if (italicDepth > 0) {
                                italicDepth--
                                pop()
                            }
                        }

                        "del", "s" -> {
                            if (strikeDepth > 0) {
                                strikeDepth--
                                pop()
                            }
                        }

                        "blockquote" -> {
                            if (blockquoteDepth > 0) {
                                blockquoteDepth--
                                pop()
                            }
                        }

                        "ul", "ol" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            if (listStack.isNotEmpty()) {
                                // Use removeAt instead of removeLast to avoid Java 21 SequencedCollection dependency
                                listStack.removeAt(listStack.lastIndex)
                            }
                            insideListItem = false
                            if (!isProse && listStack.isEmpty() && hasContent) {
                                append("\n")
                            }
                        }

                        "li" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            insideListItem = false
                            listItemJustOpened = false
                        }

                        "a" -> {
                            if (hasAnnotation) {
                                pop()
                                hasAnnotation = false
                            }
                            currentLinkType = null
                        }

                        "ruby" -> {
                            // No special action needed for closing </ruby>
                        }
                        "rt" -> {
                            if (insideRt) {
                                pop()
                                insideRt = false
                            }
                        }
                        "rp" -> {
                            insideRp = false
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
    hashtagColor: Color,
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
            builder.withStyle(SpanStyle(color = hashtagColor)) {
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

private fun TextStyle.withContentStyle(contentStyle: HtmlContentStyle): TextStyle {
    return when (contentStyle) {
        HtmlContentStyle.Compact -> this
        HtmlContentStyle.Prose -> copy(lineHeight = fontSize * 1.7f)
    }
}

private fun TextStyle.withListContentStyle(contentStyle: HtmlContentStyle): TextStyle {
    return when (contentStyle) {
        HtmlContentStyle.Compact -> this
        HtmlContentStyle.Prose -> copy(lineHeight = fontSize * 1.35f)
    }
}

private fun parseAttributes(attrString: String): Map<String, String> {
    val attrs = mutableMapOf<String, String>()
    for (match in ATTR_REGEX.findAll(attrString)) {
        attrs[match.groupValues[1]] = match.groupValues[2]
    }
    return attrs
}

@VisibleForTesting
internal fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}

@VisibleForTesting
internal fun normalizeHtmlForRendering(html: String): String {
    return html
        .replace(EMPTY_PARAGRAPH_REGEX, "")
        .replace(LIST_BOUNDARY_BREAK_REGEX, "$1")
        .replace(LIST_BREAK_BEFORE_TAG_REGEX, "$1")
}

@VisibleForTesting
internal fun normalizeListHtml(html: String): String {
    return normalizeHtmlForRendering(html)
        .replace(Regex(""">\s+<"""), "><")
}

@VisibleForTesting
internal fun normalizeListItemHtml(html: String): String {
    return normalizeHtmlForRendering(html).trim()
}

private fun normalizeInlineWhitespace(
    text: String,
    builder: AnnotatedString.Builder,
    hasContent: Boolean,
    nextTagName: String?,
): String {
    if (!text.isBlank()) return text
    if (!hasContent) return ""

    val previousChar = builder.toAnnotatedString().text.lastOrNull()
    if (previousChar == null || previousChar == '\n' || previousChar == ' ') return ""
    if (nextTagName != null && nextTagName in BLOCK_TAGS) return ""

    return " "
}

private fun endsWithLineBreak(builder: AnnotatedString.Builder): Boolean {
    return builder.toAnnotatedString().text.lastOrNull() == '\n'
}

private fun ensureTrailingNewlines(builder: AnnotatedString.Builder, count: Int) {
    val text = builder.toAnnotatedString().text
    var trailingNewlines = 0
    var idx = text.length - 1

    while (idx >= 0 && text[idx] == '\n') {
        trailingNewlines++
        idx--
    }

    repeat((count - trailingNewlines).coerceAtLeast(0)) { builder.append("\n") }
}

private fun splitTextAndListBlocks(html: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val source = html.trim()
    if (source.isBlank()) return blocks

    var listDepth = 0
    var listStart = -1
    var lastEnd = 0
    var pos = 0

    while (pos < source.length) {
        val tagMatch = TAG_REGEX.find(source, pos) ?: break
        val isClosing = tagMatch.groupValues[1] == "/"
        val tagName = tagMatch.groupValues[2].lowercase()

        if (tagName == "ul" || tagName == "ol") {
            if (!isClosing) {
                if (listDepth == 0) {
                    val before = source.substring(lastEnd, tagMatch.range.first)
                    if (before.isNotBlank()) {
                        blocks.add(ContentBlock.Text(before))
                    }
                    listStart = tagMatch.range.first
                }
                listDepth++
            } else if (listDepth > 0) {
                listDepth--
                if (listDepth == 0 && listStart >= 0) {
                    val listHtml = source.substring(listStart, tagMatch.range.last + 1)
                    if (listHtml.isNotBlank()) {
                        blocks.add(ContentBlock.List(listHtml))
                    }
                    lastEnd = tagMatch.range.last + 1
                    listStart = -1
                }
            }
        }

        pos = tagMatch.range.last + 1
    }

    val tail = source.substring(lastEnd)
    if (tail.isNotBlank()) {
        blocks.add(ContentBlock.Text(tail))
    }

    return if (blocks.isEmpty()) listOf(ContentBlock.Text(source)) else blocks
}

@VisibleForTesting
internal fun parseListHtml(html: String): ParsedListBlock? {
    val source = normalizeListHtml(html).trim()
    return parseListBlockAt(source, 0)?.first
}

private fun parseListBlockAt(html: String, start: Int): Pair<ParsedListBlock, Int>? {
    val openTag = TAG_REGEX.find(html, start) ?: return null
    if (openTag.range.first != start) return null
    if (openTag.groupValues[1] == "/") return null

    val tagName = openTag.groupValues[2].lowercase()
    if (tagName != "ul" && tagName != "ol") return null

    val ordered = tagName == "ol"
    val items = mutableListOf<ParsedListItem>()
    var pos = openTag.range.last + 1

    while (pos < html.length) {
        val nextTag = TAG_REGEX.find(html, pos) ?: break
        val isClosing = nextTag.groupValues[1] == "/"
        val nextName = nextTag.groupValues[2].lowercase()

        if (isClosing && nextName == tagName) {
            return ParsedListBlock(ordered, items) to (nextTag.range.last + 1)
        }

        if (!isClosing && nextName == "li") {
            val item = parseListItemAt(html, nextTag.range.first) ?: return null
            items.add(item.first)
            pos = item.second
            continue
        }

        pos = nextTag.range.last + 1
    }

    return ParsedListBlock(ordered, items) to pos
}

private fun parseListItemAt(html: String, start: Int): Pair<ParsedListItem, Int>? {
    val openTag = TAG_REGEX.find(html, start) ?: return null
    if (openTag.range.first != start || openTag.groupValues[1] == "/" || openTag.groupValues[2].lowercase() != "li") {
        return null
    }

    val children = mutableListOf<ParsedListBlock>()
    val contentSegments = mutableListOf<String>()
    var pos = openTag.range.last + 1
    var contentStart = pos

    while (pos < html.length) {
        val nextTag = TAG_REGEX.find(html, pos) ?: break
        val isClosing = nextTag.groupValues[1] == "/"
        val nextName = nextTag.groupValues[2].lowercase()

        if (!isClosing && (nextName == "ul" || nextName == "ol")) {
            val beforeChild = html.substring(contentStart, nextTag.range.first)
            if (beforeChild.isNotBlank()) {
                contentSegments.add(beforeChild)
            }
            val child = parseListBlockAt(html, nextTag.range.first) ?: return null
            children.add(child.first)
            pos = child.second
            contentStart = pos
            continue
        }

        if (isClosing && nextName == "li") {
            val beforeClose = html.substring(contentStart, nextTag.range.first)
            if (beforeClose.isNotBlank()) {
                contentSegments.add(beforeClose)
            }
            return ParsedListItem(
                contentHtml = contentSegments.joinToString("").trim(),
                children = children,
            ) to (nextTag.range.last + 1)
        }

        pos = nextTag.range.last + 1
    }

    return null
}

private fun blockSpacing(previous: ContentBlock, current: ContentBlock) = when {
    previous is ContentBlock.Code || current is ContentBlock.Code -> 8.dp
    previous is ContentBlock.List && current is ContentBlock.List -> 4.dp
    previous is ContentBlock.List || current is ContentBlock.List -> 16.dp
    else -> 0.dp
}
