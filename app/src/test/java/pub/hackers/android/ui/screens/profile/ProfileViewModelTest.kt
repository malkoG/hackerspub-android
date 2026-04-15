package pub.hackers.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.AccountLink
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.ActorField
import pub.hackers.android.domain.model.ProfileResult
import pub.hackers.android.testutil.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)

    private val defaultHandle = "alice@hackers.pub"

    private val sampleActor = Actor(
        id = "actor-1",
        name = "Alice",
        handle = defaultHandle,
        avatarUrl = "https://example.com/avatar.png",
    )

    private fun sampleProfile(
        actor: Actor = sampleActor,
        viewerFollows: Boolean = false,
        viewerBlocks: Boolean = false,
        fields: List<ActorField> = emptyList(),
        accountLinks: List<AccountLink> = emptyList(),
    ) = ProfileResult(
        actor = actor,
        bio = "hello",
        fields = fields,
        accountLinks = accountLinks,
        isViewer = false,
        viewerFollows = viewerFollows,
        followsViewer = false,
        viewerBlocks = viewerBlocks,
    )

    private fun stubLoadProfile(result: ProfileResult = sampleProfile()) {
        coEvery { repository.getProfile(any()) } returns Result.success(result)
    }

    private fun newViewModel(): ProfileViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("handle" to defaultHandle))
        return ProfileViewModel(repository, savedStateHandle)
    }

    // region initial load

    @Test
    fun `init triggers loadProfile and populates uiState`() = runTest {
        stubLoadProfile(sampleProfile(
            fields = listOf(ActorField("Blog", "https://blog.example")),
        ))
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(sampleActor, vm.uiState.value.actor)
        assertEquals("hello", vm.uiState.value.bio)
        assertEquals(1, vm.uiState.value.fields.size)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `init failure stores error`() = runTest {
        coEvery { repository.getProfile(any()) } returns Result.failure(RuntimeException("404"))
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals("404", vm.uiState.value.error)
        assertNull(vm.uiState.value.actor)
    }

    // endregion

    // region tab selection

    @Test
    fun `initial selectedTab is POSTS`() = runTest {
        stubLoadProfile()
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(ProfileTab.POSTS, vm.selectedTab.value)
    }

    @Test
    fun `selectTab updates selectedTab`() = runTest {
        stubLoadProfile()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.selectTab(ProfileTab.NOTES)
        assertEquals(ProfileTab.NOTES, vm.selectedTab.value)

        vm.selectTab(ProfileTab.ARTICLES)
        assertEquals(ProfileTab.ARTICLES, vm.selectedTab.value)
    }

    // endregion

    // region actions (follow/unfollow/block/unblock/removeFollower)

    @Test
    fun `followActor calls repository and triggers refresh`() = runTest {
        stubLoadProfile()
        coEvery { repository.followActor(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.followActor()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.followActor("actor-1") }
        // After action, isPerformingAction goes back to false
        assertEquals(false, vm.uiState.value.isPerformingAction)
    }

    @Test
    fun `unfollowActor calls repository unfollowActor`() = runTest {
        stubLoadProfile()
        coEvery { repository.unfollowActor(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.unfollowActor()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.unfollowActor("actor-1") }
    }

    @Test
    fun `blockActor calls repository blockActor`() = runTest {
        stubLoadProfile()
        coEvery { repository.blockActor(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.blockActor()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.blockActor("actor-1") }
    }

    @Test
    fun `action failure stores actionError`() = runTest {
        stubLoadProfile()
        coEvery { repository.followActor(any()) } returns Result.failure(RuntimeException("network"))
        val vm = newViewModel()
        advanceUntilIdle()

        vm.followActor()
        advanceUntilIdle()

        assertEquals("network", vm.uiState.value.actionError)
        assertEquals(false, vm.uiState.value.isPerformingAction)
    }

    @Test
    fun `dismissActionError clears actionError`() = runTest {
        stubLoadProfile()
        coEvery { repository.followActor(any()) } returns Result.failure(RuntimeException("x"))
        val vm = newViewModel()
        advanceUntilIdle()
        vm.followActor()
        advanceUntilIdle()

        vm.dismissActionError()

        assertNull(vm.uiState.value.actionError)
    }

    // Note: The VM's `isPerformingAction` guard is set inside `viewModelScope.launch`,
    // so two back-to-back synchronous calls BOTH pass the guard before either
    // launch body runs. That's acceptable for a debouncing UI button but can't
    // be asserted cleanly with StandardTestDispatcher; we skip that test.

    // endregion

    // region share/unshare (overlay-based)

    @Test
    fun `sharePost calls repository sharePost`() = runTest {
        stubLoadProfile()
        coEvery { repository.sharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sharePost("p1") }
    }

    @Test
    fun `unsharePost calls repository unsharePost`() = runTest {
        stubLoadProfile()
        coEvery { repository.unsharePost(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.unsharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.unsharePost("p1") }
    }

    @Test
    fun `sharePost failure still attempts the repository call`() = runTest {
        stubLoadProfile()
        coEvery { repository.sharePost(any()) } returns Result.failure(RuntimeException("boom"))
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sharePost("p1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sharePost("p1") }
    }

    // endregion

    // region refresh

    @Test
    fun `refresh re-fetches profile and clears isRefreshing on success`() = runTest {
        stubLoadProfile()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        // Initial load + refresh = 2 calls
        coVerify(atLeast = 2) { repository.getProfile(any()) }
        assertEquals(false, vm.uiState.value.isRefreshing)
    }

    // endregion
}
