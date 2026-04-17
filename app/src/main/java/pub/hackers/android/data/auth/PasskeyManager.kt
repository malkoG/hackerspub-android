package pub.hackers.android.data.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passkey (WebAuthn) sign-in and registration via the Jetpack Credential Manager.
 *
 * The passkey-specific classes (`PublicKeyCredential`,
 * `CreatePublicKeyCredentialRequest`, etc.) require **API 28+**. This class's
 * constructor and `credentialManager` field are API 26-safe, so the Hilt graph
 * can instantiate it on all supported devices. The two public entry points
 * ([authenticate] and [register]) are annotated `@RequiresApi(Build.VERSION_CODES.P)`
 * — callers must gate invocation with a runtime SDK check.
 */
@Singleton
class PasskeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun authenticate(optionsJson: String, activity: Activity): String {
        android.util.Log.d("PasskeyAuth", "authenticate: creating request with options: ${optionsJson.take(200)}")
        val request = GetCredentialRequest(
            listOf(GetPublicKeyCredentialOption(optionsJson))
        )
        android.util.Log.d("PasskeyAuth", "authenticate: calling getCredential")
        try {
            val result = credentialManager.getCredential(activity, request)
            android.util.Log.d("PasskeyAuth", "authenticate: got result, credential type=${result.credential.type}")
            val credential = result.credential as PublicKeyCredential
            android.util.Log.d("PasskeyAuth", "authenticate: success")
            return credential.authenticationResponseJson
        } catch (e: Exception) {
            android.util.Log.e("PasskeyAuth", "authenticate: failed", e)
            throw e
        }
    }

    @SuppressLint("PublicKeyCredential")
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun register(optionsJson: String, activity: Activity): String {
        android.util.Log.d("PasskeyAuth", "register: creating request")
        val request = CreatePublicKeyCredentialRequest(optionsJson)
        try {
            val result = credentialManager.createCredential(activity, request)
            android.util.Log.d("PasskeyAuth", "register: success")
            val credential = result as CreatePublicKeyCredentialResponse
            return credential.registrationResponseJson
        } catch (e: Exception) {
            android.util.Log.e("PasskeyAuth", "register: failed", e)
            throw e
        }
    }
}
