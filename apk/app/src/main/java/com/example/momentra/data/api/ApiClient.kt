package com.example.momentra.data.api

import com.example.momentra.BuildConfig
import com.example.momentra.ui.shell.maestro.QaCorrelationHolder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import java.util.concurrent.atomic.AtomicReference

/** Caches Firebase ID tokens so every API call does not block on getIdToken(). */
private object AuthTokenCache {
    private val tokenRef = AtomicReference<String?>(null)
    private val fetchedAtMs = AtomicReference(0L)
    private const val TTL_MS = 55 * 60 * 1000L

    fun get(forceRefresh: Boolean = false): String? {
        if (!forceRefresh) {
            val cached = tokenRef.get()
            if (cached != null && System.currentTimeMillis() - fetchedAtMs.get() < TTL_MS) {
                return cached
            }
        }
        val fresh = runBlocking {
            FirebaseAuth.getInstance().currentUser?.getIdToken(forceRefresh)?.await()?.token
        }
        if (fresh != null) {
            tokenRef.set(fresh)
            fetchedAtMs.set(System.currentTimeMillis())
        }
        return fresh
    }

    fun clear() {
        tokenRef.set(null)
        fetchedAtMs.set(0L)
    }
}

object ApiClient {

    /** Prefetch Firebase ID token so the first API call does not block on auth. */
    fun warmAuthToken() {
        AuthTokenCache.get()
    }

    private val refreshing = AtomicBoolean(false)

    private val authInterceptor = Interceptor { chain ->
        val token = AuthTokenCache.get()

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
            header("Accept", "application/json")
            header("Content-Type", "application/json")
            // Debug/QA: one-shot override from QaCorrelationHolder; else random UUID.
            header("X-Correlation-Id", QaCorrelationHolder.takeCorrelationId())
            QaCorrelationHolder.peekRunId()?.let { header("X-Maestro-Run-Id", it) }
            header("ngrok-skip-browser-warning", "true")
        }.build()

        chain.proceed(request)
    }

    /** Single-shot Firebase token refresh on 401 — avoids infinite refresh loops. */
    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        if (responseCount(response) >= 2) return@Authenticator null
        if (!refreshing.compareAndSet(false, true)) return@Authenticator null
        try {
            val fresh = AuthTokenCache.get(forceRefresh = true) ?: return@Authenticator null
            response.request.newBuilder()
                .header("Authorization", "Bearer $fresh")
                .build()
        } finally {
            refreshing.set(false)
        }
    }

    /**
     * Bounded retry for idempotent GETs only.
     * POST/PUT/PATCH/DELETE are never retried here (mutations must use Idempotency-Key at call site).
     */
    private val getRetryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var lastError: IOException? = null
        val attempts = if (request.method.equals("GET", ignoreCase = true)) 2 else 1
        repeat(attempts) { attempt ->
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || attempt == attempts - 1 || response.code in 400..499) {
                    return@Interceptor response
                }
                response.close()
            } catch (e: IOException) {
                lastError = e
                if (attempt == attempts - 1) throw e
            }
        }
        throw lastError ?: IOException("GET retry exhausted")
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior: Response? = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
        redactHeader("Idempotency-Key")
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 8 })
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .addInterceptor(authInterceptor)
        .addInterceptor(getRetryInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
