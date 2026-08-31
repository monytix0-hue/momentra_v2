package com.example.momentra.data.realtime

import com.example.momentra.BuildConfig
import com.example.momentra.data.api.ProjectionUpdatedEvent
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * SSE client for PROJECTION_UPDATED envelopes from GET /v1/realtime/sse.
 * APK refetches projections on each event (see PulseScreen).
 */
class SseClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson(),
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectJob: Job? = null

    private val _projectionUpdates = MutableSharedFlow<ProjectionUpdatedEvent>(extraBufferCapacity = 32)
    val projectionUpdates: SharedFlow<ProjectionUpdatedEvent> = _projectionUpdates.asSharedFlow()

    fun connect(firebaseToken: String) {
        disconnect()
        connectJob = scope.launch {
            val base = BuildConfig.API_BASE_URL.trimEnd('/')
            val request = Request.Builder()
                .url("$base/v1/realtime/sse")
                .header("Authorization", "Bearer $firebaseToken")
                .header("Accept", "text/event-stream")
                .header("ngrok-skip-browser-warning", "true")
                .build()

            runCatching {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                    var eventName: String? = null
                    var dataLine: String? = null
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        when {
                            line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> dataLine = line.removePrefix("data:").trim()
                            line.isBlank() && dataLine != null -> {
                                if (eventName == "PROJECTION_UPDATED") {
                                    val parsed = gson.fromJson(dataLine, ProjectionUpdatedEvent::class.java)
                                    _projectionUpdates.tryEmit(parsed)
                                }
                                eventName = null
                                dataLine = null
                            }
                        }
                    }
                }
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
    }
}
