package com.example.momentra.ui.shell.business

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.empty.BusinessSetupKind

/** B01–B03 Business active tokens — Team Ops / Runway / Operations. */
data class BusinessActiveTheme(
    val bg: Color,
    val accent: Color,
    val accentSolid: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val card: Color,
    val border: Color,
    val typeLabel: String,
    val pulseTitle: String,
    val momentsTitle: String,
    val lifeTitle: String,
    val memoryTitle: String,
    val hubSubtitle: String,
    val hubHeroTitle: String,
    val hubHeroDetail: String,
    val hubHeroRes: Int,
    val filterChips: List<String>,
    val heroGradient: Brush,
) {
    companion object {
        val TeamOperations = BusinessActiveTheme(
            bg = Color(0xFF0C0F15),
            accent = Color(0xFF10B981),
            accentSolid = Color(0xFF059669),
            accentLight = Color(0xFF34D399),
            accentSoft = Color(0x3310B981),
            text = Color(0xFFE5E0EE),
            secondary = Color(0xFF94A3B8),
            muted = Color(0xFF64748B),
            card = Color(0xFF161B26),
            border = Color(0xFF1E293B),
            typeLabel = "Team Operations",
            pulseTitle = "Team Pulse",
            momentsTitle = "Team Moments",
            lifeTitle = "Team Life",
            memoryTitle = "Team Memory",
            hubSubtitle = "Bring your team operations to life",
            hubHeroTitle = "Bring your team operations to life",
            hubHeroDetail = "Add people, plans, decisions, and updates",
            hubHeroRes = com.example.momentra.R.drawable.team_ops_hub_hero,
            filterChips = listOf("Team Sync", "Sprint Review"),
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
        )

        val BusinessRunway = BusinessActiveTheme(
            bg = Color(0xFF0C0F15),
            accent = Color(0xFFF59E0B),
            accentSolid = Color(0xFFD97706),
            accentLight = Color(0xFFFBBF24),
            accentSoft = Color(0x33F59E0B),
            text = Color(0xFFE5E0EE),
            secondary = Color(0xFF94A3B8),
            muted = Color(0xFF64748B),
            card = Color(0xFF161B26),
            border = Color(0xFF1E293B),
            typeLabel = "Business Runway",
            pulseTitle = "Runway Pulse",
            momentsTitle = "Runway Moments",
            lifeTitle = "Runway Life",
            memoryTitle = "Runway Memory",
            hubSubtitle = "Bring your finances to life",
            hubHeroTitle = "Bring your finances to life",
            hubHeroDetail = "Track revenue, expenses, taxes and forecasts",
            hubHeroRes = com.example.momentra.R.drawable.runway_hub_hero,
            filterChips = listOf("Revenue", "Expenses"),
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
        )

        val BusinessOperations = BusinessActiveTheme(
            bg = Color(0xFF0C0F15),
            accent = Color(0xFF818CF8),
            accentSolid = Color(0xFF6366F1),
            accentLight = Color(0xFFA5B4FC),
            accentSoft = Color(0x33818CF8),
            text = Color(0xFFE5E0EE),
            secondary = Color(0xFF94A3B8),
            muted = Color(0xFF64748B),
            card = Color(0xFF161B26),
            border = Color(0xFF1E293B),
            typeLabel = "Business Operations",
            pulseTitle = "Ops Pulse",
            momentsTitle = "Ops Moments",
            lifeTitle = "Ops Life",
            memoryTitle = "Ops Memory",
            hubSubtitle = "Bring your operations to life",
            hubHeroTitle = "Bring your operations to life",
            hubHeroDetail = "Add expenses, vendors, approvals and updates",
            hubHeroRes = com.example.momentra.R.drawable.ops_hub_hero,
            filterChips = listOf("Budget Ops", "Vendor Mgmt"),
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
        )

        fun forKind(kind: BusinessSetupKind): BusinessActiveTheme = when (kind) {
            BusinessSetupKind.TEAM_OPERATIONS -> TeamOperations
            BusinessSetupKind.BUSINESS_RUNWAY -> BusinessRunway
            BusinessSetupKind.BUSINESS_OPERATIONS -> BusinessOperations
        }

        fun forTypeCode(momentTypeCode: String?): BusinessActiveTheme {
            val code = momentTypeCode?.uppercase().orEmpty()
            return when {
                code.contains("RUNWAY") -> BusinessRunway
                code.contains("OPERATIONS") && !code.contains("TEAM") -> BusinessOperations
                else -> TeamOperations
            }
        }
    }
}

