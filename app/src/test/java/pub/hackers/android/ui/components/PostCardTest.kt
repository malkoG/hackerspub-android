package pub.hackers.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostLink
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders reply target content when replyTarget is not null`() {
        val replyTarget = makePost(
            id = "2",
            actorHandle = "reply-author@hackers.pub",
            content = "this is the reply target body"
        )
        val post = makePost(
            id = "1",
            actorHandle = "author@hackers.pub",
            content = "main post body",
            replyTarget = replyTarget
        )

        setPostCard(post)

        composeRule.onNodeWithText("this is the reply target body").assertIsDisplayed()
    }

    @Test
    fun `renders repost indicator when sharedPost and lastSharer are set`() {
        val original = makePost(
            id = "original",
            actorHandle = "original-author@hackers.pub",
            content = "original content"
        )
        val sharer = Actor(
            id = "sharer-id",
            name = null,
            handle = "sharer-user@hackers.pub",
            avatarUrl = "https://example.com/sharer-avatar.png"
        )
        val post = makePost(
            id = "shared",
            actorHandle = "wrapper@hackers.pub",
            content = "wrapper content (should not be shown as main)"
        ).copy(sharedPost = original, lastSharer = sharer)

        setPostCard(post)

        // Repost indicator shows sharer's handle
        composeRule.onNodeWithText("sharer-user@hackers.pub").assertIsDisplayed()
        // displayPost is the original; its content should be shown
        composeRule.onNodeWithText("original content").assertIsDisplayed()
    }

    @Test
    fun `renders quoted post preview when quotedPost is not null`() {
        val quoted = makePost(
            id = "quoted-id",
            actorHandle = "quoted-author@hackers.pub",
            content = "quoted body"
        )
        val post = makePost(
            id = "main",
            actorHandle = "author@hackers.pub",
            content = "main content",
            quotedPost = quoted
        )

        setPostCard(post)

        // QuotedPostPreview renders the quoted post body (actor handle is ambiguous
        // because QuotedPostPreview renders it twice: display name fallback + secondary label)
        composeRule.onNodeWithText("quoted body").assertIsDisplayed()
    }

    @Test
    fun `renders link preview when link is set and media empty and quotedPost null`() {
        val link = PostLink(
            title = "Unique Link Title Here",
            description = "a link description",
            url = "https://example.com/article",
            siteName = "Example",
            author = null,
            image = null,
            creator = null,
        )
        val post = makePost(
            id = "main",
            actorHandle = "author@hackers.pub",
            content = "main content",
            link = link
        )

        setPostCard(post)

        composeRule.onNodeWithText("Unique Link Title Here").assertIsDisplayed()
    }

    @Test
    fun `renders only main content when reply quote share and link are all null`() {
        val post = makePost(
            id = "plain",
            actorHandle = "author@hackers.pub",
            content = "just the main content"
        )

        setPostCard(post)

        composeRule.onNodeWithText("just the main content").assertIsDisplayed()
    }

    private fun setPostCard(post: Post) {
        composeRule.setContent {
            TestTheme {
                PostCard(
                    post = post,
                    onClick = {},
                    onProfileClick = {},
                )
            }
        }
    }

    private fun makePost(
        id: String,
        actorHandle: String,
        content: String,
        replyTarget: Post? = null,
        quotedPost: Post? = null,
        link: PostLink? = null,
    ) = Post(
        id = id,
        typename = "Note",
        name = null,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>$content</p>",
        excerpt = content,
        url = null,
        viewerHasShared = false,
        actor = Actor(
            id = "actor-$id",
            name = null,
            handle = actorHandle,
            avatarUrl = "https://example.com/avatar.png"
        ),
        media = emptyList(),
        link = link,
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = emptyList(),
        replyTarget = replyTarget,
        quotedPost = quotedPost,
    )
}

@Composable
private fun TestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides LightAppColors,
        LocalAppTypography provides AppTypographyDefaults,
    ) {
        MaterialTheme(content = content)
    }
}
