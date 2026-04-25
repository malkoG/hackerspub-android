package pub.hackers.android.data.messaging

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.benasher44.uuid.uuid4
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.graphql.RegisterFcmDeviceTokenMutation
import pub.hackers.android.graphql.UnregisterFcmDeviceTokenMutation

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FcmTokenManagerTest {

    private val apolloClient = mockk<ApolloClient>()
    private val sessionManager = mockk<SessionManager>()

    private fun newManager() = FcmTokenManager(apolloClient, sessionManager)

    private fun stubLoggedIn(value: Boolean) {
        every { sessionManager.isLoggedIn } returns flowOf(value)
    }

    private fun <D : Operation.Data> buildResponse(
        operation: com.apollographql.apollo.api.Operation<D>,
        data: D?,
        errors: List<Error>? = null,
    ): ApolloResponse<D> = ApolloResponse.Builder(operation, uuid4())
        .data(data)
        .errors(errors)
        .build()

    private fun stubRegisterMutation(
        data: RegisterFcmDeviceTokenMutation.Data?,
        errors: List<Error>? = null,
    ): CapturingSlot<RegisterFcmDeviceTokenMutation> {
        val slot = slot<RegisterFcmDeviceTokenMutation>()
        val call = mockk<ApolloCall<RegisterFcmDeviceTokenMutation.Data>>()
        every { apolloClient.mutation(capture(slot)) } answers {
            coEvery { call.execute() } returns buildResponse(slot.captured, data, errors)
            call
        }
        return slot
    }

    private fun stubUnregisterMutation(
        data: UnregisterFcmDeviceTokenMutation.Data?,
        errors: List<Error>? = null,
    ): CapturingSlot<UnregisterFcmDeviceTokenMutation> {
        val slot = slot<UnregisterFcmDeviceTokenMutation>()
        val call = mockk<ApolloCall<UnregisterFcmDeviceTokenMutation.Data>>()
        every { apolloClient.mutation(capture(slot)) } answers {
            coEvery { call.execute() } returns buildResponse(slot.captured, data, errors)
            call
        }
        return slot
    }

    private fun registerData(
        typename: String,
        payload: RegisterFcmDeviceTokenMutation.OnRegisterFcmDeviceTokenPayload? = null,
        failed: RegisterFcmDeviceTokenMutation.OnRegisterFcmDeviceTokenFailedError? = null,
        invalidInput: RegisterFcmDeviceTokenMutation.OnInvalidInputError? = null,
        notAuthenticated: RegisterFcmDeviceTokenMutation.OnNotAuthenticatedError? = null,
    ) = RegisterFcmDeviceTokenMutation.Data(
        registerFcmDeviceToken = RegisterFcmDeviceTokenMutation.RegisterFcmDeviceToken(
            __typename = typename,
            onRegisterFcmDeviceTokenPayload = payload,
            onRegisterFcmDeviceTokenFailedError = failed,
            onInvalidInputError = invalidInput,
            onNotAuthenticatedError = notAuthenticated,
        )
    )

    private fun unregisterData(
        typename: String,
        payload: UnregisterFcmDeviceTokenMutation.OnUnregisterFcmDeviceTokenPayload? = null,
        invalidInput: UnregisterFcmDeviceTokenMutation.OnInvalidInputError? = null,
        notAuthenticated: UnregisterFcmDeviceTokenMutation.OnNotAuthenticatedError? = null,
    ) = UnregisterFcmDeviceTokenMutation.Data(
        unregisterFcmDeviceToken = UnregisterFcmDeviceTokenMutation.UnregisterFcmDeviceToken(
            __typename = typename,
            onUnregisterFcmDeviceTokenPayload = payload,
            onInvalidInputError = invalidInput,
            onNotAuthenticatedError = notAuthenticated,
        )
    )

    @Test
    fun `registerToken returns early without calling Apollo when logged out`() = runTest {
        stubLoggedIn(false)

        newManager().registerToken("token-x")

        verify(exactly = 0) { apolloClient.mutation(any<RegisterFcmDeviceTokenMutation>()) }
    }

    @Test
    fun `registerToken sends mutation with provided token when logged in`() = runTest {
        stubLoggedIn(true)
        val slot = stubRegisterMutation(
            registerData(
                typename = "RegisterFcmDeviceTokenPayload",
                payload = RegisterFcmDeviceTokenMutation.OnRegisterFcmDeviceTokenPayload(
                    deviceToken = "token-x",
                    created = "2026-01-01T00:00:00Z",
                    updated = "2026-01-01T00:00:00Z",
                ),
            )
        )

        newManager().registerToken("token-x")

        assertEquals("token-x", slot.captured.input.deviceToken)
    }

    @Test
    fun `registerToken handles hasErrors response without throwing`() = runTest {
        stubLoggedIn(true)
        stubRegisterMutation(
            data = null,
            errors = listOf(Error.Builder(message = "boom").build()),
        )

        newManager().registerToken("token-x")
    }

    @Test
    fun `registerToken handles all union variants without throwing`() = runTest {
        val cases = listOf(
            registerData(
                "RegisterFcmDeviceTokenPayload",
                payload = RegisterFcmDeviceTokenMutation.OnRegisterFcmDeviceTokenPayload(
                    "t", "c", "u"
                ),
            ),
            registerData(
                "RegisterFcmDeviceTokenFailedError",
                failed = RegisterFcmDeviceTokenMutation.OnRegisterFcmDeviceTokenFailedError(
                    message = "limit reached", limit = 5
                ),
            ),
            registerData(
                "InvalidInputError",
                invalidInput = RegisterFcmDeviceTokenMutation.OnInvalidInputError(
                    inputPath = "input.deviceToken"
                ),
            ),
            registerData(
                "NotAuthenticatedError",
                notAuthenticated = RegisterFcmDeviceTokenMutation.OnNotAuthenticatedError(
                    notAuthenticated = "login required"
                ),
            ),
            registerData("UnknownTypename"),
        )
        for (data in cases) {
            stubLoggedIn(true)
            stubRegisterMutation(data)
            newManager().registerToken("token-x")
        }
    }

    @Test
    fun `registerToken does not throw when Apollo execute throws`() = runTest {
        stubLoggedIn(true)
        val call = mockk<ApolloCall<RegisterFcmDeviceTokenMutation.Data>>()
        coEvery { call.execute() } throws RuntimeException("network down")
        every { apolloClient.mutation(any<RegisterFcmDeviceTokenMutation>()) } returns call

        newManager().registerToken("token-x")
    }

    @Test
    fun `unregisterToken sends mutation with provided token`() = runTest {
        val slot = stubUnregisterMutation(
            unregisterData(
                typename = "UnregisterFcmDeviceTokenPayload",
                payload = UnregisterFcmDeviceTokenMutation.OnUnregisterFcmDeviceTokenPayload(
                    deviceToken = "token-x",
                    unregistered = true,
                ),
            )
        )

        newManager().unregisterToken("token-x")

        assertEquals("token-x", slot.captured.input.deviceToken)
    }

    @Test
    fun `unregisterToken handles hasErrors response without throwing`() = runTest {
        stubUnregisterMutation(
            data = null,
            errors = listOf(Error.Builder(message = "boom").build()),
        )

        newManager().unregisterToken("token-x")
    }

    @Test
    fun `unregisterToken handles all union variants without throwing`() = runTest {
        val cases = listOf(
            unregisterData(
                "UnregisterFcmDeviceTokenPayload",
                payload = UnregisterFcmDeviceTokenMutation.OnUnregisterFcmDeviceTokenPayload(
                    "t", true
                ),
            ),
            unregisterData(
                "InvalidInputError",
                invalidInput = UnregisterFcmDeviceTokenMutation.OnInvalidInputError(
                    "input.deviceToken"
                ),
            ),
            unregisterData(
                "NotAuthenticatedError",
                notAuthenticated = UnregisterFcmDeviceTokenMutation.OnNotAuthenticatedError(
                    "login required"
                ),
            ),
            unregisterData("UnknownTypename"),
        )
        for (data in cases) {
            stubUnregisterMutation(data)
            newManager().unregisterToken("token-x")
        }
    }

    @Test
    fun `unregisterToken does not throw when Apollo execute throws`() = runTest {
        val call = mockk<ApolloCall<UnregisterFcmDeviceTokenMutation.Data>>()
        coEvery { call.execute() } throws RuntimeException("network down")
        every { apolloClient.mutation(any<UnregisterFcmDeviceTokenMutation>()) } returns call

        newManager().unregisterToken("token-x")
    }
}
