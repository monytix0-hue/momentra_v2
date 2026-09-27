package com.example.momentra.data.repository

import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.local.orQueueOffline
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.CreateMomentResultDto
import com.example.momentra.data.api.DuplicateGroupMomentBody
import com.example.momentra.data.api.GroupSetupBlockDto
import com.example.momentra.data.api.LeaveCompanyResultDto
import com.example.momentra.data.api.LeaveMomentBody
import com.example.momentra.data.api.LeaveMomentResultDto
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
        update(
            momentId = momentId,
            expectedVersion = expectedVersion,
            title = title,
        )

    suspend fun update(
        momentId: String,
        expectedVersion: Long,
        title: String? = null,
        startAt: String? = null,
        endAt: String? = null,
        customTypeLabel: String? = null,
        groupSetup: GroupSetupBlockDto? = null,
    ): Result<MomentLifecycleResultDto> {
        val key = UUID.randomUUID().toString()
        val body = UpdateMomentBody(
            title = title,
            startAt = startAt,
            endAt = endAt,
            customTypeLabel = customTypeLabel,
            expectedVersion = expectedVersion,
            groupSetup = groupSetup,
        )
        return runCatching {
            api.updateMoment(
                momentId = momentId,
                idempotencyKey = key,
                body = body,
            ).data
        }.recoverCatching { e -> throw mapError(e) }
            .orQueueOffline(
                path = "v1/moments/$momentId",
                momentId = momentId,
                idempotencyKey = key,
                body = body,
                placeholder = MomentLifecycleResultDto(
                    momentId = momentId,
                    domainCode = "",
                    title = title.orEmpty(),
                    status = "ACTIVE",
                    version = expectedVersion,
                ),
                mapError = { it },
                method = "PATCH",
            )
    }

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

    suspend fun complete(momentId: String, expectedVersion: Long): Result<MomentLifecycleResultDto> =
        runCatching {
            api.completeMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = MomentVersionBody(expectedVersion = expectedVersion),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun delete(momentId: String, expectedVersion: Long): Result<MomentLifecycleResultDto> {
        val key = UUID.randomUUID().toString()
        val body = MomentVersionBody(expectedVersion = expectedVersion)
        return runCatching {
            api.deleteMoment(
                momentId = momentId,
                idempotencyKey = key,
                body = body,
            ).data
        }.recoverCatching { e -> throw mapError(e) }
            .orQueueOffline(
                path = "v1/moments/$momentId/delete",
                momentId = momentId,
                idempotencyKey = key,
                body = body,
                placeholder = MomentLifecycleResultDto(
                    momentId = momentId,
                    domainCode = "",
                    title = "",
                    status = "DELETED",
                    version = expectedVersion,
                ),
                mapError = { it },
            )
    }

    suspend fun leaveGroup(momentId: String, transferUserId: String?): Result<LeaveMomentResultDto> =
        runCatching {
            api.leaveGroupMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = LeaveMomentBody(transferUserId = transferUserId),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun leaveCompany(companyId: String, transferUserId: String?): Result<LeaveCompanyResultDto> =
        runCatching {
            api.leaveCompany(
                companyId = companyId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = LeaveMomentBody(transferUserId = transferUserId),
            ).data
        }.recoverCatching { e -> throw mapError(e) }

    suspend fun duplicateGroup(momentId: String, includeData: Boolean = false): Result<CreateMomentResultDto> =
        runCatching {
            api.duplicateGroupMoment(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = DuplicateGroupMomentBody(includeData = includeData),
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
