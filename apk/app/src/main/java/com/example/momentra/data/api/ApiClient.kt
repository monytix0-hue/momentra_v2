package com.example.momentra.data.api

import android.util.Log
import com.example.momentra.BuildConfig
import com.example.momentra.ui.shell.maestro.QaCorrelationHolder
import com.example.momentra.data.local.OfflineReplay
import com.google.firebase.FirebaseException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import java.util.concurrent.atomic.AtomicReference

/**
 * Caches Firebase ID tokens so every API call does not block on getIdToken().
 * Never throws on the OkHttp dispatcher — deleted/invalid users return null.
 */
private object AuthTokenCache {
    private const val TAG = "AuthTokenCache"
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
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            clear()
            return null
        }
        val fresh = try {
            runBlocking {
                user.getIdToken(forceRefresh).await().token
            }
        } catch (e: Throwable) {
            // FirebaseAuthInvalidUserException (and siblings) must not crash OkHttp Dispatcher.
            Log.w(TAG, "getIdToken failed: ${e.javaClass.simpleName}: ${e.message}")
            clear()
            if (e is FirebaseAuthException || e is FirebaseException) {
                // Local session is stale (user deleted / disabled). Drop Firebase session
                // so the next auth-state emission lands on SignedOut instead of looping.
                runCatching { FirebaseAuth.getInstance().signOut() }
            }
            return null
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

    /** Drop cached token on sign-out / session expiry so in-flight calls do not refresh. */
    fun clearAuthToken() {
        AuthTokenCache.clear()
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

    /** Replay a queued write. MEDIA redoes the signed upload from bytes stored on the device. */
    fun replay(
        method: String,
        path: String,
        idempotencyKey: String,
        bodyJson: String,
        mediaBytes: ByteArray? = null,
    ): OfflineReplay {
        if (method.equals("MEDIA", ignoreCase = true)) {
            if (mediaBytes == null) return OfflineReplay.Rejected("The saved photo is missing on this device.")
            return replayMedia(path, bodyJson, mediaBytes)
        }
        val body = if (method.equals("DELETE", ignoreCase = true)) null else bodyJson
        return execute(method, path, idempotencyKey, body)
    }

    private fun replayMedia(attachPath: String, bodyJson: String, bytes: ByteArray): OfflineReplay {
        val parsed = runCatching { JsonParser.parseString(bodyJson).asJsonObject }.getOrNull()
            ?: return OfflineReplay.Rejected("Saved photo details were unreadable.")
        val contentType = parsed.string("contentType") ?: "image/jpeg"
        val scopeType = parsed.string("scopeType") ?: "MOMENT"
        val scopeId = parsed.string("scopeId")
            ?: return OfflineReplay.Rejected("Saved photo is missing its moment.")
        val intentJson = buildString {
            append("{\"contentType\":").append(Gson().toJson(contentType))
            append(",\"byteSize\":").append(bytes.size)
            append(",\"scopeType\":").append(Gson().toJson(scopeType))
            append(",\"scopeId\":").append(Gson().toJson(scopeId))
            append("}")
        }
        val intent = execute("POST", "v1/media/uploads", UUID.randomUUID().toString(), intentJson)
        if (intent !is OfflineReplay.Synced) return intent
        val data = jsonData(intent.body) ?: return OfflineReplay.Rejected("Upload did not start.")
        val uploadId = data.string("uploadId") ?: return OfflineReplay.Rejected("Upload did not start.")
        val signedUrl = data.string("signedUrl") ?: return OfflineReplay.Rejected("Upload link missing.")
        val storageKey = data.string("storageKey") ?: return OfflineReplay.Rejected("Upload key missing.")
        val put = putSigned(signedUrl, bytes, contentType)
        if (put !is OfflineReplay.Synced) return put
        val complete = execute(
            "POST",
            "v1/media/uploads/$uploadId/complete",
            UUID.randomUUID().toString(),
            "{\"storageKey\":${Gson().toJson(storageKey)}}",
        )
        if (complete !is OfflineReplay.Synced) return complete
        return execute(
            "POST",
            attachPath,
            UUID.randomUUID().toString(),
            "{\"uploadId\":${Gson().toJson(uploadId)}}",
        )
    }

    private fun execute(method: String, path: String, idempotencyKey: String, bodyJson: String?): OfflineReplay {
        val url = ensureTrailingSlash(BuildConfig.API_BASE_URL) + path.removePrefix("/")
        val builder = okhttp3.Request.Builder()
            .url(url)
            .header("Idempotency-Key", idempotencyKey)
        val jsonBody = bodyJson?.toRequestBody("application/json".toMediaType())
        when (method.uppercase()) {
            "PATCH" -> builder.patch(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
            "DELETE" -> if (jsonBody == null) builder.delete() else builder.delete(jsonBody)
            "PUT" -> builder.put(jsonBody ?: ByteArray(0).toRequestBody(null))
            else -> builder.post(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
        }
        return try {
            okHttpClient.newCall(builder.build()).execute().use { response ->
                classify(response.code, response.body?.string())
            }
        } catch (_: IOException) {
            OfflineReplay.Offline
        }
    }

    private fun putSigned(signedUrl: String, bytes: ByteArray, contentType: String): OfflineReplay {
        val request = okhttp3.Request.Builder()
            .url(signedUrl)
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .header("Content-Type", contentType)
            .build()
        return try {
            OkHttpClient.Builder().callTimeout(2, TimeUnit.MINUTES).build()
                .newCall(request)
                .execute()
                .use { response ->
                    if (response.isSuccessful) OfflineReplay.Synced(null)
                    else if (response.code in 400..499) OfflineReplay.Rejected("Could not upload the saved photo.")
                    else OfflineReplay.Offline
                }
        } catch (_: IOException) {
            OfflineReplay.Offline
        }
    }

    private fun classify(code: Int, raw: String?): OfflineReplay = when {
        code in 200..299 -> OfflineReplay.Synced(raw)
        code == 401 -> OfflineReplay.Unauthorized
        code in 400..499 -> {
            val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                .find(raw.orEmpty())
                ?.groupValues
                ?.getOrNull(1)
            OfflineReplay.Rejected(message)
        }
        else -> OfflineReplay.Offline
    }

    private fun jsonData(raw: String?): JsonObject? {
        val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return null
        val data = root.get("data")
        return if (data != null && data.isJsonObject) data.asJsonObject else root
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
