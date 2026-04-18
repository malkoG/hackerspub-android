package pub.hackers.android.ui.screens.postdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveHeadingTest {

    private val threeSections = mapOf(
        "intro" to 0f,
        "body" to 600f,
        "summary" to 1400f,
    )

    @Test
    fun `returns null when no headings registered`() {
        assertNull(
            computeActiveHeadingId(
                headingOffsetsInBody = emptyMap(),
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
            )
        )
    }

    @Test
    fun `returns first heading when scrolled exactly to its top`() {
        assertEquals(
            "intro",
            computeActiveHeadingId(threeSections, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        )
    }

    @Test
    fun `returns null when scroll is above the first heading`() {
        val sections = mapOf("intro" to 200f, "body" to 800f)
        assertNull(
            computeActiveHeadingId(sections, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        )
    }

    @Test
    fun `picks the nearest heading above the viewport top`() {
        assertEquals(
            "body",
            computeActiveHeadingId(threeSections, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 700)
        )
    }

    @Test
    fun `remains on a heading while reading its section`() {
        // Viewport top sits inside "body"'s section (well before "summary").
        assertEquals(
            "body",
            computeActiveHeadingId(threeSections, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 1200)
        )
    }

    @Test
    fun `advances when the next heading scrolls past the viewport top`() {
        assertEquals(
            "summary",
            computeActiveHeadingId(threeSections, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 1500)
        )
    }

    @Test
    fun `returns the last heading once the body item has scrolled fully past`() {
        assertEquals(
            "summary",
            computeActiveHeadingId(threeSections, firstVisibleItemIndex = 2, firstVisibleItemScrollOffset = 0)
        )
    }

    @Test
    fun `unordered offset map still resolves the maximum below the scroll line`() {
        val unordered = mapOf(
            "summary" to 1400f,
            "intro" to 0f,
            "body" to 600f,
        )
        assertEquals(
            "body",
            computeActiveHeadingId(unordered, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 1000)
        )
    }
}
