package com.example.momentra.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.momentra.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In via Credential Manager's native account picker, then Firebase credential exchange.
 *
 * Requires an Android OAuth client (package + SHA) in Firebase/Google Cloud, and uses the project's
 * Web client ID as [GetSignInWithGoogleOption] serverClientId for ID tokens.
 */
class GoogleSignInClient(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun signIn(context: Context): Result<FirebaseUser> = runCatching {
        val activity = context as? Activity
            ?: error("Google sign-in requires an Activity context")

        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        require(serverClientId.isNotEmpty()) {
            "Missing GOOGLE_WEB_CLIENT_ID. Set it in local.properties or BuildConfig."
        }

        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val credentialManager = CredentialManager.create(activity)
        val result = try {
            credentialManager.getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            error("Sign-in cancelled.")
        } catch (e: NoCredentialException) {
            error(
                "No Google accounts available. Add a Google account on this device, or check that " +
                    "Firebase Android SHA fingerprints are configured (error 28444).",
            )
        }

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type from Google Sign-In")
        }

        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

        authResult.user
            ?: error("Google sign-in succeeded but Firebase user is null")
    }
}
