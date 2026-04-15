package pub.hackers.android.ui.screens.notifications

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.testutil.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)
    private val notificationStateManager = mockk<NotificationStateManager>(relaxed = true)

    private fun newViewModel() = NotificationsViewModel(repository, notificationStateManager)

    @Test
    fun `markAsSeen delegates to NotificationStateManager`() = runTest {
        val vm = newViewModel()

        vm.markAsSeen()
        advanceUntilIdle()

        coVerify(exactly = 1) { notificationStateManager.markAsSeen() }
    }

    @Test
    fun `markAsSeen can be called multiple times`() = runTest {
        val vm = newViewModel()

        vm.markAsSeen()
        vm.markAsSeen()
        vm.markAsSeen()
        advanceUntilIdle()

        coVerify(exactly = 3) { notificationStateManager.markAsSeen() }
    }

    @Test
    fun `notifications flow is exposed`() = runTest {
        // Smoke test — just verify the property exists and is not null.
        // Actual PagingData emissions require paging-testing setup per-method.
        val vm = newViewModel()
        assertNotNull(vm.notifications)
    }

    private fun assertNotNull(any: Any?) {
        org.junit.Assert.assertNotNull(any)
    }
}
