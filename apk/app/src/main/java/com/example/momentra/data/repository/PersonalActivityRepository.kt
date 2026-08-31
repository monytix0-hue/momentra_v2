package com.example.momentra.data.repository

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.mapHttpFailure
import com.example.momentra.data.api.ApiResultException
import java.io.IOException
import retrofit2.HttpException

/** Scoped Personal Activity read — used after Expense.Create for read-after-create proof. */
class PersonalActivityRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun listRecent(limit: Int = 20): Result<List<ActivityItemDto>> = runCatching {
        api.getPersonalActivity(limit = limit).data.items
    }.recoverCatching { e ->
        throw when (e) {
            is ApiResultException -> e
            is HttpException -> {
                val body = e.response()?.errorBody()?.string()
                val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
                val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
                mapHttpFailure(e.code(), code, msg)
            }
            is IOException -> ApiResultException.Network(cause = e)
            else -> e
        }
    }
}
