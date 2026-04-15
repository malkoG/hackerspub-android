package pub.hackers.android.ui.screens.postdetail

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostDetailResult
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.testutil.MainDispatcherRule
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager> {
        coEvery { userHandle } returns flowOf(null)
    }
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    private val defaultPostId = "post-1"

    private val sampleActor = Actor("actor-1", "Alice", "alice@hackers.pub", "https://ex/a.png")

    private fun samplePost(
        id: String = defaultPostId,
        shares: Int = 0,
        reactions: Int = 0,
        viewerHasShared: Boolean = false,
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
        engagementStats = EngagementStats(0, reactions, shares, 0),
        mentions = emptyList(),
    )

    private fun stubLoadPostSuccess(
        post: Post = samplePost(),
        reactionGroups: List<ReactionGroup> = emptyList(),
    ) {
        coEvery { repository.getPostDetail(any(), any()) } returns Result.success(
            PostDetailResult(
                post = post,
                reactionGroups = reactionGroups,
                replies = emptyList(),
                hasMoreReplies = false,
                repliesEndCursor = null,
            )
        )
    }

    private fun newViewModel(): PostDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("postId" to defaultPostId))
        return PostDetailViewModel(repository, sessionManager, preferencesManager, savedStateHandle)
    }

    // region initial load

    @Test
    fun `init triggers loadPost and populates uiState`() = runTest {
        stubLoadPostSuccess(samplePost(shares = 3))
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(defaultPostId, vm.uiState.value.post?.id)
        assertEquals(3, vm.uiState.value.post?.engagementStats?.shares)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `init failure stores error in uiState`() = runTest {
        coEvery { repository.getPostDetail(any(), any()) } returns Result.failure(RuntimeException("server down"))
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals("server down", vm.uiState.value.error)
        assertNull(vm.uiState.value.post)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `canDelete is true when session handle matches post author and not shared`() = runTest {
        coEvery { sessionManager.userHandle } returns flowOf("alice@hackers.pub")
        stubLoadPostSuccess(samplePost())
        val vm = newViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.canDelete)
    }

    @Test
    fun `canDelete is false when author differs from session handle`() = runTest {
        coEvery { sessionManager.userHandle } returns flowOf("bob@hackers.pub")
        stubLoadPostSuccess(samplePost())
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.canDelete)
    }

    // endregion

    // region optimistic share

    @Test
    fun `sharePost optimistically flips viewerHasShared and bumps shares`() = runTest {
        stubLoadPostSuccess(samplePost(shares = 5, viewerHasShared = false))
        coEvery { repository.sharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sharePost()
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.post?.viewerHasShared)
        assertEquals(6, vm.uiState.value.post?.engagementStats?.shares)
    }

    @Test
    fun `unsharePost optimistically flips viewerHasShared and decrements shares`() = runTest {
        stubLoadPostSuccess(samplePost(shares = 5, viewerHasShared = true))
        coEvery { repository.unsharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.unsharePost()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.post?.viewerHasShared)
        assertEquals(4, vm.uiState.value.post?.engagementStats?.shares)
    }

    @Test
    fun `unsharePost clamps shares at zero`() = runTest {
        stubLoadPostSuccess(samplePost(shares = 0, viewerHasShared = true))
        coEvery { repository.unsharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.unsharePost()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.post?.engagementStats?.shares)
    }

    // endregion

    // region delete

    @Test
    fun `deletePost calls repository and flips isDeleted on success`() = runTest {
        stubLoadPostSuccess()
        coEvery { repository.deletePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deletePost()
        advanceUntilIdle()

        coVerify { repository.deletePost(defaultPostId) }
        assertTrue(vm.uiState.value.isDeleted)
        assertEquals(false, vm.uiState.value.isDeleting)
    }

    @Test
    fun `deletePost stores deleteError on failure`() = runTest {
        stubLoadPostSuccess()
        coEvery { repository.deletePost(any()) } returns Result.failure(RuntimeException("denied"))
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deletePost()
        advanceUntilIdle()

        assertEquals("denied", vm.uiState.value.deleteError)
        assertEquals(false, vm.uiState.value.isDeleted)
    }

    @Test
    fun `dismissDeleteError clears deleteError`() = runTest {
        stubLoadPostSuccess()
        coEvery { repository.deletePost(any()) } returns Result.failure(RuntimeException("denied"))
        val vm = newViewModel()
        advanceUntilIdle()
        vm.deletePost()
        advanceUntilIdle()

        vm.dismissDeleteError()

        assertNull(vm.uiState.value.deleteError)
    }

    // endregion

    // region reaction

    @Test
    fun `toggleReaction adding new emoji creates reaction group`() = runTest {
        stubLoadPostSuccess(reactionGroups = emptyList())
        coEvery { repository.addReactionToPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.toggleReaction("🎉")
        advanceUntilIdle()

        val groups = vm.uiState.value.reactionGroups
        assertEquals(1, groups.size)
        assertEquals("🎉", groups[0].emoji)
        assertEquals(1, groups[0].count)
        assertTrue(groups[0].viewerHasReacted)
    }

    @Test
    fun `toggleReaction on existing reacted emoji removes it`() = runTest {
        stubLoadPostSuccess(
            reactionGroups = listOf(
                ReactionGroup("❤️", null, 1, emptyList(), viewerHasReacted = true),
            ),
        )
        coEvery { repository.removeReactionFromPost(any(), any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.toggleReaction("❤️")
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.reactionGroups.size)
        assertEquals(false, vm.uiState.value.isReacting)
    }

    @Test
    fun `toggleReactionPicker toggles showReactionPicker`() = runTest {
        stubLoadPostSuccess()
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.showReactionPicker)

        vm.toggleReactionPicker()
        assertEquals(true, vm.uiState.value.showReactionPicker)

        vm.toggleReactionPicker()
        assertEquals(false, vm.uiState.value.showReactionPicker)
    }

    // endregion
}
