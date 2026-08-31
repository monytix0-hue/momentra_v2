package com.example.momentra.data.repository

import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.MomentLifecycleResultDto
import com.example.momentra.data.api.MomentVersionBody
import com.example.momentra.data.api.UpdateMomentBody
import com.example.momentra.data.api.mapHttpFailure
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class MomentLifecycleRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun getVersion(momentId: String): Result<Long> = runCatching {
        api.getMoment(momentId).data.version
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun rename(momentId: String, title: String, expectedVersion: Long): Result<MomentLifecycleResultDto> =
        runCatching {
            api.updateMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = UpdateMomentBody(title = title, expectedVersion = expectedVersion),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun archive(momentId: String, expectedVersion: Long): Result<MomentLifecycleResultDto> =
        runCatching {
            api.archiveMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = MomentVersionBody(expectedVersion = expectedVersion),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun cancel(momentId: String, expectedVersion: Long): Result<MomentLifecycleResultDto> =
        runCatching {
            api.cancelMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = MomentVersionBody(expectedVersion = expectedVersion),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun delete(momentId: String, expectedVersion: Long): Result<MomentLifecycleResultDto> =
        runCatching {
            api.deleteMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = MomentVersionBody(expectedVersion = expectedVersion),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    private fun mapError(e: Throwable): Throwable = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            mapHttpFailure(e.code(), code, msg)
        }
        is IOException -> IOException("Network unavailable", e)
        else -> e
    }
}
