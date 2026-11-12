package com.example.momentra.ui.shell.empty.business

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.shell.business.shared.OperatingInviteRole
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSubtype
import com.example.momentra.ui.shell.business.shared.businessInviteMembership
import com.example.momentra.ui.shell.business.shared.businessInviteRoleLabel
import com.example.momentra.ui.shell.business.shared.inviteRoleAccess
import java.util.UUID

/**
 * `MKT-SETUP-v6` / `MKT-OWN-SETUP-v6` — one scrolling document with Client / Own kind tiles.
 * Structure mirrors [SetupDocumentSpec]; accents and Client-only fields differ by subtype.
 */
internal object MarketingSetupDocumentSpec {
    const val CLIENT = "CLIENT_CAMPAIGN"
    const val OWN = "OWN_BUSINESS_CAMPAIGN"
    const val DEFAULT_INVITE_ROLE = "OPERATIONS_LEAD"

    fun isOwn(subtypeCode: String): Boolean =
        subtypeCode.equals(OWN, ignoreCase = true)

    fun isClient(subtypeCode: String): Boolean =
        subtypeCode.equals(CLIENT, ignoreCase = true)

    fun kindLabel(subtypeCode: String): String =
        if (isOwn(subtypeCode)) "Own Business Campaign" else "Client Campaign"

    fun partyLabel(subtypeCode: String): String =
        if (isOwn(subtypeCode)) "Brand" else "Client"

    fun sections(subtypeCode: String): List<SetupSectionSpec> {
        val own = isOwn(subtypeCode)
        return listOf(
            SetupSectionSpec(
                "01",
                "Business Basics",
                if (own) {
                    "Name the campaign and what you promote. Mode is fixed for this Moment."
                } else {
                    "Name the campaign and the client it is run for. Mode is fixed for this Moment."
                },
            ),
            SetupSectionSpec(
                "02",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
            ),
            // BS-06 — marketing uses Campaign Resources & Providers, not inventory catalogue.
            SetupSectionSpec(
                "03",
                "Campaign Resources & Providers",
                if (own) {
                    "Resources, providers, prices and tax in one place. No warehouse or SKU setup."
                } else {
                    "Campaign resources, providers, prices and tax. No warehouse or SKU setup."
                },
            ),
            SetupSectionSpec(
                "04",
                "Channels & Monthly Plan",
                if (own) {
                    "Channels, own marketing budget and KPI targets."
                } else {
                    "Channels, client commercials, budget and KPI targets."
                },
            ),
            SetupSectionSpec("05", "GST & Tax Details", "Set up GST once to make buying and selling easier."),
            SetupSectionSpec("06", "Review your setup", ""),
        )
    }

    fun subtype(subtypeCode: String): OperatingSubtype =
        OperatingMomentCatalog.subtypes(BusinessSetupKind.MARKETING_CAMPAIGNS)
            .firstOrNull { it.code.equals(subtypeCode, ignoreCase = true) }
            ?: OperatingMomentCatalog.subtypes(BusinessSetupKind.MARKETING_CAMPAIGNS).first()

    fun channels(subtypeCode: String): List<String> = subtype(subtypeCode).channels

    fun inviteRoles(subtypeCode: String): List<OperatingInviteRole> = subtype(subtypeCode).inviteRoles

    fun roleLabel(code: String): String = businessInviteRoleLabel(code)

    fun roleCode(label: String): String =
        inviteRoles(CLIENT).firstOrNull { it.label.equals(label, ignoreCase = true) }?.code
            ?: DEFAULT_INVITE_ROLE

    fun membershipType(roleCode: String): String = businessInviteMembership(roleCode)

    fun inviteAccessNote(roleCode: String): String = inviteRoleAccess(roleCode)

