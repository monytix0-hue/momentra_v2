package com.example.momentra.data.local

import android.content.Context
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiResultException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.util.UUID

data class OfflineCommand(
    val id: String,
    val path: String,
    val momentId: String,
    val idempotencyKey: String,
    val bodyJson: String,
    val createdAt: Long,
    val status: String,
    val error: String? = null,
    val method: String? = null,
    val parentCommandId: String? = null,
    val fileName: String? = null,
)

/** Per-user file of writes waiting for a connection. */
object OfflineOutbox {
    const val LOCAL_ID = "offline"

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<OfflineCommand>>() {}.type
    private val createdIdKeys = listOf("memoryId", "expenseId", "momentId")
    private var filesDir: File? = null
    private var userId: String? = null
    private var bootstrapRefresh = false

    @Volatile
    var lastQueuedCommandId: String? = null
        private set

    var onChanged: (() -> Unit)? = null

    fun install(context: Context) {
        filesDir = context.applicationContext.filesDir
    }

    fun bindUser(userId: String) {
        if (userId.isBlank()) return
        this.userId = userId
    }

    @Synchronized
    fun enqueue(
        path: String,
        momentId: String,
        idempotencyKey: String,
        body: Any,
        method: String = "POST",
        parentCommandId: String? = null,
    ): String? {
        val uid = userId ?: return null
        val items = load(uid)
        val existing = items.firstOrNull { it.idempotencyKey == idempotencyKey && it.status == "pending" }
        if (existing != null) {
            lastQueuedCommandId = existing.id
            return existing.id
        }
        val id = UUID.randomUUID().toString()
        items.add(
            OfflineCommand(
                id = id,
                path = path,
                momentId = momentId,
                idempotencyKey = idempotencyKey,
                bodyJson = gson.toJson(body),
                createdAt = System.currentTimeMillis(),
                status = "pending",
                error = null,
                method = method,
                parentCommandId = parentCommandId,
                fileName = null,
            ),
        )
        save(uid, items)
        lastQueuedCommandId = id
        onChanged?.invoke()
        return id
    }

    /** Store photo bytes and attach them after [resourceId]'s parent command syncs. */
    @Synchronized
    fun enqueuePhoto(
        attachPath: String,
        momentId: String,
        bytes: ByteArray,
        contentType: String,
        scopeId: String,
        resourceId: String,
    ) {
        val uid = userId ?: return
        val id = UUID.randomUUID().toString()
        val file = mediaFile(id) ?: return
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        val body = mapOf(
            "contentType" to contentType,
            "byteSize" to bytes.size,
            "scopeType" to "MOMENT",
            "scopeId" to scopeId,
        )
        val items = load(uid)
        items.add(
            OfflineCommand(
                id = id,
                path = attachPath,
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                bodyJson = gson.toJson(body),
                createdAt = System.currentTimeMillis(),
                status = "pending",
                error = null,
                method = "MEDIA",
                parentCommandId = if (resourceId == LOCAL_ID) lastQueuedCommandId else null,
                fileName = id,
            ),
        )
        save(uid, items)
        onChanged?.invoke()
    }

    @Synchronized
    fun pendingCount(): Int = userId?.let { uid -> load(uid).count { it.status == "pending" } } ?: 0

    @Synchronized
    fun failedMessage(): String? =
        userId?.let { uid -> load(uid).firstOrNull { it.status == "failed" }?.error }

    @Synchronized
    fun takeBootstrapRefresh(): Boolean {
        val value = bootstrapRefresh
        bootstrapRefresh = false
        return value
    }

    /** Replay pending commands in order. A photo waits until its parent command has a real id. */
    @Synchronized
    fun flush(): Int {
        val uid = userId ?: return 0
        val items = load(uid)
        var synced = 0
        val remaining = mutableListOf<OfflineCommand>()
        val deferred = mutableListOf<OfflineCommand>()
        val createdIds = mutableMapOf<String, String>()
        val outcomes = mutableMapOf<String, String>()
        var stop = false
        fun replayOne(item: OfflineCommand, allowDefer: Boolean): Boolean {
            if (item.status == "failed") {
                remaining.add(item)
                outcomes[item.id] = "failed"
                return true
            }
            if (stop) {
                remaining.add(item)
                outcomes[item.id] = "stopped"
                return true
            }
            val prepared = prepare(item, createdIds, outcomes, items)
            when (prepared.kind) {
                "defer" -> {
                    if (allowDefer) deferred.add(item) else remaining.add(item)
                    return true
                }
                "fail" -> {
                    remaining.add(item.copy(status = "failed", error = prepared.message))
                    outcomes[item.id] = "failed"
                    return true
                }
            }
            val command = prepared.command
            val bytes = command.fileName?.let { name -> mediaFile(name)?.takeIf { it.exists() }?.readBytes() }
            if ((command.method ?: "POST") == "MEDIA" && bytes == null) {
                remaining.add(command.copy(status = "failed", error = "The saved photo is missing on this device."))
                outcomes[command.id] = "failed"
                return true
            }
            when (
                val result = ApiClient.replay(
                    command.method ?: "POST",
                    command.path,
                    command.idempotencyKey,
                    command.bodyJson,
                    bytes,
                )
            ) {
                is OfflineReplay.Synced -> {
                    synced += 1
                    createdResourceId(result.body)?.let { createdIds[command.id] = it }
                    outcomes[command.id] = "synced"
                    if (needsBootstrap(command.path)) bootstrapRefresh = true
                    command.fileName?.let { mediaFile(it)?.delete() }
                }
                is OfflineReplay.Offline, is OfflineReplay.Unauthorized -> {
                    remaining.add(command)
                    outcomes[command.id] = "stopped"
                    stop = true
                }
                is OfflineReplay.Rejected -> {
                    val message = result.message.orEmpty()
                    if (message.contains("already deleted", ignoreCase = true)) {
                        synced += 1
                        outcomes[command.id] = "synced"
                        if (needsBootstrap(command.path)) bootstrapRefresh = true
                        command.fileName?.let { mediaFile(it)?.delete() }
                    } else {
                        remaining.add(
                            command.copy(status = "failed", error = result.message ?: "Could not sync a saved change."),
                        )
                        outcomes[command.id] = "failed"
                    }
                }
            }
            return true
        }
        for (item in items) replayOne(item, allowDefer = true)
        for (item in deferred) replayOne(item, allowDefer = false)
        save(uid, remaining)
        return synced
    }

