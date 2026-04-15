package pub.hackers.android.ui.screens.timeline

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.testutil.MainDispatcherRule
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true) {
        coEvery { getArticleDrafts() } returns Result.success(emptyList())
    }
    private val preferencesManager = mockk<PreferencesManager> {
        coEvery { confirmBeforeDelete } returns MutableStateFlow(true)
        coEvery { confirmBeforeShare } returns MutableStateFlow(false)
    }

    private fun newViewModel() = TimelineViewModel(repository, preferencesManager)

    private val sampleActor = Actor(
        id = "actor-1",
        name = "Alice",
        handle = "alice@hackers.pub",
        avatarUrl = "https://example.com/avatar.png",
    )

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
        actor = sampleActor,
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = emptyList(),
        sharedPost = sharedPost,
        reactionGroups = reactionGroups,
    )

    // region reaction picker visibility

    @Test
    fun `showReactionPicker sets reactionPickerPostId in uiState`() = runTest {
        val vm = newViewModel()
        vm.showReactionPicker("post-1")
        assertEquals("post-1", vm.uiState.value.reactionPickerPostId)
    }

    @Test
    fun `hideReactionPicker clears reactionPickerPostId`() = runTest {
        val vm = newViewModel()
        vm.showReactionPicker("post-1")
        vm.hideReactionPicker()
        assertNull(vm.uiState.value.reactionPickerPostId)
    }

    @Test
    fun `toggleReaction closes reaction picker as side effect`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        vm.showReactionPicker("post-1")

        vm.toggleReaction(samplePost(id = "post-1"), "❤️")
        advanceUntilIdle()

        assertNull(vm.uiState.value.reactionPickerPostId)
    }

    // endregion

    // region repository call verification

    @Test
    fun `sharePost invokes repository sharePost with postId`() = runTest {
        coEvery { repository.sharePost("p1") } returns Result.success(Unit)
        val vm = newViewModel()

        vm.sharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sharePost("p1") }
    }

    @Test
    fun `unsharePost invokes repository unsharePost with postId`() = runTest {
        coEvery { repository.unsharePost("p1") } returns Result.success(Unit)
        val vm = newViewModel()

        vm.unsharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.unsharePost("p1") }
    }

    @Test
    fun `toggleReaction with new emoji calls addReactionToPost`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        val post = samplePost(id = "p1", reactionGroups = emptyList())

        vm.toggleReaction(post, "🎉")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addReactionToPost("p1", "🎉") }
    }

    @Test
    fun `toggleReaction when already reacted calls removeReactionFromPost`() = runTest {
        coEvery { repository.removeReactionFromPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        val post = samplePost(
            id = "p1",
            reactionGroups = listOf(
                ReactionGroup(
                    emoji = "❤️", customEmoji = null,
                    count = 1, reactors = emptyList(), viewerHasReacted = true,
                ),
            ),
        )

        vm.toggleReaction(post, "❤️")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.removeReactionFromPost("p1", "❤️") }
    }

    @Test
    fun `toggleReaction on shared post target operates on inner post id`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        val inner = samplePost(id = "inner")
        val outer = samplePost(id = "outer", sharedPost = inner)

        vm.toggleReaction(outer, "🎉")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addReactionToPost("inner", "🎉") }
    }

    // endregion

    // region optimistic update failure revert
    //
    // Failure paths are exercised here to ensure the ViewModel doesn't crash
    // and that the revert branch runs. Overlay internals aren't directly
    // observable through the public API, so we verify by checking the
    // repository was called (i.e., the VM actually attempted the mutation).

    @Test
    fun `sharePost still calls repository even if it ultimately fails`() = runTest {
        coEvery { repository.sharePost(any()) } returns Result.failure(RuntimeException("boom"))
        val vm = newViewModel()

        vm.sharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sharePost("p1") }
    }

    @Test
    fun `toggleReaction still calls repository even on failure`() = runTest {
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.failure(RuntimeException("boom"))
        val vm = newViewModel()

        vm.toggleReaction(samplePost(id = "p1"), "🎉")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addReactionToPost("p1", "🎉") }
    }

    // endregion
}