enum class BusinessQuickAddKind {
    TEAM_UPDATE,
    DECISION,
    BLOCKER,
    MEETING,
    RECOGNITION,
    APPROVAL,
    MILESTONE,
    RETROSPECTIVE,
    RISK_FLAG,
    ACTIVITY_LOG,
    POLL,
    MEMORY,
    REVENUE,
    EXPENSE,
    TAX_ENTRY,
    INVESTOR_UPDATE,
    BUDGET_ALERT,
    FORECAST_UPDATE,
    INVOICE,
    GENERAL_UPDATE,
    SPEND_ENTRY,
    UPDATE_VENDOR,
    REQUEST_APPROVAL,
    REPORT_ISSUE,
    LOG_IMPROVEMENT,
    BUDGET_REVIEW,
    SLA_CHECK,
}

fun BusinessQuickAddKind.isLive(): Boolean = when (this) {
    BusinessQuickAddKind.EXPENSE,
    BusinessQuickAddKind.SPEND_ENTRY,
    BusinessQuickAddKind.REVENUE,
    BusinessQuickAddKind.INVOICE,
    BusinessQuickAddKind.TAX_ENTRY,
    BusinessQuickAddKind.INVESTOR_UPDATE,
    BusinessQuickAddKind.BUDGET_ALERT,
    BusinessQuickAddKind.FORECAST_UPDATE,
    BusinessQuickAddKind.POLL,
    BusinessQuickAddKind.MEMORY,
    BusinessQuickAddKind.TEAM_UPDATE,
    BusinessQuickAddKind.DECISION,
    BusinessQuickAddKind.BLOCKER,
    BusinessQuickAddKind.MEETING,
    BusinessQuickAddKind.RECOGNITION,
    BusinessQuickAddKind.APPROVAL,
    BusinessQuickAddKind.MILESTONE,
    BusinessQuickAddKind.RETROSPECTIVE,
    BusinessQuickAddKind.RISK_FLAG,
    BusinessQuickAddKind.ACTIVITY_LOG,
    BusinessQuickAddKind.GENERAL_UPDATE,
    BusinessQuickAddKind.UPDATE_VENDOR,
    BusinessQuickAddKind.REQUEST_APPROVAL,
    BusinessQuickAddKind.REPORT_ISSUE,
    BusinessQuickAddKind.LOG_IMPROVEMENT,
    BusinessQuickAddKind.BUDGET_REVIEW,
    BusinessQuickAddKind.SLA_CHECK,
    -> true
    else -> false
}

fun BusinessQuickAddKind.label(): String = when (this) {
    BusinessQuickAddKind.TEAM_UPDATE -> "Team Update"
    BusinessQuickAddKind.DECISION -> "Decision"
    BusinessQuickAddKind.BLOCKER -> "Blocker"
    BusinessQuickAddKind.MEETING -> "Meeting"
    BusinessQuickAddKind.RECOGNITION -> "Recognition"
    BusinessQuickAddKind.APPROVAL -> "Approval"
    BusinessQuickAddKind.MILESTONE -> "Milestone"
    BusinessQuickAddKind.RETROSPECTIVE -> "Retrospective"
    BusinessQuickAddKind.RISK_FLAG -> "Risk Flag"
    BusinessQuickAddKind.ACTIVITY_LOG -> "Activity Log"
    BusinessQuickAddKind.POLL -> "Poll"
    BusinessQuickAddKind.MEMORY -> "Memory"
    BusinessQuickAddKind.REVENUE -> "Log Revenue"
    BusinessQuickAddKind.EXPENSE -> "Log Expense"
    BusinessQuickAddKind.TAX_ENTRY -> "Tax Entry"
    BusinessQuickAddKind.INVESTOR_UPDATE -> "Investor Update"
    BusinessQuickAddKind.BUDGET_ALERT -> "Budget Alert"
    BusinessQuickAddKind.FORECAST_UPDATE -> "Forecast Update"
    BusinessQuickAddKind.INVOICE -> "Invoice Track"
    BusinessQuickAddKind.GENERAL_UPDATE -> "General Update"
    BusinessQuickAddKind.SPEND_ENTRY -> "Log Spend Entry"
    BusinessQuickAddKind.UPDATE_VENDOR -> "Update Vendor"
    BusinessQuickAddKind.REQUEST_APPROVAL -> "Request Approval"
    BusinessQuickAddKind.REPORT_ISSUE -> "Report Issue"
    BusinessQuickAddKind.LOG_IMPROVEMENT -> "Log Improvement"
    BusinessQuickAddKind.BUDGET_REVIEW -> "Budget Review"
    BusinessQuickAddKind.SLA_CHECK -> "SLA Check"
}