    private fun prepare(
        item: OfflineCommand,
        createdIds: Map<String, String>,
        outcomes: Map<String, String>,
        all: List<OfflineCommand>,
    ): Prepared {
        val method = item.method ?: "POST"
        val parent = item.parentCommandId
        val needsId = parent != null && item.path.contains("/$LOCAL_ID/")
        if (!needsId) return Prepared("ready", item.copy(method = method))
        val id = createdIds[parent]
        if (id != null) {
            return Prepared("ready", item.copy(path = item.path.replace("/$LOCAL_ID/", "/$id/"), method = method))
        }
        return when (outcomes[parent]) {
            "failed" -> Prepared(
                "fail",
                item,
                "The saved record could not sync, so its photo stayed on this device.",
            )
            "stopped" -> Prepared("defer", item)
            "synced" -> Prepared(
                "fail",
                item,
                "The saved record synced without an id, so its photo could not be attached.",
            )
            else -> if (all.any { it.id == parent }) {
                Prepared("defer", item)
            } else {
                Prepared("fail", item, "The saved record is gone, so its photo could not be attached.")
            }
        }
    }

    private fun createdResourceId(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        for (key in createdIdKeys) {
            val value = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
                .find(raw)
                ?.groupValues
                ?.getOrNull(1)
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun needsBootstrap(path: String): Boolean {
        if (path == "v1/moments") return true
        if (path.startsWith("v1/group/invites/") && path.endsWith("/redeem")) return true
        if (path.startsWith("v1/company/invites/") && path.endsWith("/redeem")) return true
        if (Regex("v1/moments/[^/]+").matches(path)) return true
        if (Regex("v1/moments/[^/]+/delete").matches(path)) return true
        return false
    }

    private fun file(userId: String): File? {
        val dir = filesDir ?: return null
        return File(dir, "offline-outbox-$userId.json")
    }

    private fun mediaFile(name: String): File? {
        val dir = filesDir ?: return null
        return File(File(dir, "offline-media"), name)
    }

    private fun load(userId: String): MutableList<OfflineCommand> {
        val raw = file(userId)?.takeIf { it.exists() }?.readText().orEmpty()
        if (raw.isBlank()) return mutableListOf()
        return runCatching { gson.fromJson<MutableList<OfflineCommand>>(raw, listType) }.getOrNull()
            ?: mutableListOf()
    }

    private fun save(userId: String, items: List<OfflineCommand>) {
        file(userId)?.writeText(gson.toJson(items))
    }

    private data class Prepared(
        val kind: String,
        val command: OfflineCommand,
        val message: String? = null,
    )
}

sealed class OfflineReplay {
    data class Synced(val body: String? = null) : OfflineReplay()
    data object Offline : OfflineReplay()
    data object Unauthorized : OfflineReplay()
    data class Rejected(val message: String?) : OfflineReplay()
}

private fun Throwable.isOfflineFailure(): Boolean =
    this is ApiResultException.Network || this is IOException || cause is IOException

internal fun <T> Result<T>.orQueueOffline(
    path: String,
    momentId: String,
    idempotencyKey: String,
    body: Any,
    placeholder: T,
    mapError: (Throwable) -> Throwable,
    method: String = "POST",
): Result<T> {
    val error = exceptionOrNull() ?: return this
    val mapped = mapError(error)
    if (mapped.isOfflineFailure()) {
        OfflineOutbox.enqueue(path, momentId, idempotencyKey, body, method)
        return Result.success(placeholder)
    }
    return Result.failure(mapped)
}

internal fun <T> Result<T>.orQueuePhoto(
    attachPath: String,
    momentId: String,
    bytes: ByteArray,
    contentType: String,
    scopeId: String,
    resourceId: String,
    placeholder: T,
    mapError: (Throwable) -> Throwable,
): Result<T> {
    val error = exceptionOrNull() ?: return this
    val mapped = mapError(error)
    if (mapped.isOfflineFailure()) {
        OfflineOutbox.enqueuePhoto(attachPath, momentId, bytes, contentType, scopeId, resourceId)
        return Result.success(placeholder)
    }
    return Result.failure(mapped)
}