    fun metricLabel(code: String): String = when (code.trim().uppercase()) {
        "LEADS" -> "Leads"
        "QUALIFIED_LEADS" -> "Qualified Leads"
        "ENQUIRIES" -> "Enquiries"
        "REACH" -> "Reach"
        "IMPRESSIONS" -> "Impressions"
        "ENGAGEMENTS", "ENGAGEMENT" -> "Engagements"
        "ENGAGEMENT_RATE" -> "Engagement Rate"
        "CLICKS" -> "Clicks"
        "CTR" -> "CTR"
        "WEBSITE_VISITS" -> "Website Visits"
        "CONVERSIONS" -> "Conversions"
        "CONVERSION_RATE" -> "Conversion Rate"
        "ORDERS" -> "Orders"
        "ATTRIBUTED_SALES", "REVENUE" -> "Attributed Sales"
        "ROAS" -> "ROAS"
        "REGISTRATIONS" -> "Registrations"
        "ATTENDANCE" -> "Attendance"
        "CUSTOM", "CUSTOM_COUNT" -> "Custom"
        "CLIENT_BILLING" -> "Client Billing"
        else -> code.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    fun metricCode(label: String): String {
        val compact = label.trim().uppercase().replace(' ', '_')
        return when (compact) {
            "ATTRIBUTED_SALES", "REVENUE" -> "ATTRIBUTED_SALES"
            "ENGAGEMENTS", "ENGAGEMENT" -> "ENGAGEMENTS"
            "CUSTOM", "CUSTOM_COUNT", "COUNT" -> "CUSTOM"
            else -> compact.ifBlank { "LEADS" }
        }
    }

    fun metricUnit(code: String): String = when (code.trim().uppercase()) {
        "ATTRIBUTED_SALES", "REVENUE", "CLIENT_BILLING" -> "₹"
        "ENGAGEMENT_RATE", "CTR", "CONVERSION_RATE" -> "%"
        "ROAS" -> "Ratio"
        else -> "Count"
    }

    fun currencyLabels(): List<String> = listOf("INR · ₹", "USD · $", "EUR · €", "GBP · £")

    fun currencyCode(label: String): String = label.substringBefore('·').trim().ifBlank { "INR" }

    val gstTrackingLabels: List<String> = listOf("Yes, track GST", "No, skip for now")

    fun gstTrackingCode(label: String): String =
        if (label.startsWith("Yes", ignoreCase = true)) "YES" else "NO"

    val gstRegistrationLabels: List<String> =
        listOf("Regular", "Composition", "Unregistered")

    fun gstRegistrationCode(label: String): String = label.trim().uppercase()

    val gstPricingLabels: List<String> =
        listOf("Prices include GST", "GST is added separately")

    fun gstPricingCode(label: String): String =
        if (label.contains("include", ignoreCase = true)) "INCLUSIVE" else "EXCLUSIVE"

    fun campaignPurposes(subtypeCode: String): List<String> =
        if (isOwn(subtypeCode)) {
            listOf("Brand awareness", "Leads", "Sales", "Launch", "Retention", "Other")
        } else {
            listOf("Awareness", "Leads", "Conversions", "Retargeting", "Launch", "Always-on", "Other")
        }

    fun activateLabel(status: String): String =
        if (status.equals("ACTIVE", ignoreCase = true)) "Save changes" else "Activate"

    fun activatingLabel(status: String): String =
        if (status.equals("ACTIVE", ignoreCase = true)) "Saving…" else "Activating…"

    fun isPromotable(status: String): Boolean = status.equals("DRAFT", ignoreCase = true)

    fun isDiscardable(status: String): Boolean = status.equals("DRAFT", ignoreCase = true)

    fun statusPillLabel(status: String): String =
        when (status.uppercase()) {
            "ACTIVE" -> "Live"
            "DRAFT" -> "Setup in progress"
            else -> status
        }

    fun setupDateLabel(iso: String): String {
        if (iso.length < 10) return ""
        val parts = iso.take(10).split("-")
        if (parts.size != 3) return iso
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
        val m = parts[1].toIntOrNull() ?: return iso
        return "${parts[2].toIntOrNull() ?: parts[2]} ${months.getOrElse(m - 1) { parts[1] }} ${parts[0]}"
    }

    fun initials(name: String): String {
        val parts = name.trim().split(" ", "\t").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    fun participants(ownerName: String, invitees: List<SetupInvitee>): List<Map<String, String>> {
        val rows = mutableListOf(
            mapOf(
                "displayName" to ownerName.trim().ifBlank { "Owner" },
                "role" to "OWNER_ADMIN",
            ),
        )
        invitees.forEach { invitee ->
            rows += mapOf(
                "displayName" to invitee.name.trim(),
                "role" to invitee.roleCode,
            )
        }
        return rows
    }

    fun emittedPreferenceKeys(): Set<String> = setOf(
        "subtype",
        "identityName",
        "partyName",
        "description",
        "currency",
        "startDate",
        "endDate",
        "location",
        "channels",
        "monthlyBudget",
        "monthlyTarget",
        "targetMetric",
        "participants",
        "gstin",
        "gstState",
        "gstStateCode",
        "gstRegistration",
        "gstTracking",
        "salePriceIncludesGst",
        "purchasePriceIncludesGst",
        "gstEffectiveFrom",
        "clientFee",
        "clientApprovedMediaBudget",
        "agencyOperatingBudget",
        "offering",
        "campaignPurpose",
        "creativeApprovalRequired",
        "reportingCadence",
        "campaignStatus",
    )

    fun preferences(
        subtypeCode: String,
        identityName: String,
        partyName: String,
        description: String = "",
        currency: String = "INR",
        startDate: String = "",
        endDate: String = "",
        location: String = "",
        channels: List<String> = emptyList(),
        monthlyBudget: String = "",
        monthlyTarget: String = "",
        targetMetric: String = "LEADS",
        participants: List<Map<String, String>> = emptyList(),
        gstin: String = "",
        gstState: String = "",
        gstRegistration: String = "",
        gstTracking: String = "",
        salePriceIncludesGst: String = "",
        purchasePriceIncludesGst: String = salePriceIncludesGst,
        gstEffectiveFrom: String = "",
        clientFee: String = "",
        clientApprovedMediaBudget: String = "",
        agencyOperatingBudget: String = "",
        offering: String = "",
        campaignPurpose: String = "",
        creativeApprovalRequired: String = "",
        reportingCadence: String = "",
    ): Map<String, Any> {
        val own = isOwn(subtypeCode)
        return mapOf(
            "subtype" to subtypeCode,
            "identityName" to identityName.trim(),
            "partyName" to partyName.trim(),
            "description" to description.trim(),
            "currency" to currency.trim().ifBlank { "INR" },
            "startDate" to startDate.trim(),
            "endDate" to endDate.trim(),
            "location" to location.trim(),
            "channels" to channels,
            "monthlyBudget" to monthlyBudget.trim(),
            "monthlyTarget" to monthlyTarget.trim(),
            "targetMetric" to targetMetric.trim().uppercase().ifBlank { "LEADS" },
            "participants" to participants,
            "gstin" to gstin.trim().uppercase(),
            "gstState" to IndiaStateCatalog.canonical(gstState).ifBlank { gstState.trim() },
            "gstStateCode" to IndiaStateCatalog.codeOf(gstState),
            "gstRegistration" to gstRegistration.trim().uppercase(),
            "gstTracking" to gstTracking.trim().uppercase(),
            "salePriceIncludesGst" to salePriceIncludesGst.trim().uppercase(),
            "purchasePriceIncludesGst" to purchasePriceIncludesGst.trim().uppercase(),
            "gstEffectiveFrom" to gstEffectiveFrom.trim(),
            "clientFee" to if (own) "" else clientFee.trim(),
            "clientApprovedMediaBudget" to if (own) "" else clientApprovedMediaBudget.trim(),
            "agencyOperatingBudget" to if (own) "" else agencyOperatingBudget.trim(),
            "offering" to offering.trim(),
            "campaignPurpose" to campaignPurpose.trim(),
            "creativeApprovalRequired" to if (creativeApprovalRequired.trim().uppercase() == "YES") "YES" else "",
            "reportingCadence" to if (own) "" else reportingCadence.trim().uppercase(),
            "campaignStatus" to "OPEN",
        )
    }
}

/** Client indigo / Own magenta themes with Figma glow colours. */
internal val MarketingClientSetupDocTheme = SetupDocTheme(
    accent = Color(0xFF6366F1),
    canvas = Color(0xFF0F172A),
    card = Color(0xFF1E293B),
    inset = Color(0xFF0F172A),
    border = Color(0xFF334155),
    panelStroke = Color(0xFF475569),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

internal val MarketingOwnSetupDocTheme = SetupDocTheme(
    accent = Color(0xFFD946EF),
    canvas = Color(0xFF0F172A),
    card = Color(0xFF1E293B),
    inset = Color(0xFF0F172A),
    border = Color(0xFF334155),
    panelStroke = Color(0xFF475569),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** Warm orange emblem glow shared by Client and Own (Figma `rgba(234,88,12,0.4)`). */
internal val MarketingEmblemGlow = Color(0xFFEA580C)

internal fun marketingSetupDocTheme(subtypeCode: String): SetupDocTheme =
    if (MarketingSetupDocumentSpec.isOwn(subtypeCode)) MarketingOwnSetupDocTheme else MarketingClientSetupDocTheme

internal fun marketingAccentSoft(subtypeCode: String): Color =
    if (MarketingSetupDocumentSpec.isOwn(subtypeCode)) Color(0xFFE879F9) else Color(0xFF818CF8)

internal class MarketingSetupDocState(
    subtypeCode: String = MarketingSetupDocumentSpec.CLIENT,
    initialName: String = "",
) {
    var subtype by mutableStateOf(subtypeCode)
    var campaignName by mutableStateOf(initialName)
    var companyName by mutableStateOf("")
    var partyName by mutableStateOf("")
    var partyContact by mutableStateOf("")
    var offering by mutableStateOf("")
    var purpose by mutableStateOf("")
    var description by mutableStateOf("")
    var currency by mutableStateOf(MarketingSetupDocumentSpec.currencyLabels().first())
    var startDate by mutableStateOf(java.time.LocalDate.now().toString())
    var endDate by mutableStateOf("")
    var gstin by mutableStateOf("")
    var channels by mutableStateOf(emptySet<String>())
    var budget by mutableStateOf("")
    var clientFee by mutableStateOf("")
    var clientApprovedMediaBudget by mutableStateOf("")
    var agencyOperatingBudget by mutableStateOf("")
    var creativeApprovalRequired by mutableStateOf(false)
    var reportingCadence by mutableStateOf("")
    var mediaApproved by mutableStateOf("")
    var target by mutableStateOf("")
    var metric by mutableStateOf(
        MarketingSetupDocumentSpec.metricLabel(
            MarketingSetupDocumentSpec.subtype(subtypeCode).targetMetrics.first(),
        ),
    )
    var inviteRole by mutableStateOf(MarketingSetupDocumentSpec.DEFAULT_INVITE_ROLE)
    var invitees by mutableStateOf<List<SetupInvitee>>(emptyList())
    var gstTracking by mutableStateOf(MarketingSetupDocumentSpec.gstTrackingLabels.first())
    var gstRegistration by mutableStateOf(MarketingSetupDocumentSpec.gstRegistrationLabels.first())
    var gstState by mutableStateOf("")
    var gstPricing by mutableStateOf(MarketingSetupDocumentSpec.gstPricingLabels.first())
    var gstEffectiveFrom by mutableStateOf("")
    var gstOpen by mutableStateOf(true)
    var draftMomentId by mutableStateOf<String?>(null)
    var companyId by mutableStateOf<String?>(null)

    val hasChannels: Boolean get() = channels.isNotEmpty()

    val canStart: Boolean
        get() {
            val budgetOk = plainAmount(budget).toDoubleOrNull().let { it != null && it > 0 }
            val partyOk = partyName.isNotBlank()
            val nameOk = campaignName.isNotBlank()
            val startOk = startDate.length >= 10
            return nameOk && partyOk && budgetOk && startOk && hasChannels
        }

    fun switchSubtype(code: String) {
        if (code == subtype) return
        subtype = code
        val allowed = MarketingSetupDocumentSpec.channels(code).toSet()
        channels = channels.intersect(allowed)
        val metrics = MarketingSetupDocumentSpec.subtype(code).targetMetrics
        if (MarketingSetupDocumentSpec.metricCode(metric) !in metrics) {
            metric = MarketingSetupDocumentSpec.metricLabel(metrics.first())
        }
        if (MarketingSetupDocumentSpec.inviteRoles(code).none { it.code == inviteRole }) {
            inviteRole = MarketingSetupDocumentSpec.DEFAULT_INVITE_ROLE
        }
        if (MarketingSetupDocumentSpec.isOwn(code)) {
            clientFee = ""
            clientApprovedMediaBudget = ""
            agencyOperatingBudget = ""
            reportingCadence = ""
            mediaApproved = ""
        }
    }

    fun toggleChannel(channel: String) {
        channels = if (channel in channels) channels - channel else channels + channel
    }

    val orderedChannels: List<String>
        get() = MarketingSetupDocumentSpec.channels(subtype).filter { it in channels }

    fun addInvitee(name: String, roleCode: String = inviteRole, phone: String = "") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        invitees = invitees + SetupInvitee(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            roleCode = roleCode,
            phone = phone,
        )
    }

    fun removeInvitee(id: String) {
        invitees = invitees.filterNot { it.id == id }
    }

    fun applyPrefill(preferences: Map<String, Any?>) {
        fun string(key: String): String? = preferences[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        string("subtype")?.let { saved ->
            if (saved.equals(MarketingSetupDocumentSpec.OWN, ignoreCase = true)) {
                subtype = MarketingSetupDocumentSpec.OWN
            } else if (saved.equals(MarketingSetupDocumentSpec.CLIENT, ignoreCase = true)) {
                subtype = MarketingSetupDocumentSpec.CLIENT
            }
        }
        string("identityName")?.let { campaignName = it }
        string("partyName")?.let { partyName = it }
        string("description")?.let { description = it }
        string("offering")?.let { offering = it }
        string("currency")?.let { code ->
            currency = MarketingSetupDocumentSpec.currencyLabels().firstOrNull { it.startsWith(code) } ?: currency
        }
        string("startDate")?.let { startDate = it.take(10) }
        string("endDate")?.let { endDate = it.take(10) }
        string("gstin")?.let { gstin = it }
        string("monthlyBudget")?.let { budget = formatIndianAmount(it) }
        string("monthlyTarget")?.let { target = formatIndianAmount(it) }
        string("clientFee")?.let { clientFee = formatIndianAmount(it) }
        string("clientApprovedMediaBudget")?.let { clientApprovedMediaBudget = formatIndianAmount(it) }
        string("agencyOperatingBudget")?.let { agencyOperatingBudget = formatIndianAmount(it) }
        string("campaignPurpose")?.let { purpose = it }
        string("creativeApprovalRequired")?.let { creativeApprovalRequired = it.equals("YES", ignoreCase = true) }
        string("reportingCadence")?.let { reportingCadence = it.uppercase() }
        string("targetMetric")?.let { metric = MarketingSetupDocumentSpec.metricLabel(it) }
        @Suppress("UNCHECKED_CAST")
        (preferences["channels"] as? List<*>)?.mapNotNull { it?.toString()?.trim() }?.let { list ->
            val choices = MarketingSetupDocumentSpec.channels(subtype).toSet()
            channels = list.filter { it in choices }.toSet()
        }
        string("gstTracking")?.let { saved ->
            gstTracking = MarketingSetupDocumentSpec.gstTrackingLabels.firstOrNull {
                MarketingSetupDocumentSpec.gstTrackingCode(it) == saved.uppercase()
            } ?: gstTracking
        }
        string("gstRegistration")?.let { saved ->
            gstRegistration = MarketingSetupDocumentSpec.gstRegistrationLabels.firstOrNull {
                MarketingSetupDocumentSpec.gstRegistrationCode(it) == saved.uppercase()
            } ?: gstRegistration
        }
        string("salePriceIncludesGst")?.let { saved ->
            gstPricing = MarketingSetupDocumentSpec.gstPricingLabels.firstOrNull {
                MarketingSetupDocumentSpec.gstPricingCode(it) == saved.uppercase()
            } ?: gstPricing
        }
        string("gstState")?.let { gstState = it }
        string("gstEffectiveFrom")?.let { gstEffectiveFrom = it.take(10) }
        inviteRole = MarketingSetupDocumentSpec.inviteRoles(subtype)
            .firstOrNull { it.code == string("inviteRole") }
            ?.code
            ?: MarketingSetupDocumentSpec.DEFAULT_INVITE_ROLE
        invitees = SetupDocumentSpec.invitees(preferences)
    }

    fun preferences(ownerName: String): Map<String, Any> = MarketingSetupDocumentSpec.preferences(
        subtypeCode = subtype,
        identityName = campaignName,
        partyName = partyName,
        description = description.ifBlank { purpose },
        currency = MarketingSetupDocumentSpec.currencyCode(currency),
        startDate = startDate,
        endDate = endDate,
        channels = orderedChannels,
        monthlyBudget = plainAmount(budget),
        monthlyTarget = plainAmount(target),
        targetMetric = MarketingSetupDocumentSpec.metricCode(metric),
        participants = MarketingSetupDocumentSpec.participants(ownerName, invitees),
        gstin = gstin,
        gstState = gstState,
        gstRegistration = MarketingSetupDocumentSpec.gstRegistrationCode(gstRegistration),
        gstTracking = MarketingSetupDocumentSpec.gstTrackingCode(gstTracking),
        salePriceIncludesGst = MarketingSetupDocumentSpec.gstPricingCode(gstPricing),
        gstEffectiveFrom = gstEffectiveFrom,
        clientFee = plainAmount(clientFee),
        clientApprovedMediaBudget = plainAmount(clientApprovedMediaBudget),
        agencyOperatingBudget = plainAmount(agencyOperatingBudget),
        offering = offering,
        campaignPurpose = purpose,
        creativeApprovalRequired = if (creativeApprovalRequired) "YES" else "",
        reportingCadence = reportingCadence,
    )
}
