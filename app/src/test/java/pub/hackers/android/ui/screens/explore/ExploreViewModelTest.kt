package pub.hackers.android.ui.screens.explore

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.testutil.MainDispatcherRule
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)

    private fun newViewModel() = ExploreViewModel(repository)

    private fun samplePost(
        id: String = "post-1",
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
        viewerHasShared = false,
        actor = Actor("a1", null, "alice@hackers.pub", "https://example.com/avatar.png"),
        media = emptyList(),
        engagementStats = EngagementStats(0, 0, 0, 0),
        mentions = emptyList(),
        sharedPost = sharedPost,
        reactionGroups = reactionGroups,
    )

    // region tab selection

    @Test
    fun `initial selectedTab is LOCAL`() = runTest {
        val vm = newViewModel()
        assertEquals(ExploreTab.LOCAL, vm.selectedTab.value)
    }

    @Test
    fun `selectTab changes selectedTab in uiState`() = runTest {
        val vm = newViewModel()
        vm.selectTab(ExploreTab.GLOBAL)
        assertEquals(ExploreTab.GLOBAL, vm.selectedTab.value)
    }

    @Test
    fun `selectTab to the same tab is a no-op`() = runTest {
        val vm = newViewModel()
        vm.selectTab(ExploreTab.LOCAL)
        assertEquals(ExploreTab.LOCAL, vm.selectedTab.value)
    }

    // endregion

    // region reaction picker visibility

    @Test
    fun `showReactionPicker sets reactionPickerPostId`() = runTest {
        val vm = newViewModel()
        vm.showReactionPicker("p1")
        assertEquals("p1", vm.uiState.value.reactionPickerPostId)
    }

    @Test
    fun `hideReactionPicker clears reactionPickerPostId`() = runTest {
        val vm = newViewModel()
        vm.showReactionPicker("p1")
        vm.hideReactionPicker()
        assertNull(vm.uiState.value.reactionPickerPostId)
    }

    @Test
    fun `toggleReaction closes reaction picker`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        vm.showReactionPicker("p1")

        vm.toggleReaction(samplePost(id = "p1"), "🎉")
        advanceUntilIdle()

        assertNull(vm.uiState.value.reactionPickerPostId)
    }

    // endregion

    // region repository call verification

    @Test
    fun `sharePost invokes repository sharePost`() = runTest {
        coEvery { repository.sharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.sharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sharePost("p1") }
    }

    @Test
    fun `unsharePost invokes repository unsharePost`() = runTest {
        coEvery { repository.unsharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.unsharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.unsharePost("p1") }
    }

    @Test
    fun `toggleReaction with shared post targets inner post id`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        val inner = samplePost(id = "inner")
        val outer = samplePost(id = "outer", sharedPost = inner)

        vm.toggleReaction(outer, "🎉")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addReactionToPost("inner", "🎉") }
    }

    @Test
    fun `toggleFavourite uses heart emoji`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.toggleFavourite(samplePost(id = "p1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addReactionToPost("p1", "❤️") }
    }

    // endregion
}
