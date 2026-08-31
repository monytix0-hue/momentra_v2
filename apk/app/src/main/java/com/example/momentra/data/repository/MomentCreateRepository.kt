package com.example.momentra.data.repository

import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.BusinessSetupBlockDto
import com.example.momentra.data.api.CreateMomentBody
import com.example.momentra.data.api.CreateMomentParticipantBody
import com.example.momentra.data.api.CreateMomentResultDto
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.api.MintGroupInviteBody
import com.example.momentra.data.api.GroupSetupBlockDto
import com.example.momentra.data.api.PersonalSetupBlockDto
import com.example.momentra.data.api.mapHttpFailure
import retrofit2.HttpException
import java.io.IOException
import java.util.TimeZone
import java.util.UUID

class MomentCreateRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun createPersonalMoment(
        systemCode: String,
        momentTypeCode: String,
        title: String,
        preferences: Map<String, Any>? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMomentResultDto> = runCatching {
        api.createMoment(
            idempotencyKey = idempotencyKey,
            body = CreateMomentBody(
                domainCode = "PERSONAL",
                momentTypeCode = momentTypeCode,
                title = title,
                personalSetup = PersonalSetupBlockDto(systemCode = systemCode, preferences = preferences),
            ),
        ).data
    }.recoverCatching { e -> throw mapCreateError(e) }

    suspend fun createBusinessMoment(
        companyId: String,
        familyCode: String,
        momentTypeCode: String,
        title: String,
        preferences: Map<String, Any>? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMomentResultDto> = runCatching {
        api.createMoment(
            idempotencyKey = idempotencyKey,
            body = CreateMomentBody(
                domainCode = "BUSINESS",
                momentTypeCode = momentTypeCode,
                title = title,
                companyId = companyId,
                businessSetup = BusinessSetupBlockDto(familyCode = familyCode, preferences = preferences),
            ),
        ).data
    }.recoverCatching { e -> throw mapCreateError(e) }

    suspend fun createGroupMoment(
        momentTypeCode: String,
        title: String,
        description: String? = null,
        startAt: String? = null,
        endAt: String? = null,
        participants: List<CreateMomentParticipantBody> = emptyList(),
        inviteCode: String? = null,
        customTypeLabel: String? = null,
        groupSetup: GroupSetupBlockDto? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMomentResultDto> = runCatching {
        api.createMoment(
            idempotencyKey = idempotencyKey,
            body = CreateMomentBody(
                domainCode = "GROUP",
                momentTypeCode = momentTypeCode,
                title = title,
                description = description,
                startAt = startAt,
                endAt = endAt,
                timezone = TimeZone.getDefault().id,
                customTypeLabel = customTypeLabel,
                participants = participants.ifEmpty { null },
                inviteCode = inviteCode,
                groupSetup = groupSetup,
            ),
        ).data
    }.recoverCatching { e -> throw mapCreateError(e) }

    suspend fun mintGroupInvite(
        title: String,
        momentTypeCode: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<GroupInviteDto> = runCatching {
        api.mintGroupInvite(
            idempotencyKey = idempotencyKey,
            body = MintGroupInviteBody(title = title, momentTypeCode = momentTypeCode),
        ).data
    }.recoverCatching { e -> throw mapCreateError(e) }

    private fun mapCreateError(e: Throwable): Throwable = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            mapHttpFailure(e.code(), code, msg)
        }
        is IOException -> com.example.momentra.data.api.ApiResultException.Network(cause = e)
        else -> e
    }
}
