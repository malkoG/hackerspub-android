package pub.hackers.android.ui.screens.compose

import android.content.Context
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Before
import org.robolectric.annotation.Config
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostDetailResult
import pub.hackers.android.testutil.MainDispatcherRule
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun stubViewer() {
        // ComposeViewModel.init calls repository.getViewer(); the relaxed
        // mock's default Result wraps a generic Object that crashes when the
        // collector destructures it as Viewer?. Stub explicitly.
        coEvery { repository.getViewer() } returns Result.success(null)
    }

    private fun newViewModel() = ComposeViewModel(repository, context)

    private val sampleActor = Actor(
        id = "actor-1",
        name = "Alice",
        handle = "alice@hackers.pub",
        avatarUrl = "https://example.com/avatar.png",
    )

    private fun samplePost(
        id: String = "post-1",
        actor: Actor = sampleActor,
        mentions: List<String> = emptyList(),
    ) = Post(
        id = id,
        typename = "Note",
        name = null,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>hello</p>",
        excerpt = "hello",
        url = null,
        viewerHasShared = false,
        actor = actor,
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = mentions,
        reactionGroups = emptyList(),
    )

    @Test
    fun `setReplyTarget injects mention prefix when content is blank`() = runTest {
        val post = samplePost()
        coEvery { repository.getPostDetail("post-1") } returns Result.success(
            PostDetailResult(
                post = post,
                reactionGroups = emptyList(),
                replies = emptyList(),
                hasMoreReplies = false,
                repliesEndCursor = null,
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()

        vm.setReplyTarget("post-1")
        advanceUntilIdle()

        assertEquals("@alice@hackers.pub ", vm.uiState.value.content)
    }

    @Test
    fun `setReplyTarget does not overwrite when user already typed content`() = runTest {
        val post = samplePost()
        coEvery { repository.getPostDetail("post-1") } returns Result.success(
            PostDetailResult(
                post = post,
                reactionGroups = emptyList(),
                replies = emptyList(),
                hasMoreReplies = false,
                repliesEndCursor = null,
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("user typed this")
        vm.setReplyTarget("post-1")
        advanceUntilIdle()

        assertEquals("user typed this", vm.uiState.value.content)
    }
}
