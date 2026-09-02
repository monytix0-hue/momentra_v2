package com.example.momentra.data.repository

import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.BusinessFacetDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessLifePayloadDto
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.api.CreateBusinessMemoryBody
import com.example.momentra.data.api.CreateBusinessMemoryResultDto
import com.example.momentra.data.api.BusinessTimelineDto
import com.example.momentra.data.api.CreateBusinessApprovalRequestBody
import com.example.momentra.data.api.CreateBusinessApprovalRequestResultDto
import com.example.momentra.data.api.CreateBusinessExpenseBody
import com.example.momentra.data.api.CreateBusinessExpenseResultDto
import com.example.momentra.data.api.CreateBusinessImprovementBody
import com.example.momentra.data.api.CreateBusinessImprovementResultDto
import com.example.momentra.data.api.CreateBusinessInvoiceBody
import com.example.momentra.data.api.CreateBusinessInvoiceResultDto
import com.example.momentra.data.api.CreateBusinessIssueBody
import com.example.momentra.data.api.CreateBusinessIssueResultDto
import com.example.momentra.data.api.CreateIssueEvidenceBody
import com.example.momentra.data.api.CreateIssueEvidenceResultDto
import com.example.momentra.data.api.CreateBusinessRevenueBody
import com.example.momentra.data.api.CreateBusinessRevenueResultDto
import com.example.momentra.data.api.CreateBusinessUpdateBody
import com.example.momentra.data.api.CreateBusinessUpdateResultDto
import com.example.momentra.data.api.CreateBusinessVendorBody
import com.example.momentra.data.api.CreateBusinessVendorResultDto
import com.example.momentra.data.api.CreateSlaCheckBody
import com.example.momentra.data.api.CreateSlaCheckResultDto
import com.example.momentra.data.api.CreateSlaDefinitionBody
import com.example.momentra.data.api.CreateSlaDefinitionResultDto
import com.example.momentra.data.api.CreateVendorContractBody
import com.example.momentra.data.api.CreateVendorContractResultDto
import com.example.momentra.data.api.DecideApprovalBody
import com.example.momentra.data.api.DecideApprovalResultDto
import com.example.momentra.data.api.UpdateBusinessVendorBody
import com.example.momentra.data.api.UpdateBusinessVendorResultDto
import com.example.momentra.data.api.CreateMilestoneBody
import com.example.momentra.data.api.CreateMilestoneResultDto
import com.example.momentra.data.api.CreateRiskBody
import com.example.momentra.data.api.CreateRiskResultDto
import com.example.momentra.data.api.CreateTaxObligationBody
import com.example.momentra.data.api.CreateTaxObligationResultDto
import com.example.momentra.data.api.CreateForecastScenarioBody
import com.example.momentra.data.api.CreateForecastScenarioResultDto
import com.example.momentra.data.api.CreateInvestorUpdateBody
import com.example.momentra.data.api.CreateInvestorUpdateResultDto
import com.example.momentra.data.api.CreateBudgetAlertBody
import com.example.momentra.data.api.CreateBudgetAlertResultDto
import com.example.momentra.data.api.CreateBusinessReviewBody
import com.example.momentra.data.api.CreateBusinessReviewResultDto
import com.example.momentra.data.api.CreateDecisionBody
import com.example.momentra.data.api.CreateDecisionResultDto
import com.example.momentra.data.api.CreateMeetingRecordBody
import com.example.momentra.data.api.CreateMeetingRecordResultDto
import com.example.momentra.data.api.CreateRecognitionBody
import com.example.momentra.data.api.CreateRecognitionResultDto
import com.example.momentra.data.api.CreateRetrospectiveBody
import com.example.momentra.data.api.CreateRetrospectiveResultDto
import com.example.momentra.data.api.CreateActivityLogEntryBody
import com.example.momentra.data.api.CreateActivityLogEntryResultDto
import com.example.momentra.data.api.CapacityDto
import com.example.momentra.data.api.WorkloadDto
import com.example.momentra.data.api.MomDeltasDto
import com.example.momentra.data.api.ProgressSnapshotDto
import com.example.momentra.data.api.RosterDto
import com.example.momentra.data.api.WeeklyReportDto
import com.example.momentra.data.api.ShareLinkResultDto
import com.example.momentra.data.api.VendorListDto
import com.example.momentra.data.api.mapHttpFailure
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class BusinessSliceRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun getPulse(momentId: String): Result<BusinessFacetDto<BusinessPulsePayloadDto>> = runCatching {
        api.getBusinessPulse(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getLife(momentId: String): Result<BusinessFacetDto<BusinessLifePayloadDto>> = runCatching {
        api.getBusinessLife(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getMemory(momentId: String): Result<BusinessFacetDto<BusinessMemoryPayloadDto>> = runCatching {
        api.getBusinessMemory(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getFinance(momentId: String): Result<BusinessFacetDto<BusinessFinancePayloadDto>> = runCatching {
        api.getBusinessFinance(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getActivity(
        momentId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): Result<ActivityPage> = runCatching {
        val env = api.getBusinessActivity(momentId = momentId, cursor = cursor, limit = limit)
        ActivityPage(items = env.data.items, nextCursor = env.nextCursor ?: env.data.nextCursor)
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getMomentTimeline(
        momentId: String,
        limit: Int = 50,
    ): Result<BusinessTimelineDto> = runCatching {
        api.getBusinessMomentTimeline(momentId = momentId, limit = limit).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createMemory(
        momentId: String,
        body: CreateBusinessMemoryBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessMemoryResultDto> = runCatching {
        api.createBusinessMemory(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createExpense(
        momentId: String,
        body: CreateBusinessExpenseBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessExpenseResultDto> = runCatching {
        api.createBusinessExpense(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createRevenue(
        momentId: String,
        body: CreateBusinessRevenueBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessRevenueResultDto> = runCatching {
        api.createBusinessRevenue(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createInvoice(
        momentId: String,
        body: CreateBusinessInvoiceBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessInvoiceResultDto> = runCatching {
        api.createBusinessInvoice(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun decideApproval(
        approvalRequestId: String,
        decision: String,
        reason: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<DecideApprovalResultDto> = runCatching {
        api.decideApproval(
            approvalRequestId = approvalRequestId,
            idempotencyKey = idempotencyKey,
            body = DecideApprovalBody(decision = decision, reason = reason),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createVendor(
        companyId: String,
        body: CreateBusinessVendorBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessVendorResultDto> = runCatching {
        api.createBusinessVendor(companyId = companyId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun updateVendor(
        companyId: String,
        vendorId: String,
        body: UpdateBusinessVendorBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<UpdateBusinessVendorResultDto> = runCatching {
        api.updateBusinessVendor(
            companyId = companyId,
            vendorId = vendorId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createVendorContract(
        companyId: String,
        vendorId: String,
        body: CreateVendorContractBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateVendorContractResultDto> = runCatching {
        api.createVendorContract(
            companyId = companyId,
            vendorId = vendorId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createSlaDefinition(
        companyId: String,
        vendorId: String,
        body: CreateSlaDefinitionBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateSlaDefinitionResultDto> = runCatching {
        api.createSlaDefinition(
            companyId = companyId,
            vendorId = vendorId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createSlaCheck(
        companyId: String,
        slaDefinitionId: String,
        body: CreateSlaCheckBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateSlaCheckResultDto> = runCatching {
        api.createSlaCheck(
            companyId = companyId,
            slaDefinitionId = slaDefinitionId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createIssue(
        momentId: String,
        body: CreateBusinessIssueBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessIssueResultDto> = runCatching {
        api.createBusinessIssue(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createIssueEvidence(
        momentId: String,
        issueId: String,
        body: CreateIssueEvidenceBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateIssueEvidenceResultDto> = runCatching {
        api.createIssueEvidence(momentId = momentId, issueId = issueId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createImprovement(
        momentId: String,
        body: CreateBusinessImprovementBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessImprovementResultDto> = runCatching {
        api.createBusinessImprovement(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createBusinessUpdate(
        momentId: String,
        body: CreateBusinessUpdateBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessUpdateResultDto> = runCatching {
        api.createBusinessUpdate(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createApprovalRequest(
        momentId: String,
        body: CreateBusinessApprovalRequestBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessApprovalRequestResultDto> = runCatching {
        api.createBusinessApprovalRequest(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    // --- Business Deployment Closure wrappers ---

    suspend fun createMilestone(
        momentId: String,
        body: CreateMilestoneBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMilestoneResultDto> = runCatching {
        api.createMilestone(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createRisk(
        momentId: String,
        body: CreateRiskBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateRiskResultDto> = runCatching {
        api.createRisk(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createTaxObligation(
        momentId: String,
        body: CreateTaxObligationBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateTaxObligationResultDto> = runCatching {
        api.createTaxObligation(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createForecastScenario(
        momentId: String,
        body: CreateForecastScenarioBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateForecastScenarioResultDto> = runCatching {
        api.createForecastScenario(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createInvestorUpdate(
        momentId: String,
        body: CreateInvestorUpdateBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateInvestorUpdateResultDto> = runCatching {
        api.createInvestorUpdate(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createBudgetAlert(
        momentId: String,
        body: CreateBudgetAlertBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBudgetAlertResultDto> = runCatching {
        api.createBudgetAlert(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createBusinessReview(
        momentId: String,
        body: CreateBusinessReviewBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateBusinessReviewResultDto> = runCatching {
        api.createBusinessReview(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createDecision(
        momentId: String,
        body: CreateDecisionBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateDecisionResultDto> = runCatching {
        api.createDecision(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createMeetingRecord(
        momentId: String,
        body: CreateMeetingRecordBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMeetingRecordResultDto> = runCatching {
        api.createMeetingRecord(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createRecognition(
        momentId: String,
        body: CreateRecognitionBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateRecognitionResultDto> = runCatching {
        api.createRecognition(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createRetrospective(
        momentId: String,
        body: CreateRetrospectiveBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateRetrospectiveResultDto> = runCatching {
        api.createRetrospective(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createActivityLogEntry(
        momentId: String,
        body: CreateActivityLogEntryBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateActivityLogEntryResultDto> = runCatching {
        api.createActivityLogEntry(momentId = momentId, idempotencyKey = idempotencyKey, body = body).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getCapacity(momentId: String): Result<CapacityDto> = runCatching {
        api.getBusinessCapacity(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getWorkload(momentId: String): Result<WorkloadDto> = runCatching {
        api.getBusinessWorkload(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getMomDeltas(momentId: String): Result<MomDeltasDto> = runCatching {
        api.getBusinessMomDeltas(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getProgressSnapshot(momentId: String): Result<ProgressSnapshotDto> = runCatching {
        api.getBusinessProgressSnapshot(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getRoster(momentId: String): Result<RosterDto> = runCatching {
        api.getBusinessRoster(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getWeeklyReport(momentId: String): Result<WeeklyReportDto> = runCatching {
        api.getBusinessWeeklyReport(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createShareLink(
        momentId: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ShareLinkResultDto> = runCatching {
        api.createShareLink(momentId = momentId, idempotencyKey = idempotencyKey).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listCompanyVendors(
        companyId: String,
    ): Result<VendorListDto> = runCatching {
        api.listCompanyVendors(companyId).data
    }.recoverCatching { e -> throw mapError(e) }

    private fun mapError(e: Throwable): Throwable = when (e) {
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
