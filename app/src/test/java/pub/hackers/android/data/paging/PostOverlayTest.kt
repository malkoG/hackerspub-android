package pub.hackers.android.data.paging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import java.time.Instant

class PostOverlayTest {

    private val sampleActor = Actor(
        id = "actor-1",
        name = "Alice",
        handle = "alice@hackers.pub",
        avatarUrl = "https://example.com/avatar.png",
    )

    private fun post(
        id: String = "post-1",
        viewerHasShared: Boolean = false,
        shares: Int = 0,
        reactions: Int = 0,
        reactionGroups: List<ReactionGroup> = emptyList(),
        sharedPost: Post? = null,
    ) = Post(
        id = id,
        typename = "Note",
        name = null,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>body</p>",
        excerpt = "body",
        url = null,
        viewerHasShared = viewerHasShared,
        actor = sampleActor,
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = reactions, shares = shares, quotes = 0),
        mentions = emptyList(),
        sharedPost = sharedPost,
        reactionGroups = reactionGroups,
    )

    // region applyOverlay

    @Test
    fun `applyOverlay returns same instance when overlay is null`() {
        val original = post()
        val result = original.applyOverlay(null)
        assertSame(original, result)
    }

    @Test
    fun `applyOverlay overrides viewerHasShared`() {
        val original = post(viewerHasShared = false)
        val overlay = PostOverlay(viewerHasShared = true)

        val result = original.applyOverlay(overlay)

        assertTrue(result.viewerHasShared)
        // original is untouched (immutability)
        assertEquals(false, original.viewerHasShared)
    }

    @Test
    fun `applyOverlay applies positive shareDelta`() {
        val original = post(shares = 5)
        val overlay = PostOverlay(shareDelta = 2)

        val result = original.applyOverlay(overlay)

        assertEquals(7, result.engagementStats.shares)
    }

    @Test
    fun `applyOverlay applies negative shareDelta but clamps at zero`() {
        val original = post(shares = 1)
        val overlay = PostOverlay(shareDelta = -5)

        val result = original.applyOverlay(overlay)

        assertEquals(0, result.engagementStats.shares)
    }

    @Test
    fun `applyOverlay uses reactionCountOverride over original reactions`() {
        val original = post(reactions = 3)
        val overlay = PostOverlay(reactionCountOverride = 99)

        val result = original.applyOverlay(overlay)

        assertEquals(99, result.engagementStats.reactions)
    }

    @Test
    fun `applyOverlay replaces reactionGroups when reactionOverride is set`() {
        val original = post(
            reactionGroups = listOf(
                ReactionGroup(emoji = "❤️", customEmoji = null, count = 2, reactors = emptyList(), viewerHasReacted = false),
            ),
        )
        val newGroups = listOf(
            ReactionGroup(emoji = "🎉", customEmoji = null, count = 1, reactors = emptyList(), viewerHasReacted = true),
        )
        val overlay = PostOverlay(reactionOverride = newGroups)

        val result = original.applyOverlay(overlay)

        assertEquals(newGroups, result.reactionGroups)
    }

    @Test
    fun `applyOverlay keeps unchanged fields when overlay is partial`() {
        val original = post(shares = 10, reactions = 3, viewerHasShared = false)
        // overlay only touches shareDelta
        val overlay = PostOverlay(shareDelta = 1)

        val result = original.applyOverlay(overlay)

        assertEquals(11, result.engagementStats.shares)
        assertEquals(3, result.engagementStats.reactions)
        assertEquals(false, result.viewerHasShared)
    }

    // endregion

    // region applyOverlays (direct + sharedPost propagation)

    @Test
    fun `applyOverlays returns same instance when overlays map is empty`() {
        val original = post()
        val result = original.applyOverlays(emptyMap())
        assertSame(original, result)
    }

    @Test
    fun `applyOverlays applies direct overlay`() {
        val original = post(id = "p1", shares = 5)
        val overlays = mapOf("p1" to PostOverlay(shareDelta = 3))

        val result = original.applyOverlays(overlays)

        assertEquals(8, result.engagementStats.shares)
    }

    @Test
    fun `applyOverlays propagates overlay to sharedPost by its id`() {
        val inner = post(id = "original", viewerHasShared = false)
        val outer = post(id = "repost", sharedPost = inner)
        val overlays = mapOf("original" to PostOverlay(viewerHasShared = true))

        val result = outer.applyOverlays(overlays)

        assertTrue(result.sharedPost!!.viewerHasShared)
        // outer repost wrapper untouched
        assertEquals(false, result.viewerHasShared)
    }

    @Test
    fun `applyOverlays applies both direct and sharedPost overlays simultaneously`() {
        val inner = post(id = "inner", shares = 2)
        val outer = post(id = "outer", shares = 10, sharedPost = inner)
        val overlays = mapOf(
            "outer" to PostOverlay(shareDelta = 1),
            "inner" to PostOverlay(shareDelta = 5),
        )

        val result = outer.applyOverlays(overlays)

        assertEquals(11, result.engagementStats.shares)
        assertEquals(7, result.sharedPost!!.engagementStats.shares)
    }

    @Test
    fun `applyOverlays returns original when overlay map does not contain matching ids`() {
        val inner = post(id = "inner")
        val outer = post(id = "outer", sharedPost = inner)
        val overlays = mapOf("unrelated" to PostOverlay(shareDelta = 10))

        val result = outer.applyOverlays(overlays)

        assertSame(outer, result)
    }

    // endregion
}