fun BusinessQuickAddKind.subtitle(): String = when (this) {
    BusinessQuickAddKind.TEAM_UPDATE -> "Share progress"
    BusinessQuickAddKind.DECISION -> "Log choices"
    BusinessQuickAddKind.BLOCKER -> "Flag issues"
    BusinessQuickAddKind.MEETING -> "Capture notes"
    BusinessQuickAddKind.RECOGNITION -> "Celebrate wins"
    BusinessQuickAddKind.APPROVAL -> "Route requests"
    BusinessQuickAddKind.MILESTONE -> "Mark progress"
    BusinessQuickAddKind.RETROSPECTIVE -> "Review & learn"
    BusinessQuickAddKind.RISK_FLAG -> "Raise concerns"
    BusinessQuickAddKind.ACTIVITY_LOG -> "Track actions"
    BusinessQuickAddKind.POLL -> "Gather input"
    BusinessQuickAddKind.MEMORY -> "Save learnings"
    BusinessQuickAddKind.REVENUE -> "Track income"
    BusinessQuickAddKind.EXPENSE -> "Record spend"
    BusinessQuickAddKind.TAX_ENTRY -> "File taxes"
    BusinessQuickAddKind.INVESTOR_UPDATE -> "Share metrics"
    BusinessQuickAddKind.BUDGET_ALERT -> "Flag overrun"
    BusinessQuickAddKind.FORECAST_UPDATE -> "Update projections"
    BusinessQuickAddKind.INVOICE -> "Monitor payments"
    BusinessQuickAddKind.GENERAL_UPDATE -> "Share updates"
    BusinessQuickAddKind.SPEND_ENTRY -> "Record expenses"
    BusinessQuickAddKind.UPDATE_VENDOR -> "Update suppliers"
    BusinessQuickAddKind.REQUEST_APPROVAL -> "Request sign-off"
    BusinessQuickAddKind.REPORT_ISSUE -> "Flag a problem"
    BusinessQuickAddKind.LOG_IMPROVEMENT -> "Log optimization"
    BusinessQuickAddKind.BUDGET_REVIEW -> "Check budgets"
    BusinessQuickAddKind.SLA_CHECK -> "Monitor SLAs"
}

fun BusinessQuickAddKind.emoji(): String = when (this) {
    BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.GENERAL_UPDATE -> "📢"
    BusinessQuickAddKind.DECISION -> "🚩"
    BusinessQuickAddKind.BLOCKER, BusinessQuickAddKind.RISK_FLAG, BusinessQuickAddKind.REPORT_ISSUE -> "🛡"
    BusinessQuickAddKind.MEETING, BusinessQuickAddKind.BUDGET_REVIEW -> "📅"
    BusinessQuickAddKind.RECOGNITION -> "⭐"
    BusinessQuickAddKind.APPROVAL, BusinessQuickAddKind.REQUEST_APPROVAL -> "🏪"
    BusinessQuickAddKind.MILESTONE -> "🏁"
    BusinessQuickAddKind.RETROSPECTIVE -> "⚡"
    BusinessQuickAddKind.ACTIVITY_LOG -> "📈"
    BusinessQuickAddKind.POLL -> "📊"
    BusinessQuickAddKind.MEMORY -> "📦"
    BusinessQuickAddKind.REVENUE -> "💰"
    BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.SPEND_ENTRY -> "💳"
    BusinessQuickAddKind.TAX_ENTRY -> "🧾"
    BusinessQuickAddKind.INVESTOR_UPDATE -> "📣"
    BusinessQuickAddKind.BUDGET_ALERT -> "🚨"
    BusinessQuickAddKind.FORECAST_UPDATE -> "📉"
    BusinessQuickAddKind.INVOICE -> "📄"
    BusinessQuickAddKind.UPDATE_VENDOR -> "🏷"
    BusinessQuickAddKind.LOG_IMPROVEMENT -> "✨"
    BusinessQuickAddKind.SLA_CHECK -> "⏱"
}

