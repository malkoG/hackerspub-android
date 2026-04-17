package pub.hackers.android.ui.screens.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.exceptions.NoCredentialException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pub.hackers.android.R
import pub.hackers.android.data.auth.PasskeyManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.testutil.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HackersPubRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val passkeyManager = mockk<PasskeyManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val activity = mockk<Activity>(relaxed = true)

    private fun newViewModel() = SignInViewModel(
        repository = repository,
        sessionManager = sessionManager,
        passkeyManager = passkeyManager,
        context = context,
    )

    @Test
    fun `signInWithPasskey shows no-passkey message on NoCredentialException`() = runTest {
        val expectedMessage = "No passkey found."
        every { context.getString(R.string.no_passkey_registered) } returns expectedMessage
        coEvery { repository.getPasskeyAuthenticationOptions(any()) } returns Result.success("{}")
        coEvery { passkeyManager.authenticate(any(), any()) } throws NoCredentialException()

        val vm = newViewModel()
        vm.signInWithPasskey(activity)
        advanceUntilIdle()

        assertEquals(expectedMessage, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isSignedIn)
    }

    @Test
    fun `signInWithPasskey shows exception message on generic failure`() = runTest {
        coEvery { repository.getPasskeyAuthenticationOptions(any()) } returns Result.success("{}")
        coEvery { passkeyManager.authenticate(any(), any()) } throws RuntimeException("timeout")

        val vm = newViewModel()
        vm.signInWithPasskey(activity)
        advanceUntilIdle()

        assertEquals("timeout", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isSignedIn)
    }
}
