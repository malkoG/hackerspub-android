package pub.hackers.android.ui.screens.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleActor = Actor(
        id = "a1",
        name = "Alice",
        handle = "alice@hackers.pub",
        avatarUrl = "https://example.com/avatar.png",
    )

    @Test
    fun `ProfileStateDispatch shows loading when loading and actor is null`() {
        composeRule.setContent {
            TestTheme {
                ProfileStateDispatch(
                    actor = null,
                    isLoading = true,
                    error = null,
                    onRetry = {},
                ) { Text("content-sentinel") }
            }
        }

        // Content slot must NOT be invoked
        composeRule.onAllNodesWithText("content-sentinel").assertCountEquals(0)
    }

    @Test
    fun `ProfileStateDispatch shows error message when error and actor is null`() {
        composeRule.setContent {
            TestTheme {
                ProfileStateDispatch(
                    actor = null,
                    isLoading = false,
                    error = "Something broke",
                    onRetry = {},
                ) { Text("content-sentinel") }
            }
        }

        composeRule.onNodeWithText("Something broke").assertIsDisplayed()
        composeRule.onAllNodesWithText("content-sentinel").assertCountEquals(0)
    }

    @Test
    fun `ProfileStateDispatch onRetry is invoked when retry is clicked`() {
        var retryCount = 0
        composeRule.setContent {
            TestTheme {
                ProfileStateDispatch(
                    actor = null,
                    isLoading = false,
                    error = "Network error",
                    onRetry = { retryCount++ },
                ) { Text("content-sentinel") }
            }
        }

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun `ProfileStateDispatch shows content and passes actor when actor is not null`() {
        composeRule.setContent {
            TestTheme {
                ProfileStateDispatch(
                    actor = sampleActor,
                    isLoading = false,
                    error = null,
                    onRetry = {},
                ) { resolvedActor ->
                    Text("actor-handle-is-${resolvedActor.handle}")
                }
            }
        }

        composeRule.onNodeWithText("actor-handle-is-alice@hackers.pub").assertIsDisplayed()
    }

    @Test
    fun `ProfileStateDispatch shows content even during refresh when actor is already loaded`() {
        // isLoading=true with existing actor → should still show content (stale-while-revalidate)
        composeRule.setContent {
            TestTheme {
                ProfileStateDispatch(
                    actor = sampleActor,
                    isLoading = true,
                    error = null,
                    onRetry = {},
                ) { resolvedActor ->
                    Text("content-for-${resolvedActor.handle}")
                }
            }
        }

        composeRule.onNodeWithText("content-for-alice@hackers.pub").assertIsDisplayed()
    }
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