fun BusinessQuickAddKind.stripeColor(): Color = when (this) {
    BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.GENERAL_UPDATE,
    BusinessQuickAddKind.APPROVAL, BusinessQuickAddKind.ACTIVITY_LOG, BusinessQuickAddKind.SPEND_ENTRY,
    BusinessQuickAddKind.BUDGET_REVIEW,
    -> Color(0xFF818CF8)
    BusinessQuickAddKind.DECISION, BusinessQuickAddKind.INVESTOR_UPDATE,
    BusinessQuickAddKind.UPDATE_VENDOR, BusinessQuickAddKind.MEMORY,
    -> Color(0xFFA78BFA)
    BusinessQuickAddKind.BLOCKER, BusinessQuickAddKind.RISK_FLAG, BusinessQuickAddKind.BUDGET_ALERT,
    BusinessQuickAddKind.REPORT_ISSUE,
    -> Color(0xFFEF4444)
    BusinessQuickAddKind.MEETING, BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE,
    BusinessQuickAddKind.FORECAST_UPDATE, BusinessQuickAddKind.REQUEST_APPROVAL,
    BusinessQuickAddKind.RETROSPECTIVE,
    -> Color(0xFFF59E0B)
    BusinessQuickAddKind.RECOGNITION, BusinessQuickAddKind.TAX_ENTRY, BusinessQuickAddKind.LOG_IMPROVEMENT,
    BusinessQuickAddKind.POLL,
    -> Color(0xFF10B981)
    BusinessQuickAddKind.MILESTONE, BusinessQuickAddKind.INVOICE, BusinessQuickAddKind.SLA_CHECK -> Color(0xFF14B8A6)
}

/** Figma 649:26162 Team Ops Action Center tile icons (MCP exports). */
fun BusinessQuickAddKind.teamOpsHubIconRes(): Int? = when (this) {
    BusinessQuickAddKind.TEAM_UPDATE -> com.example.momentra.R.drawable.ic_teamops_qa_update
    BusinessQuickAddKind.DECISION -> com.example.momentra.R.drawable.ic_teamops_qa_decision
    BusinessQuickAddKind.BLOCKER -> com.example.momentra.R.drawable.ic_teamops_qa_blocker
    BusinessQuickAddKind.MEETING -> com.example.momentra.R.drawable.ic_teamops_qa_meeting
    BusinessQuickAddKind.RECOGNITION -> com.example.momentra.R.drawable.ic_teamops_qa_recognition
    BusinessQuickAddKind.APPROVAL -> com.example.momentra.R.drawable.ic_teamops_qa_approval
    BusinessQuickAddKind.MILESTONE -> com.example.momentra.R.drawable.ic_teamops_qa_milestone
    BusinessQuickAddKind.RETROSPECTIVE -> com.example.momentra.R.drawable.ic_teamops_qa_retro
    BusinessQuickAddKind.RISK_FLAG -> com.example.momentra.R.drawable.ic_teamops_qa_risk
    BusinessQuickAddKind.ACTIVITY_LOG -> com.example.momentra.R.drawable.ic_teamops_qa_activity
    BusinessQuickAddKind.POLL -> com.example.momentra.R.drawable.ic_teamops_qa_poll
    BusinessQuickAddKind.MEMORY -> com.example.momentra.R.drawable.ic_teamops_qa_memory
    else -> null
}

fun businessHubTiles(theme: BusinessActiveTheme): List<BusinessQuickAddKind> = when (theme.typeLabel) {
    "Business Runway" -> listOf(
        BusinessQuickAddKind.REVENUE,
        BusinessQuickAddKind.EXPENSE,
        BusinessQuickAddKind.TAX_ENTRY,
        BusinessQuickAddKind.INVESTOR_UPDATE,
        BusinessQuickAddKind.BUDGET_ALERT,
        BusinessQuickAddKind.FORECAST_UPDATE,
        BusinessQuickAddKind.INVOICE,
        BusinessQuickAddKind.GENERAL_UPDATE,
        BusinessQuickAddKind.MEMORY,
    )
    "Business Operations" -> listOf(
        BusinessQuickAddKind.SPEND_ENTRY,
        BusinessQuickAddKind.UPDATE_VENDOR,
        BusinessQuickAddKind.REQUEST_APPROVAL,
        BusinessQuickAddKind.REPORT_ISSUE,
        BusinessQuickAddKind.LOG_IMPROVEMENT,
        BusinessQuickAddKind.BUDGET_REVIEW,
        BusinessQuickAddKind.SLA_CHECK,
        BusinessQuickAddKind.GENERAL_UPDATE,
        BusinessQuickAddKind.MEMORY,
    )
    else -> listOf(
        BusinessQuickAddKind.TEAM_UPDATE,
        BusinessQuickAddKind.DECISION,
        BusinessQuickAddKind.BLOCKER,
        BusinessQuickAddKind.MEETING,
        BusinessQuickAddKind.RECOGNITION,
        BusinessQuickAddKind.APPROVAL,
        BusinessQuickAddKind.MILESTONE,
        BusinessQuickAddKind.RETROSPECTIVE,
        BusinessQuickAddKind.RISK_FLAG,
        BusinessQuickAddKind.ACTIVITY_LOG,
        BusinessQuickAddKind.POLL,
        BusinessQuickAddKind.MEMORY,
    )
}
