package com.example.momentra.data.repository

import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.ConsentPurposeBody
import com.example.momentra.data.api.ConsentPurposeDto
import com.example.momentra.data.api.DeviceItemDto
import com.example.momentra.data.api.PatchMeBody
import com.example.momentra.data.api.mapHttpFailure
import retrofit2.HttpException
import java.util.UUID

class AccountRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun patchMe(
        displayName: String? = null,
        timezone: String? = null,
        locale: String? = null,
    ): Result<Unit> = runCatching {
        api.patchMe(PatchMeBody(displayName = displayName, timezone = timezone, locale = locale))
        Unit
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun softDeleteMe(): Result<Unit> = runCatching {
        api.deleteMe()
        Unit
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun listDevices(): Result<List<DeviceItemDto>> = runCatching {
        api.listDevices().data.items
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun revokeDevice(deviceId: String): Result<Unit> = runCatching {
        api.revokeDevice(deviceId)
        Unit
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun listConsents(): Result<List<ConsentPurposeDto>> = runCatching {
        api.listConsents().data.purposes
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun grantConsent(purposeCode: String): Result<Unit> = runCatching {
        api.grantConsent(UUID.randomUUID().toString(), ConsentPurposeBody(purposeCode))
        Unit
    }.recoverCatching { e -> throw mapThrowable(e) }

    suspend fun withdrawConsent(purposeCode: String): Result<Unit> = runCatching {
        api.withdrawConsent(UUID.randomUUID().toString(), ConsentPurposeBody(purposeCode))
        Unit
    }.recoverCatching { e -> throw mapThrowable(e) }

    private fun mapThrowable(e: Throwable): Throwable = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            mapHttpFailure(e.code(), code, msg)
        }
        is java.io.IOException -> com.example.momentra.data.api.ApiResultException.Network(cause = e)
        else -> e
    }
}
