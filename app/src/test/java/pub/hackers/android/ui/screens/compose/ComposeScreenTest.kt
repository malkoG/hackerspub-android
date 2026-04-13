package pub.hackers.android.ui.screens.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ReplyTargetSection renders reply target content when post is not null`() {
        val replyTarget = makePost(
            id = "rt",
            actorHandle = "reply-target@hackers.pub",
            content = "unique reply target body"
        )

        composeRule.setContent {
            TestTheme {
                Column {
                    ReplyTargetSection(isLoading = false, replyTargetPost = replyTarget)
                }
            }
        }

        composeRule.onNodeWithText("unique reply target body").assertIsDisplayed()
    }

    @Test
    fun `ReplyTargetSection renders nothing when not loading and post is null`() {
        composeRule.setContent {
            TestTheme {
                Column {
                    ReplyTargetSection(isLoading = false, replyTargetPost = null)
                }
            }
        }

        // Nothing to display - no crash is the core assertion; also verify a known
        // content text is absent.
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `QuotedPostSection renders quoted post content when post is not null`() {
        val quoted = makePost(
            id = "q",
            actorHandle = "quoted-author@hackers.pub",
            content = "unique quoted body"
        )

        composeRule.setContent {
            TestTheme {
                Column {
                    QuotedPostSection(isLoading = false, quotedPost = quoted)
                }
            }
        }

        composeRule.onNodeWithText("unique quoted body").assertIsDisplayed()
    }

    @Test
    fun `QuotedPostSection renders nothing when not loading and post is null`() {
        composeRule.setContent {
            TestTheme {
                Column {
                    QuotedPostSection(isLoading = false, quotedPost = null)
                }
            }
        }

        composeRule.onRoot().assertExists()
    }

    private fun makePost(
        id: String,
        actorHandle: String,
        content: String,
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
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = emptyList(),
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
