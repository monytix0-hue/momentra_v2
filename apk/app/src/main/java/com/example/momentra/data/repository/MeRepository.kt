package com.example.momentra.data.repository

import android.content.Context
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.BootstrapMomentDto
import com.example.momentra.data.api.CompanyItemDto
import com.example.momentra.data.api.MeBootstrapDto
import com.example.momentra.data.api.mapHttpFailure
import com.example.momentra.data.local.BootstrapCache
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellBootstrap
import com.example.momentra.domain.ShellIdentity
import retrofit2.HttpException
import java.io.IOException

interface MeGateway {
    suspend fun getMe(): Result<ShellIdentity>
    /** Fresh bootstrap from network; updates SWR cache. */
    suspend fun getBootstrap(): Result<ShellBootstrap>
    /** Last cached bootstrap for userId, if any. */
    fun cachedBootstrap(userId: String): ShellBootstrap?
    fun isBootstrapCacheFresh(userId: String, maxAgeMs: Long = 30_000L): Boolean
    fun clearBootstrapCache(userId: String?)
    suspend fun listCompanies(): Result<List<CompanySummary>>
    suspend fun listGroupMomentCount(): Result<Int>
    suspend fun listPersonalMoments(limit: Int = 20): Result<List<MomentSummary>>
    suspend fun listGroupMoments(limit: Int = 20): Result<List<MomentSummary>>
    suspend fun listBusinessMoments(limit: Int = 20): Result<List<MomentSummary>>
    suspend fun hasLife360(): Result<Boolean>
}

class MeRepository(
    private val api: ApiService = ApiClient.apiService,
    private val bootstrapCache: BootstrapCache? = null,
) : MeGateway {

    constructor(context: Context) : this(
        api = ApiClient.apiService,
        bootstrapCache = BootstrapCache(context),
    )

    override suspend fun getMe(): Result<ShellIdentity> = getBootstrap().map { it.identity }

    override suspend fun getBootstrap(): Result<ShellBootstrap> = runCatching {
        val dto = api.getMe().data
        bootstrapCache?.save(dto.userId, dto)
        dto.toShellBootstrap()
    }.recoverCatching { e -> throw mapThrowable(e) }

    override fun cachedBootstrap(userId: String): ShellBootstrap? =
        bootstrapCache?.load(userId)?.toShellBootstrap()

    override fun isBootstrapCacheFresh(userId: String, maxAgeMs: Long): Boolean =
        bootstrapCache?.isFresh(userId, maxAgeMs) == true

    override fun clearBootstrapCache(userId: String?) {
        bootstrapCache?.clear(userId)
    }

    override suspend fun listCompanies(): Result<List<CompanySummary>> = runCatching {
        api.listCompanies().data.items.map { it.toSummary() }
    }.recoverCatching { e -> throw mapThrowable(e) }

    override suspend fun listGroupMomentCount(): Result<Int> = runCatching {
        api.listGroupMoments(limit = 1).data.items.size
    }.recoverCatching { e -> throw mapThrowable(e) }

    override suspend fun listPersonalMoments(limit: Int): Result<List<MomentSummary>> = runCatching {
        api.listPersonalMoments(limit = limit).data.items.map {
            MomentSummary(it.momentId, it.title, it.status, it.momentTypeCode)
        }
    }.recoverCatching { e -> throw mapThrowable(e) }

    override suspend fun listGroupMoments(limit: Int): Result<List<MomentSummary>> = runCatching {
        api.listGroupMoments(limit = limit).data.items.map {
            MomentSummary(it.momentId, it.title, it.status)
        }
    }.recoverCatching { e -> throw mapThrowable(e) }

    override suspend fun listBusinessMoments(limit: Int): Result<List<MomentSummary>> = runCatching {
        api.listBusinessMoments(limit = limit).data.items.map {
            MomentSummary(it.momentId, it.title, it.status)
        }
    }.recoverCatching { e -> throw mapThrowable(e) }

    /**
     * S5: Life360 entry is always available as Coming Soon — do not call GET /v1/life360.
     * Projection/API remain for a later stage.
     */
    override suspend fun hasLife360(): Result<Boolean> = Result.success(true)

    private fun MeBootstrapDto.toShellBootstrap(): ShellBootstrap {
        val moments = activeMoments
        return ShellBootstrap(
            identity = ShellIdentity(
                userId = userId,
                displayName = displayName,
                email = email,
                firebaseUid = firebaseUid,
            ),
            supportedContexts = (supportedContexts ?: listOf("PERSONAL", "CIRCLE")).mapNotNull { parseContext(it) },
            currentlySelectedContext = parseContext(currentlySelectedContext) ?: AppContext.PERSONAL,
            personalMoments = moments?.personal.orEmpty().map { it.toSummary() },
            groupMoments = moments?.group.orEmpty().map { it.toSummary() },
            businessMoments = moments?.business.orEmpty().map { it.toSummary() },
            companies = companies.orEmpty().map { it.toSummary() },
            selectedCompany = selectedCompany?.toSummary() ?: companies?.firstOrNull()?.toSummary(),
            capabilities = capabilities.orEmpty(),
            roles = roles.orEmpty(),
            preferencesTimezone = preferences?.timezone ?: timezone ?: "UTC",
            preferencesLocale = preferences?.locale ?: locale,
            featureFlags = featureFlags.orEmpty().mapValues { (_, v) -> v?.toString().orEmpty() },
        )
    }

    private fun parseContext(raw: String?): AppContext? = when (raw?.uppercase()) {
        "PERSONAL" -> AppContext.PERSONAL
        "GROUP" -> AppContext.GROUP
        "BUSINESS" -> AppContext.BUSINESS
        "CIRCLE" -> AppContext.CIRCLE
        else -> null
    }

    private fun BootstrapMomentDto.toSummary() =
        MomentSummary(momentId, title, status, momentTypeCode, companyId)

    private fun CompanyItemDto.toSummary() = CompanySummary(
        companyId = companyId,
        displayName = displayName,
    )

    private fun mapThrowable(e: Throwable): Throwable = when (e) {
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
