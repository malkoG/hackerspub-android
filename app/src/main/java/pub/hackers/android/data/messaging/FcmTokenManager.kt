package pub.hackers.android.data.messaging

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.graphql.RegisterFcmDeviceTokenMutation
import pub.hackers.android.graphql.UnregisterFcmDeviceTokenMutation
import pub.hackers.android.graphql.type.RegisterFcmDeviceTokenInput
import pub.hackers.android.graphql.type.UnregisterFcmDeviceTokenInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val apolloClient: ApolloClient,
    private val sessionManager: SessionManager,
) {
    companion object {
        private const val TAG = "FcmTokenManager"
    }

    suspend fun registerCurrentToken() {
        val isLoggedIn = sessionManager.isLoggedIn.first()
        if (!isLoggedIn) return

        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get FCM token", e)
            return
        }
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        val isLoggedIn = sessionManager.isLoggedIn.first()
        if (!isLoggedIn) return

        try {
            val response = apolloClient.mutation(
                RegisterFcmDeviceTokenMutation(
                    RegisterFcmDeviceTokenInput(deviceToken = token)
                )
            ).execute()

            if (response.hasErrors()) {
                Log.w(TAG, "FCM token registration returned errors: ${response.errors}")
                return
            }

            val result = response.data?.registerFcmDeviceToken
            when {
                result?.onRegisterFcmDeviceTokenPayload != null ->
                    Log.d(TAG, "FCM token registered")
                result?.onRegisterFcmDeviceTokenFailedError != null ->
                    Log.w(TAG, "FCM token registration failed: ${result.onRegisterFcmDeviceTokenFailedError.message}")
                result?.onInvalidInputError != null ->
                    Log.w(TAG, "FCM token registration invalid input: ${result.onInvalidInputError.inputPath}")
                result?.onNotAuthenticatedError != null ->
                    Log.w(TAG, "FCM token registration not authenticated")
                else ->
                    Log.w(TAG, "FCM token registration unexpected result")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register FCM token", e)
        }
    }

    suspend fun unregisterCurrentToken() {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get FCM token", e)
            return
        }
        unregisterToken(token)
    }

    suspend fun unregisterToken(token: String) {
        try {
            val response = apolloClient.mutation(
                UnregisterFcmDeviceTokenMutation(
                    UnregisterFcmDeviceTokenInput(deviceToken = token)
                )
            ).execute()

            if (response.hasErrors()) {
                Log.w(TAG, "FCM token unregistration returned errors: ${response.errors}")
                return
            }

            val result = response.data?.unregisterFcmDeviceToken
            when {
                result?.onUnregisterFcmDeviceTokenPayload != null ->
                    Log.d(TAG, "FCM token unregistered")
                result?.onInvalidInputError != null ->
                    Log.w(TAG, "FCM token unregistration invalid input: ${result.onInvalidInputError.inputPath}")
                result?.onNotAuthenticatedError != null ->
                    Log.w(TAG, "FCM token unregistration not authenticated")
                else ->
                    Log.w(TAG, "FCM token unregistration unexpected result")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister FCM token", e)
        }
    }
}
