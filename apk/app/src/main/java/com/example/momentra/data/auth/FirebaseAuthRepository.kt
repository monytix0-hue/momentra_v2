package com.example.momentra.data.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class PhoneSendResult {
    data object CodeSent : PhoneSendResult()
    data class AutoVerified(val user: FirebaseUser) : PhoneSendResult()
}

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val googleSignInClient: GoogleSignInClient = GoogleSignInClient(firebaseAuth),
) {
    private var phoneVerificationId: String? = null

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isSignedIn: Boolean
        get() = currentUser != null

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-in succeeded but user is null")
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Registration succeeded but user is null")
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> =
        googleSignInClient.signIn(context)

    suspend fun sendPhoneCode(activity: Activity, rawPhone: String): Result<PhoneSendResult> =
        runCatching {
            val e164 = toE164(rawPhone)
                ?: error("Enter a phone number in E.164 format, e.g. +919876543210.")
            suspendCancellableCoroutine { cont ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        firebaseAuth.signInWithCredential(credential)
                            .addOnSuccessListener { result ->
                                val user = result.user
                                if (!cont.isActive) return@addOnSuccessListener
                                if (user == null) {
                                    cont.resume(Result.failure(IllegalStateException("Phone sign-in succeeded but user is null")))
                                } else {
                                    cont.resume(Result.success(PhoneSendResult.AutoVerified(user)))
                                }
                            }
                            .addOnFailureListener { e ->
                                if (cont.isActive) cont.resume(Result.failure(e))
                            }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        phoneVerificationId = verificationId
                        if (cont.isActive) cont.resume(Result.success(PhoneSendResult.CodeSent))
                    }
                }

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(e164)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }.getOrThrow()
        }

    suspend fun confirmPhoneCode(code: String): Result<FirebaseUser> = runCatching {
        val verificationId = phoneVerificationId
            ?: error("Request an SMS code first.")
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        firebaseAuth.signInWithCredential(credential).await().user
            ?: error("Phone sign-in succeeded but user is null")
    }

    fun resetPhoneFlow() {
        phoneVerificationId = null
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    fun signOut() {
        resetPhoneFlow()
        firebaseAuth.signOut()
    }

    companion object {
        fun toE164(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.startsWith("+")) {
                val digits = trimmed.drop(1).filter { it.isDigit() }
                return if (digits.length >= 8) "+$digits" else null
            }
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length < 10) return null
            return if (digits.length == 10) "+91$digits" else "+$digits"
        }
    }
}
