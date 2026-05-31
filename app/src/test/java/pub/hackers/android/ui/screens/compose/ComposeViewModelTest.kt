package pub.hackers.android.ui.screens.compose

import android.content.Context
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
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
import pub.hackers.android.domain.model.NoteMediumAttachment
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostDetailResult
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.domain.model.QuotePolicy
import pub.hackers.android.domain.model.UploadedMedium
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

    private val replyPostedSignal = ReplyPostedSignal()

    private fun newViewModel() = ComposeViewModel(repository, context, replyPostedSignal)

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
        typename: String = "Note",
        rawContent: String? = null,
        language: String? = null,
        visibility: PostVisibility = PostVisibility.PUBLIC,
        quotePolicy: QuotePolicy = QuotePolicy.EVERYONE,
    ) = Post(
        id = id,
        typename = typename,
        name = null,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>hello</p>",
        excerpt = "hello",
        url = null,
        viewerHasShared = false,
        rawContent = rawContent,
        language = language,
        actor = actor,
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = mentions,
        visibility = visibility,
        quotePolicy = quotePolicy,
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

    @Test
    fun `setInitialContent preloads blank composer`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.setInitialContent("Shared title\n\nhttps://example.com")

        assertEquals("Shared title\n\nhttps://example.com", vm.uiState.value.content)
        assertEquals("Shared title\n\nhttps://example.com".length, vm.uiState.value.cursorPosition)
    }

    @Test
    fun `setInitialContent does not overwrite typed content`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("user typed this")
        vm.setInitialContent("shared content")

        assertEquals("user typed this", vm.uiState.value.content)
    }

    @Test
    fun `setEditTarget loads raw note content`() = runTest {
        val post = samplePost(
            rawContent = "raw **markdown**",
            language = "ko",
            visibility = PostVisibility.UNLISTED,
            quotePolicy = QuotePolicy.FOLLOWERS,
        )
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

        vm.setEditTarget("post-1")
        advanceUntilIdle()

        assertEquals("post-1", vm.uiState.value.editPostId)
        assertEquals("raw **markdown**", vm.uiState.value.content)
        assertEquals("ko", vm.uiState.value.language)
        assertEquals(PostVisibility.UNLISTED, vm.uiState.value.visibility)
        assertEquals(QuotePolicy.FOLLOWERS, vm.uiState.value.quotePolicy)
    }

    @Test
    fun `setEditTarget rejects notes without raw content`() = runTest {
        coEvery { repository.getPostDetail("post-1") } returns Result.success(
            PostDetailResult(
                post = samplePost(rawContent = null),
                reactionGroups = emptyList(),
                replies = emptyList(),
                hasMoreReplies = false,
                repliesEndCursor = null,
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()

        vm.setEditTarget("post-1")
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.editPostId)
        assertEquals("This note cannot be edited", vm.uiState.value.error)
    }

    @Test
    fun `post updates existing note in edit mode`() = runTest {
        val existingPost = samplePost(
            rawContent = "old content",
            language = "en",
            quotePolicy = QuotePolicy.EVERYONE,
        )
        val updatedPost = samplePost(
            id = "post-1",
            rawContent = "new content",
            language = "ja",
            quotePolicy = QuotePolicy.SELF,
        )
        coEvery { repository.getPostDetail("post-1") } returns Result.success(
            PostDetailResult(
                post = existingPost,
                reactionGroups = emptyList(),
                replies = emptyList(),
                hasMoreReplies = false,
                repliesEndCursor = null,
            )
        )
        coEvery {
            repository.updateNote(
                noteId = "post-1",
                content = "new content",
                language = "ja",
                quotePolicy = QuotePolicy.SELF,
            )
        } returns Result.success(updatedPost)

        val vm = newViewModel()
        advanceUntilIdle()

        vm.setEditTarget("post-1")
        advanceUntilIdle()
        vm.updateContent("new content")
        vm.updateLanguage("ja")
        vm.updateQuotePolicy(QuotePolicy.SELF)
        vm.post()
        advanceUntilIdle()

        coVerify {
            repository.updateNote(
                noteId = "post-1",
                content = "new content",
                language = "ja",
                quotePolicy = QuotePolicy.SELF,
            )
        }
        coVerify(exactly = 0) {
            repository.createNote(
                content = any(),
                language = any(),
                visibility = any(),
                quotePolicy = any(),
                replyTargetId = any(),
                quotedPostId = any(),
                media = any(),
            )
        }
        assertEquals(true, vm.uiState.value.isPosted)
    }

    @Test
    fun `post sends selected quote policy for public notes`() = runTest {
        val createdPost = samplePost(id = "created")
        coEvery {
            repository.createNote(
                content = "hello",
                language = any(),
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.FOLLOWERS,
                replyTargetId = null,
                quotedPostId = null,
            )
        } returns Result.success(createdPost)

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("hello")
        vm.updateQuotePolicy(QuotePolicy.FOLLOWERS)
        vm.post()
        advanceUntilIdle()

        coVerify {
            repository.createNote(
                content = "hello",
                language = any(),
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.FOLLOWERS,
                replyTargetId = null,
                quotedPostId = null,
            )
        }
    }

    @Test
    fun `updateLanguage normalizes tag and post uses selected language`() = runTest {
        val createdPost = samplePost(id = "created")
        coEvery {
            repository.createNote(
                content = "bonjour",
                language = "fr-CA",
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.EVERYONE,
                replyTargetId = null,
                quotedPostId = null,
            )
        } returns Result.success(createdPost)

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("bonjour")
        vm.updateLanguage(" fr_CA ")
        vm.post()
        advanceUntilIdle()

        assertEquals("fr-CA", vm.uiState.value.language)
        coVerify {
            repository.createNote(
                content = "bonjour",
                language = "fr-CA",
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.EVERYONE,
                replyTargetId = null,
                quotedPostId = null,
            )
        }
    }

    @Test
    fun `post clamps quote policy to self for followers-only notes`() = runTest {
        val createdPost = samplePost(id = "created")
        coEvery {
            repository.createNote(
                content = "hello",
                language = any(),
                visibility = PostVisibility.FOLLOWERS,
                quotePolicy = QuotePolicy.SELF,
                replyTargetId = null,
                quotedPostId = null,
            )
        } returns Result.success(createdPost)

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("hello")
        vm.updateQuotePolicy(QuotePolicy.EVERYONE)
        vm.updateVisibility(PostVisibility.FOLLOWERS)
        vm.post()
        advanceUntilIdle()

        coVerify {
            repository.createNote(
                content = "hello",
                language = any(),
                visibility = PostVisibility.FOLLOWERS,
                quotePolicy = QuotePolicy.SELF,
                replyTargetId = null,
                quotedPostId = null,
            )
        }
    }

    @Test
    fun `post sends uploaded media with required alt text`() = runTest {
        val createdPost = samplePost(id = "created")
        coEvery { repository.uploadMedium(any(), any()) } coAnswers {
            secondArg<(Int) -> Unit>().invoke(100)
            Result.success(
                UploadedMedium(
                    relayId = "relay-medium-1",
                    uuid = "medium-uuid-1",
                    url = "https://example.com/image.webp",
                    width = 100,
                    height = 80,
                )
            )
        }
        coEvery {
            repository.createNote(
                content = "hello with image",
                language = any(),
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.EVERYONE,
                replyTargetId = null,
                quotedPostId = null,
                media = listOf(NoteMediumAttachment(mediumId = "medium-uuid-1", alt = "diagram")),
            )
        } returns Result.success(createdPost)

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("hello with image")
        vm.addMediaUris(listOf(Uri.parse("content://images/1")))
        advanceUntilIdle()
        val attachmentId = vm.uiState.value.mediaAttachments.single().localId
        vm.updateMediaAltText(attachmentId, "diagram")
        vm.post()
        advanceUntilIdle()

        coVerify {
            repository.createNote(
                content = "hello with image",
                language = any(),
                visibility = PostVisibility.PUBLIC,
                quotePolicy = QuotePolicy.EVERYONE,
                replyTargetId = null,
                quotedPostId = null,
                media = listOf(NoteMediumAttachment(mediumId = "medium-uuid-1", alt = "diagram")),
            )
        }
    }

    @Test
    fun `post blocks media without alt text`() = runTest {
        coEvery { repository.uploadMedium(any(), any()) } returns Result.success(
            UploadedMedium(
                relayId = "relay-medium-1",
                uuid = "medium-uuid-1",
                url = "https://example.com/image.webp",
                width = 100,
                height = 80,
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateContent("hello with image")
        vm.addMediaUris(listOf(Uri.parse("content://images/1")))
        advanceUntilIdle()
        vm.post()
        advanceUntilIdle()

        assertEquals("Alt text is required for every image", vm.uiState.value.error)
        coVerify(exactly = 0) {
            repository.createNote(
                content = any(),
                language = any(),
                visibility = any(),
                quotePolicy = any(),
                replyTargetId = any(),
                quotedPostId = any(),
                media = any(),
            )
        }
    }
}
