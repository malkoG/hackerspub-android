package pub.hackers.android.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareTargetTextTest {
    @Test
    fun `formats subject and text separated by blank line`() {
        val result = ShareTargetText.format(
            subject = "Article title",
            text = "https://example.com/article",
        )

        assertEquals("Article title\n\nhttps://example.com/article", result)
    }

    @Test
    fun `uses text as-is when subject is missing`() {
        val result = ShareTargetText.format(
            subject = null,
            text = "  https://example.com/article\n",
        )

        assertEquals("  https://example.com/article\n", result)
    }

    @Test
    fun `ignores subject without shared text`() {
        val result = ShareTargetText.format(
            subject = "Article title",
            text = "   ",
        )

        assertNull(result)
    }
}
