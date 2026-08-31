package com.example.momentra.ui.shell.policy

import com.example.momentra.domain.AppContext
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.isActiveStatus

/** Central MomentSwitcher / CompanySwitcher visibility — do not scatter in screens. */
object ShellVisibilityPolicy {
    fun showMomentSwitcher(
        context: AppContext,
        content: ShellContentState,
        destination: BottomDestination,
        activeMomentCount: Int,
        authReady: Boolean,
    ): Boolean {
        if (!authReady) return false
        if (context == AppContext.CIRCLE) return false
        if (destination == BottomDestination.CREATE) return false
        if (content !is ShellContentState.Ready && content !is ShellContentState.Empty) {
            // Empty with active moments still can show between-moments chrome; Ready required for populated.
            if (content !is ShellContentState.Empty) return false
        }
        return activeMomentCount > 0 &&
            (context == AppContext.PERSONAL || context == AppContext.GROUP || context == AppContext.BUSINESS)
    }

    fun showCompanySwitcher(context: AppContext, companies: List<CompanySummary>): Boolean =
        context == AppContext.BUSINESS && companies.isNotEmpty()
}

data class ShellInvariantInput(
    val supportedContexts: List<AppContext>,
    val selectedContext: AppContext,
    val selectedCompanyId: String?,
    val companies: List<CompanySummary>,
    val moments: List<MomentSummary>,
    val selectedMomentId: String?,
    val selectedTabByContext: Map<AppContext, BottomDestination>,
    val currentlySelectedContextDefault: AppContext = AppContext.PERSONAL,
)

data class ShellInvariantResult(
    val selectedContext: AppContext,
    val selectedCompanyId: String?,
    val selectedMomentId: String?,
    val moments: List<MomentSummary>,
    val selectedTabByContext: Map<AppContext, BottomDestination>,
    val healed: Boolean,
)

/**
 * Shell-state self-heal after bootstrap merge.
 * Business order: Context → Company → Moments(company) → Moment. Never Moment→Company.
 */
object ShellStateInvariants {
    fun heal(input: ShellInvariantInput): ShellInvariantResult {
        var healed = false
        val supported = input.supportedContexts.ifEmpty { listOf(AppContext.PERSONAL) }

        var context = input.selectedContext
        if (context !in supported) {
            context = input.currentlySelectedContextDefault.takeIf { it in supported }
                ?: supported.first()
            healed = true
        }

        var companyId = input.selectedCompanyId
        if (context != AppContext.BUSINESS) {
            if (companyId != null) {
                companyId = null
                healed = true
            }
        } else {
            val validIds = input.companies.map { it.companyId }.toSet()
            if (companyId == null || companyId !in validIds) {
                companyId = input.companies.firstOrNull()?.companyId
                healed = true
            }
        }

        val scopedMoments = when (context) {
            AppContext.BUSINESS -> input.moments.filter { m ->
                m.companyId == null || m.companyId == companyId
            }
            AppContext.CIRCLE -> emptyList()
            else -> input.moments
        }

        var momentId = input.selectedMomentId
        val momentOk = scopedMoments.any { it.momentId == momentId }
        if (momentId != null && !momentOk) {
            momentId = null
            healed = true
        }
        if (momentId == null) {
            momentId = scopedMoments.firstOrNull { it.isActiveStatus() }?.momentId
                ?: scopedMoments.firstOrNull()?.momentId
        }
        if (context == AppContext.BUSINESS && momentId != null) {
            val m = scopedMoments.firstOrNull { it.momentId == momentId }
            if (m?.companyId != null && m.companyId != companyId) {
                momentId = scopedMoments.firstOrNull { it.isActiveStatus() }?.momentId
                healed = true
            }
        }

        val tabs = input.selectedTabByContext.filterKeys { it in supported }.toMutableMap()
        for (c in supported) {
            if (c !in tabs) tabs[c] = BottomDestination.PULSE
        }

        return ShellInvariantResult(
            selectedContext = context,
            selectedCompanyId = companyId,
            selectedMomentId = momentId,
            moments = scopedMoments,
            selectedTabByContext = tabs,
            healed = healed,
        )
    }
}

enum class ShellScreenSlot {
    LOADING,
    EMPTY,
    ERROR,
    OFFLINE,
    UNAUTHORIZED,
    DEFERRED,
    PRODUCT,
    LIFE360_GLOBAL,
    PROFILE,
}

object ShellScreenResolver {
    fun resolve(
        content: ShellContentState,
        life360Open: Boolean,
        profileOpen: Boolean,
    ): ShellScreenSlot = when {
        profileOpen -> ShellScreenSlot.PROFILE
        life360Open -> ShellScreenSlot.LIFE360_GLOBAL
        content is ShellContentState.Loading || content is ShellContentState.Idle -> ShellScreenSlot.LOADING
        content is ShellContentState.Offline -> ShellScreenSlot.OFFLINE
        content is ShellContentState.Forbidden -> ShellScreenSlot.UNAUTHORIZED
        content is ShellContentState.Error -> {
            if (content.code == "UNAUTHORIZED") ShellScreenSlot.UNAUTHORIZED else ShellScreenSlot.ERROR
        }
        content is ShellContentState.Deferred -> ShellScreenSlot.DEFERRED
        content is ShellContentState.Empty -> ShellScreenSlot.EMPTY
        content is ShellContentState.Ready -> ShellScreenSlot.PRODUCT
        else -> ShellScreenSlot.LOADING
    }
}
