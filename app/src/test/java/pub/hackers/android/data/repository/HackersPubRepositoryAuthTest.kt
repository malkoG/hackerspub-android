package pub.hackers.android.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.benasher44.uuid.uuid4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.graphql.CompleteLoginChallengeMutation
import pub.hackers.android.graphql.LoginByPasskeyMutation

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HackersPubRepositoryAuthTest {

    private val apolloClient = mockk<ApolloClient>()
    private val okHttpClient = mockk<OkHttpClient>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private fun newRepository() = HackersPubRepository(
        apolloClient = apolloClient,
        okHttpClient = okHttpClient,
        context = context,
    )

    private fun <D : Operation.Data> buildResponse(
        operation: Operation<D>,
        data: D?,
        errors: List<Error>? = null,
    ): ApolloResponse<D> = ApolloResponse.Builder(operation, uuid4())
        .data(data)
        .errors(errors)
        .build()

    private fun stubCompleteLoginChallenge(
        data: CompleteLoginChallengeMutation.Data?,
        errors: List<Error>? = null,
    ) {
        val call = mockk<ApolloCall<CompleteLoginChallengeMutation.Data>>()
        every { apolloClient.mutation(any<CompleteLoginChallengeMutation>()) } answers {
            val operation = firstArg<CompleteLoginChallengeMutation>()
            coEvery { call.execute() } returns buildResponse(operation, data, errors)
            call
        }
    }

    private fun stubLoginByPasskey(
        data: LoginByPasskeyMutation.Data?,
        errors: List<Error>? = null,
    ) {
        val call = mockk<ApolloCall<LoginByPasskeyMutation.Data>>()
        every { apolloClient.mutation(any<LoginByPasskeyMutation>()) } answers {
            val operation = firstArg<LoginByPasskeyMutation>()
            coEvery { call.execute() } returns buildResponse(operation, data, errors)
            call
        }
    }

    private fun completeLoginSessionData() = CompleteLoginChallengeMutation.Data(
        completeLoginChallenge = CompleteLoginChallengeMutation.CompleteLoginChallenge(
            __typename = "Session",
            onSession = CompleteLoginChallengeMutation.OnSession(
                id = "session-token",
                account = CompleteLoginChallengeMutation.Account(
                    id = "account-1",
                    username = "alice",
                    name = "Alice",
                    avatarUrl = "https://hackers.pub/avatar.png",
                    handle = "@alice@hackers.pub",
                ),
            ),
            onAccountBannedError = null,
        )
    )

    private fun completeLoginBannedData() = CompleteLoginChallengeMutation.Data(
        completeLoginChallenge = CompleteLoginChallengeMutation.CompleteLoginChallenge(
            __typename = "AccountBannedError",
            onSession = null,
            onAccountBannedError = CompleteLoginChallengeMutation.OnAccountBannedError(
                since = "2026-07-05T00:00:00Z",
            ),
        )
    )

    private fun passkeySessionData() = LoginByPasskeyMutation.Data(
        loginByPasskey = LoginByPasskeyMutation.LoginByPasskey(
            __typename = "Session",
            onSession = LoginByPasskeyMutation.OnSession(
                id = "passkey-session-token",
                account = LoginByPasskeyMutation.Account(
                    id = "account-2",
                    username = "bob",
                    name = "Bob",
                    avatarUrl = "https://hackers.pub/bob.png",
                    handle = "@bob@hackers.pub",
                ),
            ),
            onAccountBannedError = null,
        )
    )

    private fun passkeyBannedData() = LoginByPasskeyMutation.Data(
        loginByPasskey = LoginByPasskeyMutation.LoginByPasskey(
            __typename = "AccountBannedError",
            onSession = null,
            onAccountBannedError = LoginByPasskeyMutation.OnAccountBannedError(
                since = "2026-07-05T00:00:00Z",
            ),
        )
    )

    @Test
    fun `completeLoginChallenge maps Session union result`() = runTest {
        stubCompleteLoginChallenge(completeLoginSessionData())

        val result = newRepository().completeLoginChallenge(
            token = "challenge-token",
            code = "ABCDEF",
        )

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("session-token", session.id)
        assertEquals("account-1", session.account.id)
        assertEquals("alice", session.account.username)
        assertEquals("@alice@hackers.pub", session.account.handle)
    }

    @Test
    fun `completeLoginChallenge returns failure for banned account result`() = runTest {
        stubCompleteLoginChallenge(completeLoginBannedData())

        val result = newRepository().completeLoginChallenge(
            token = "challenge-token",
            code = "ABCDEF",
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Account is banned since 2026-07-05T00:00:00Z",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun `loginByPasskey maps Session union result`() = runTest {
        stubLoginByPasskey(passkeySessionData())

        val result = newRepository().loginByPasskey(
            sessionId = "session-id",
            authenticationResponse = mapOf("id" to "credential-id"),
        )

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("passkey-session-token", session.id)
        assertEquals("account-2", session.account.id)
        assertEquals("bob", session.account.username)
        assertEquals("@bob@hackers.pub", session.account.handle)
    }

    @Test
    fun `loginByPasskey returns failure for banned account result`() = runTest {
        stubLoginByPasskey(passkeyBannedData())

        val result = newRepository().loginByPasskey(
            sessionId = "session-id",
            authenticationResponse = mapOf("id" to "credential-id"),
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Account is banned since 2026-07-05T00:00:00Z",
            result.exceptionOrNull()?.message,
        )
    }
}
