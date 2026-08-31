package com.example.momentra.data.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In via Firebase Auth's browser OAuth flow ([OAuthProvider]).
 *
 * Uses the project's Web Google IdP (already enabled in Identity Toolkit) and does
 * **not** require an Android OAuth client / SHA wiring for Credential Manager (error 28444).
 */
class GoogleSignInClient(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun signIn(context: Context): Result<FirebaseUser> = runCatching {
        val activity = context as? Activity
            ?: error("Google sign-in requires an Activity context")

        val provider = OAuthProvider.newBuilder("google.com")
            .setScopes(listOf("profile", "email"))
            .build()

        val pending = firebaseAuth.pendingAuthResult
        val authResult = if (pending != null) {
            pending.await()
        } else {
            firebaseAuth.startActivityForSignInWithProvider(activity, provider).await()
        }

        authResult.user
            ?: error("Google sign-in succeeded but Firebase user is null")
    }
}
