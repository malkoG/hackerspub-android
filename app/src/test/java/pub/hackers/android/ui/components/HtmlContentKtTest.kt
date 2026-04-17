package pub.hackers.android.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlContentKtTest {

    // region decodeHtmlEntities

    @Test
    fun `decodeHtmlEntities decodes amp`() {
        assertEquals("A & B", decodeHtmlEntities("A &amp; B"))
    }

    @Test
    fun `decodeHtmlEntities decodes lt and gt`() {
        assertEquals("<div>", decodeHtmlEntities("&lt;div&gt;"))
    }

    @Test
    fun `decodeHtmlEntities decodes quot`() {
        assertEquals("say \"hello\"", decodeHtmlEntities("say &quot;hello&quot;"))
    }

    @Test
    fun `decodeHtmlEntities decodes apos and numeric apos`() {
        assertEquals("it's it's", decodeHtmlEntities("it&apos;s it&#39;s"))
    }

    @Test
    fun `decodeHtmlEntities decodes nbsp`() {
        assertEquals("a b", decodeHtmlEntities("a&nbsp;b"))
    }

    @Test
    fun `decodeHtmlEntities returns plain text unchanged`() {
        assertEquals("hello world", decodeHtmlEntities("hello world"))
    }

    @Test
    fun `normalizeHtmlForRendering removes empty list paragraphs and breaks`() {
        val html = "<ol><li><p>item 1</p><p><br></p><ul><br><li><p>item 1-1</p></li><p><br></p><li><p>item 1-2</p></li></ul></li></ol>"
        val normalized = normalizeHtmlForRendering(html)
        assertEquals(
            "<ol><li><p>item 1</p><ul><li><p>item 1-1</p></li><li><p>item 1-2</p></li></ul></li></ol>",
            normalized
        )
    }

    @Test
    fun `normalizeListHtml removes formatting whitespace between list tags`() {
        val html = """
            <ol>
              <li><p>item 1</p>
                <ul>
                  <li><p>item 1-1</p></li>
                  
                  <li><p>item 1-2</p></li>
                </ul>
              </li>
            </ol>
        """.trimIndent()
        assertEquals(
            "<ol><li><p>item 1</p><ul><li><p>item 1-1</p></li><li><p>item 1-2</p></li></ul></li></ol>",
            normalizeListHtml(html)
        )
    }

    @Test
    fun `parseListHtml parses nested ordered and unordered lists`() {
        val html = "<ol><li><p>item 1</p><ul><li><p>item 1-1</p></li><li><p>item 1-2</p></li></ul></li><li><p>item 2</p></li></ol>"
        val parsed = parseListHtml(html)
        assertEquals(true, parsed?.ordered)
        assertEquals(2, parsed?.items?.size)
        assertEquals("<p>item 1</p>", parsed?.items?.get(0)?.contentHtml)
        assertEquals(1, parsed?.items?.get(0)?.children?.size)
        assertEquals(false, parsed?.items?.get(0)?.children?.get(0)?.ordered)
        assertEquals(2, parsed?.items?.get(0)?.children?.get(0)?.items?.size)
    }

    // endregion

    // region extractHandleFromUrl

    @Test
    fun `extractHandleFromUrl extracts handle from profile url`() {
        assertEquals("alice@example.com", extractHandleFromUrl("https://example.com/alice"))
    }

    @Test
    fun `extractHandleFromUrl extracts handle with at prefix`() {
        assertEquals("alice@example.com", extractHandleFromUrl("https://example.com/@alice"))
    }

    @Test
    fun `extractHandleFromUrl returns null for empty path`() {
        assertNull(extractHandleFromUrl("https://example.com/"))
    }

    @Test
    fun `extractHandleFromUrl returns null for invalid url`() {
        assertNull(extractHandleFromUrl("not a url"))
    }

    // endregion

    // region splitIntoBlocks

    @Test
    fun `splitIntoBlocks returns single text block for plain html`() {
        val blocks = splitIntoBlocks("<p>Hello</p>")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Text)
    }

    @Test
    fun `splitIntoBlocks extracts code block`() {
        val html = "<p>before</p><pre><code>val x = 1</code></pre><p>after</p>"
        val blocks = splitIntoBlocks(html)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Text)
        assertTrue(blocks[1] is ContentBlock.Code)
        assertTrue(blocks[2] is ContentBlock.Text)
        assertEquals("val x = 1", (blocks[1] as ContentBlock.Code).codeHtml)
    }

    @Test
    fun `splitIntoBlocks handles multiple code blocks`() {
        val html = "<pre><code>a</code></pre><p>mid</p><pre><code>b</code></pre>"
        val blocks = splitIntoBlocks(html)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Code)
        assertTrue(blocks[1] is ContentBlock.Text)
        assertTrue(blocks[2] is ContentBlock.Code)
    }

    @Test
    fun `splitIntoBlocks handles code-only content`() {
        val html = "<pre><code>only code</code></pre>"
        val blocks = splitIntoBlocks(html)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Code)
    }

    @Test
    fun `splitIntoBlocks extracts top level list into separate block`() {
        val html = "<p>before</p><ul><li>item</li></ul><p>after</p>"
        val blocks = splitIntoBlocks(html)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Text)
        assertTrue(blocks[1] is ContentBlock.List)
        assertTrue(blocks[2] is ContentBlock.Text)
    }

    @Test
    fun `splitIntoBlocks keeps nested list inside one list block`() {
        val html = "<ul><li>parent<ul><li>child</li></ul></li></ul>"
        val blocks = splitIntoBlocks(html)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.List)
    }

    // endregion

    // region parseHtmlToAnnotatedString

    private val linkColor = Color(0xFF1DA1F2)
    private val hashtagColor = Color(0xFF0891B2)
    private val mentionBg = Color(0x1A1DA1F2)
    private val codeBg = Color(0xFFF5F5F5)

    @Test
    fun `parseHtmlToAnnotatedString extracts plain text`() {
        val result = parseHtmlToAnnotatedString("<p>Hello world</p>", linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("Hello world", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles br tag`() {
        val result = parseHtmlToAnnotatedString("line1<br>line2", linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("line1\nline2", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles multiple paragraphs`() {
        val result = parseHtmlToAnnotatedString("<p>first</p><p>second</p>", linkColor, hashtagColor, mentionBg, codeBg)
        assertTrue(result.text.contains("first"))
        assertTrue(result.text.contains("second"))
        assertTrue(result.text.contains("\n\n"))
    }

    @Test
    fun `parseHtmlToAnnotatedString handles link with URL annotation`() {
        val html = """<a href="https://example.com">click</a>"""
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("click", result.text)
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, annotations.size)
        assertEquals("https://example.com", annotations[0].item)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles mention link`() {
        val html = """<a href="https://example.com/@alice" class="mention">@alice</a>"""
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("@alice", result.text)
        val annotations = result.getStringAnnotations("MENTION", 0, result.length)
        assertEquals(1, annotations.size)
        assertEquals("https://example.com/@alice", annotations[0].item)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles hashtag link`() {
        val html = """<a href="https://example.com/tags/kotlin" class="hashtag">#kotlin</a>"""
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("#kotlin", result.text)
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, annotations.size)
    }

    @Test
    fun `parseHtmlToAnnotatedString skips invisible spans`() {
        val html = """<span class="invisible">hidden</span>visible"""
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("visible", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles unordered list`() {
        val html = "<ul><li>item1</li><li>item2</li></ul>"
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertTrue(result.text.contains("\u2022 item1"))
        assertTrue(result.text.contains("\u2022 item2"))
    }

    @Test
    fun `parseHtmlToAnnotatedString handles ordered list`() {
        val html = "<ol><li>first</li><li>second</li></ol>"
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertTrue(result.text.contains("1. first"))
        assertTrue(result.text.contains("2. second"))
    }

    @Test
    fun `parseHtmlToAnnotatedString preserves whitespace between inline tokens`() {
        val html = "<strong>Hello</strong>\n<em>world</em>"
        val result = parseHtmlToAnnotatedString(html, linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("Hello world", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString adds prose spacing between list items`() {
        val html = "<ul><li>item1</li><li>item2</li></ul>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertTrue(result.text.contains("\u2022 item1\n\u2022 item2"))
    }

    @Test
    fun `parseHtmlToAnnotatedString uses paragraph indent for prose list items`() {
        val html = "<ul><li>item1</li></ul>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertEquals(1, result.paragraphStyles.size)
        assertTrue(result.paragraphStyles.first().item.textIndent?.restLine?.value ?: 0f > 0f)
    }

    @Test
    fun `parseHtmlToAnnotatedString gives ordered lists a hanging indent`() {
        val html = "<ol><li>first item</li></ol>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        val indent = result.paragraphStyles.first().item.textIndent
        assertTrue((indent?.restLine?.value ?: 0f) > (indent?.firstLine?.value ?: 0f))
    }

    @Test
    fun `parseHtmlToAnnotatedString keeps prose list block spacing compact`() {
        val html = "<p>before</p><ul><li>item1</li><li>item2</li></ul><p>after</p>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertTrue(result.text.contains("before\n\u2022 item1\n\u2022 item2\n\nafter"))
    }

    @Test
    fun `parseHtmlToAnnotatedString puts nested list on its own line`() {
        val html = "<ul><li>parent<ul><li>child</li></ul></li><li>next</li></ul>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertTrue(result.text.contains("\u2022 parent\n\u2022 child\n\u2022 next"))
        assertEquals(3, result.paragraphStyles.size)
    }

    @Test
    fun `parseHtmlToAnnotatedString keeps list item paragraph on marker line`() {
        val html = "<ol><li><p>blah</p><ul><li>item 1</li></ul></li><li><p>foobar</p><ul><li>item 2</li></ul></li></ol>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertTrue(result.text.contains("1. blah\n\u2022 item 1\n2. foobar\n\u2022 item 2"))
    }

    @Test
    fun `parseHtmlToAnnotatedString collapses nested list blank lines`() {
        val html = "<ol><li><p>item 1</p><ul><li><p>item 1-1</p></li><li><p>item 1-2</p></li></ul></li></ol>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertEquals("1. item 1\n\u2022 item 1-1\n\u2022 item 1-2", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString uses compact paragraph spacing inside list item`() {
        val html = "<ul><li><p>first paragraph</p><p>second paragraph</p></li></ul>"
        val result = parseHtmlToAnnotatedString(
            html = html,
            linkColor = linkColor,
            hashtagColor = hashtagColor,
            mentionBg = mentionBg,
            codeBg = codeBg,
            contentStyle = HtmlContentStyle.Prose,
        )
        assertTrue(result.text.contains("\u2022 first paragraph\nsecond paragraph"))
    }

    @Test
    fun `parseHtmlToAnnotatedString handles hr tag`() {
        val result = parseHtmlToAnnotatedString("<hr>", linkColor, hashtagColor, mentionBg, codeBg)
        assertTrue(result.text.contains("\u2500"))
    }

    @Test
    fun `parseHtmlToAnnotatedString decodes entities in text`() {
        val result = parseHtmlToAnnotatedString("<p>A &amp; B</p>", linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("A & B", result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles empty html`() {
        val result = parseHtmlToAnnotatedString("", linkColor, hashtagColor, mentionBg, codeBg)
        assertEquals("", result.text)
    }

    // endregion
}
