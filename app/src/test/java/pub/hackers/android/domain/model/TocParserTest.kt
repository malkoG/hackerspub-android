package pub.hackers.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TocParserTest {

    @Test
    fun `parseTocJson returns empty list for null`() {
        assertTrue(parseTocJson(null).isEmpty())
    }

    @Test
    fun `parseTocJson returns empty list for non-list input`() {
        assertTrue(parseTocJson("not a list").isEmpty())
        assertTrue(parseTocJson(42).isEmpty())
        assertTrue(parseTocJson(mapOf("x" to 1)).isEmpty())
    }

    @Test
    fun `parseTocJson returns empty for empty list`() {
        assertTrue(parseTocJson(emptyList<Any>()).isEmpty())
    }

    @Test
    fun `parseTocJson parses flat entries`() {
        val input = listOf(
            mapOf("id" to "intro", "level" to 1, "title" to "Intro", "children" to emptyList<Any>()),
            mapOf("id" to "body", "level" to 2, "title" to "Body", "children" to emptyList<Any>()),
        )
        val items = parseTocJson(input)
        assertEquals(2, items.size)
        assertEquals(TocItem(id = "intro", level = 1, title = "Intro", children = emptyList()), items[0])
        assertEquals(TocItem(id = "body", level = 2, title = "Body", children = emptyList()), items[1])
    }

    @Test
    fun `parseTocJson parses nested children recursively`() {
        val input = listOf(
            mapOf(
                "id" to "root",
                "level" to 1,
                "title" to "Root",
                "children" to listOf(
                    mapOf(
                        "id" to "child-a",
                        "level" to 2,
                        "title" to "Child A",
                        "children" to listOf(
                            mapOf("id" to "leaf", "level" to 3, "title" to "Leaf", "children" to emptyList<Any>())
                        )
                    ),
                    mapOf("id" to "child-b", "level" to 2, "title" to "Child B", "children" to emptyList<Any>())
                )
            )
        )

        val items = parseTocJson(input)
        assertEquals(1, items.size)
        val root = items[0]
        assertEquals("root", root.id)
        assertEquals(2, root.children.size)
        assertEquals("child-a", root.children[0].id)
        assertEquals(1, root.children[0].children.size)
        assertEquals("leaf", root.children[0].children[0].id)
        assertEquals(3, root.children[0].children[0].level)
        assertTrue(root.children[1].children.isEmpty())
    }

    @Test
    fun `parseTocJson skips entries missing required fields`() {
        val input = listOf(
            mapOf("id" to "ok", "level" to 1, "title" to "OK", "children" to emptyList<Any>()),
            mapOf("id" to "missing-level", "title" to "nope", "children" to emptyList<Any>()),
            mapOf("level" to 2, "title" to "no-id", "children" to emptyList<Any>()),
            mapOf("id" to "no-title", "level" to 2, "children" to emptyList<Any>()),
            "not-a-map",
            null,
        )
        val items = parseTocJson(input)
        assertEquals(1, items.size)
        assertEquals("ok", items[0].id)
    }

    @Test
    fun `parseTocJson accepts numeric level as long or double`() {
        val input = listOf(
            mapOf("id" to "a", "level" to 2L, "title" to "A", "children" to emptyList<Any>()),
            mapOf("id" to "b", "level" to 3.0, "title" to "B", "children" to emptyList<Any>()),
        )
        val items = parseTocJson(input)
        assertEquals(2, items.size)
        assertEquals(2, items[0].level)
        assertEquals(3, items[1].level)
    }

    @Test
    fun `parseTocJson treats missing children as empty list`() {
        val input = listOf(
            mapOf("id" to "a", "level" to 1, "title" to "A")
        )
        val items = parseTocJson(input)
        assertEquals(1, items.size)
        assertTrue(items[0].children.isEmpty())
    }
}
