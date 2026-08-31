package com.example.momentra.analytics

import android.content.Context
import android.os.Build
import com.example.momentra.BuildConfig
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.TelemetryEventDto
import com.example.momentra.data.api.TelemetryIngestBody
import com.example.momentra.data.api.TelemetryUserSnapshotDto
import com.example.momentra.data.local.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * First-party telemetry — batches events to POST /v1/telemetry/events (your PostgreSQL store).
 * Works logged-in or anonymous (pre-auth splash/onboarding/login).
 */
class BackendTelemetry private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = AppPreferences(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val pending = ConcurrentLinkedQueue<QueuedEvent>()

    private val anonymousId: String = prefs.getOrCreateTelemetryAnonymousId()
    private var sessionId: String = prefs.getOrCreateTelemetrySessionId()
    private var userSnapshot: TelemetryUserSnapshotDto? = null

    init {
        scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    fun onSessionStart() {
        sessionId = UUID.randomUUID().toString()
        prefs.setTelemetrySessionId(sessionId)
    }

    fun onSessionEnd() {
        enqueue("session_end", mapOf("platform" to "android"))
        scope.launch { flush(sessionEnded = true) }
    }

    fun updateUserSnapshot(snapshot: TelemetryUserSnapshotDto) {
        userSnapshot = snapshot
    }

    fun enqueue(eventName: String, properties: Map<String, Any?>) {
        val screenName = properties["screen_name"] as? String
        val widgetName = properties["widget_name"] as? String
        val props = properties
            .filterKeys { it !in setOf("screen_name", "widget_name") }
            .mapValues { (_, v) -> v ?: "" }

        pending.add(
            QueuedEvent(
                eventName = eventName,
                screenName = screenName,
                widgetName = widgetName,
                clientOccurredAt = telemetryInstant(),
                properties = props,
            ),
        )
        if (pending.size >= MAX_BATCH) {
            scope.launch { flush() }
        }
    }

    suspend fun flush(sessionEnded: Boolean = false) {
        if (BuildConfig.DEBUG) {
            mutex.withLock { pending.clear() }
            return
        }
        mutex.withLock {
            if (pending.isEmpty() && !sessionEnded) return
            val batch = mutableListOf<QueuedEvent>()
            while (pending.isNotEmpty() && batch.size < MAX_BATCH) {
                pending.poll()?.let { batch.add(it) }
            }
            if (batch.isEmpty()) return

            val body = TelemetryIngestBody(
                sessionId = sessionId,
                anonymousId = anonymousId,
                platform = "android",
                appVersion = BuildConfig.VERSION_NAME,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                sessionEndedAt = if (sessionEnded) telemetryInstant() else null,
                userSnapshot = userSnapshot,
                events = batch.map {
                    TelemetryEventDto(
                        eventName = it.eventName,
                        screenName = it.screenName,
                        widgetName = it.widgetName,
                        clientOccurredAt = it.clientOccurredAt,
                        properties = it.properties.mapValues { entry -> entry.value.toString() },
                    )
                },
            )
            val result = runCatching {
                ApiClient.apiService.ingestTelemetry(body)
            }
            result.onFailure { error ->
                val dropBatch = (error as? HttpException)?.code()?.let { it in 400..499 } == true
                if (!dropBatch) {
                    batch.forEach { pending.add(it) }
                }
            }
        }
    }

    private data class QueuedEvent(
        val eventName: String,
        val screenName: String?,
        val widgetName: String?,
        val clientOccurredAt: String,
        val properties: Map<String, Any?>,
    )

    private fun telemetryInstant(): String =
        DateTimeFormatter.ISO_INSTANT.format(Instant.now())

    companion object {
        private const val FLUSH_INTERVAL_MS = 15_000L
        private const val MAX_BATCH = 100

        @Volatile
        private var instance: BackendTelemetry? = null

        fun init(context: Context): BackendTelemetry =
            instance ?: synchronized(this) {
                instance ?: BackendTelemetry(context.applicationContext).also { instance = it }
            }

        fun get(): BackendTelemetry =
            instance ?: error("BackendTelemetry.init() must be called from Application.onCreate")
    }
}
