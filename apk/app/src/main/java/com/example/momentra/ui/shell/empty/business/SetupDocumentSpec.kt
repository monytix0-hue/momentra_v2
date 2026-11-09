package com.example.momentra.ui.shell.empty.business

import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.shell.business.shared.OperatingInviteRole
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSubtype
import com.example.momentra.ui.shell.business.shared.businessInviteMembership
import com.example.momentra.ui.shell.business.shared.businessInviteRoleLabel
import com.example.momentra.ui.shell.business.shared.inviteRoleAccess

/**
 * The six-section setup document of `TRD-SETUP-v6` (`2566:32923`) and `DST-SETUP-v6`
 * (`2560:29336`), as data. Everything the document renders — section order, channel and
 * role chip sets, the §04 count subtitles, and the preference keys it emits — lives here so
 * it can be asserted in a JVM unit test without Compose.
 */
internal data class SetupSectionSpec(
    val number: String,
    val title: String,
    val subtitle: String,
)

/**
 * One person §02 sent an invite to, before the company exists. Kept in the document state so
 * the row survives a rerender and rides the draft save as a `participants` preference row.
 *
 * The role is held as the wire code, not the chip label: an invite minted for a code the chip
 * set no longer offers must round-trip unchanged instead of degrading to the default role.
 */
internal data class SetupInvitee(
    val id: String,
    val name: String,
    val roleCode: String,
    val phone: String = "",
) {
    val roleLabel: String get() = SetupDocumentSpec.roleLabel(roleCode)
}

/** Live §04 counts, read from the company's masters tables. A zero count is never printed. */
internal data class SetupMasterCounts(
    val skus: Int = 0,
    val priceLists: Int = 0,
    val warehouses: Int = 0,
    val routes: Int = 0,
    val principals: Int = 0,
    val dealers: Int = 0,
) {
    val isEmpty: Boolean
        get() = skus == 0 && priceLists == 0 && warehouses == 0 && routes == 0 &&
            principals == 0 && dealers == 0
}

internal object SetupDocumentSpec {
    const val TRADING = "TRADING_WHOLESALE"
    const val DISTRIBUTION = "DISTRIBUTION_DEALERSHIP"

    const val CATALOGUE_INVITE = "Name, barcode, MRP, case size, HSN, GST"
    const val WAREHOUSE_INVITE = "Godowns hold stock · routes carry it · branches receive"
    const val NETWORK_INVITE = "GSTIN on file · ITC posts on GRN, never on PO save"

    /** The role every §02 chip defaults to; also the fallback for an unknown saved code. */
    const val DEFAULT_INVITE_ROLE = "OPERATIONS_LEAD"

    /** The six sections, in the order the frames stack them. */
    fun sections(subtypeCode: String): List<SetupSectionSpec> {
        val distribution = isDistribution(subtypeCode)
        val noun = if (distribution) "distribution" else "trading"
        return listOf(
            SetupSectionSpec("01", "Business Basics", "A home for this $noun business."),
            SetupSectionSpec(
                "02",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
            ),
            SetupSectionSpec(
                "03",
                "Channels & Monthly Plan",
                "Where do you sell? Choose at least one.",
            ),
            SetupSectionSpec(
                "04",
                "Catalogue, Warehouses & Network",
                "Masters your orders, dispatches and purchases read from. Each opens its own sheet.",
            ),
            SetupSectionSpec("05", "GST & Tax Details", "Set up GST once to make buying and selling easier."),
            SetupSectionSpec("06", "Review your setup", ""),
        )
    }

    fun isDistribution(subtypeCode: String): Boolean =
        subtypeCode.equals(DISTRIBUTION, ignoreCase = true)

    fun subtype(subtypeCode: String): OperatingSubtype =
        OperatingMomentCatalog.subtypes(BusinessSetupKind.TRADING_DISTRIBUTION)
            .firstOrNull { it.code.equals(subtypeCode, ignoreCase = true) }
            ?: OperatingMomentCatalog.subtypes(BusinessSetupKind.TRADING_DISTRIBUTION).first()

    fun channels(subtypeCode: String): List<String> =
        if (isDistribution(subtypeCode)) OperatingMomentCatalog.distributionChannels
        else OperatingMomentCatalog.tradingChannels

    fun inviteRoles(subtypeCode: String): List<OperatingInviteRole> =
        if (isDistribution(subtypeCode)) OperatingMomentCatalog.distributionInviteRoles
        else OperatingMomentCatalog.tradingInviteRoles

    /** The label for a role code, or the code itself when a saved draft carries an old one. */
    fun roleLabel(code: String): String =
        inviteRoles(TRADING).firstOrNull { it.code == code }?.label ?: businessInviteRoleLabel(code)

    /** The code for a role label; unknown labels fall back to the default operations role. */
    fun roleCode(label: String): String =
        inviteRoles(TRADING).firstOrNull { it.label == label }?.code ?: DEFAULT_INVITE_ROLE

    /** `FINANCE` and `APPROVER` mint an admin membership; the other two mint a member. */
    fun membershipType(code: String): String = businessInviteMembership(code)

    /** What the selected role can reach, under the chips. */
    fun inviteAccessNote(code: String): String = "Access: ${inviteRoleAccess(code)}."

    /**
     * §02 participant rows for the wire. The owner is always first, because the backend
     * normaliser re-inserts them if the list omits them and an invitee must never be mistaken
     * for the owner.
     */
    fun participants(ownerName: String, invitees: List<SetupInvitee>): List<Map<String, String>> =
        buildList {
            add(
                mapOf(
                    "displayName" to ownerName.ifBlank { "Owner" },
                    "role" to "OWNER_ADMIN",
                ),
            )
            invitees.forEach { invitee ->
                val name = invitee.name.trim()
                if (name.isEmpty()) return@forEach
                add(
                    mapOf(
                        "displayName" to name,
                        "role" to invitee.roleCode.ifBlank { DEFAULT_INVITE_ROLE },
                    ),
                )
            }
        }

    /** Reads `preferences.participants` back into §02 rows, skipping the owner row. */
    fun invitees(preferences: Map<String, Any?>): List<SetupInvitee> =
        (preferences["participants"] as? List<*>).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { row ->
                val name = (row["displayName"] as? String)?.trim().orEmpty()
                val code = (row["role"] as? String)?.trim()?.uppercase().orEmpty()
                if (name.isEmpty() || code == "OWNER_ADMIN") return@mapNotNull null
                SetupInvitee(
                    id = "saved:" + name.lowercase(),
                    name = name,
                    roleCode = code.ifBlank { DEFAULT_INVITE_ROLE },
                    phone = (row["phone"] as? String)?.trim().orEmpty(),
                )
            }

    fun networkLabel(subtypeCode: String): String = subtype(subtypeCode).networkLabel

    fun kindLabel(subtypeCode: String): String = if (isDistribution(subtypeCode)) "Distribution" else "Trading"

    /** §02's `Default …` footnote, keyed off the default invite role. */
    fun defaultRoleNote(subtypeCode: String): String =
        "Default ${roleLabel(DEFAULT_INVITE_ROLE)} · " +
            "${inviteRoleAccess(DEFAULT_INVITE_ROLE)}. Role applies when they join."

    // MARK: §06 copy

    /**
     * A draft is started; a live moment is saved. Printing `Start this business` over a moment
     * that is already running would read like a second, duplicate business.
     */
    fun activateLabel(status: String): String =
        if (status.equals("ACTIVE", ignoreCase = true)) "Save changes" else "Start this business"

    fun activatingLabel(status: String): String =
        if (status.equals("ACTIVE", ignoreCase = true)) "Saving…" else "Starting…"

    fun statusPillLabel(status: String): String =
        if (status.equals("ACTIVE", ignoreCase = true)) "Business is live" else "Setup in progress"

    /**
     * Only a draft may be discarded on Close or promoted on Start; an ACTIVE moment is edited in
     * place, because discarding it would delete a record the operator is still running.
     */
    fun isDiscardable(status: String): Boolean = status.equals("DRAFT", ignoreCase = true)

    fun isPromotable(status: String): Boolean = status.equals("DRAFT", ignoreCase = true)

    /** §02's team subtitle for an invited member. */
    fun teamMeta(status: String, role: String): String {
        val verb = when (status.uppercase()) {
            "ACTIVE" -> "Added"
            "PENDING" -> "Invite sent"
            else -> "Added"
        }
        return "$verb · $role"
    }

    /**
     * §04 row one. With nothing in the catalogue the row falls back to the invitation copy —
     * a zero is never printed, because `0 SKUs` reads like a real, empty catalogue.
     */
    fun catalogueSubtitle(counts: SetupMasterCounts): String =
        if (counts.skus <= 0) CATALOGUE_INVITE
        else "${counts.skus} SKU${plural(counts.skus)} · " +
            (if (counts.priceLists > 0) "${counts.priceLists} price list${plural(counts.priceLists)} · " else "") +
            "HSN + GST on each"

    /** §04 row three. Named warehouses first, then the route count. */
    fun warehouseSubtitle(names: List<String>, counts: SetupMasterCounts): String {
        if (counts.warehouses <= 0) return WAREHOUSE_INVITE
        val head = names.filter { it.isNotBlank() }.take(3).joinToString(" · ")
        val routes = if (counts.routes > 0) " · ${counts.routes} route${plural(counts.routes)}" else ""
        return if (head.isBlank()) "${counts.warehouses} warehouse${plural(counts.warehouses)}$routes" else head + routes
    }

    /** §04 row four — `Principals & dealers` for trading, `Suppliers & buyers` for distribution. */
    fun networkSubtitle(subtypeCode: String, counts: SetupMasterCounts): String {
        val primary = if (isDistribution(subtypeCode)) "supplier" else "principal"
        val secondary = if (isDistribution(subtypeCode)) "buyer" else "dealer"
        if (counts.principals <= 0 && counts.dealers <= 0) return NETWORK_INVITE
        val parts = buildList {
            if (counts.principals > 0) add("${counts.principals} $primary${plural(counts.principals)}")
            if (counts.dealers > 0) add("${counts.dealers} $secondary${plural(counts.dealers)}")
            add("GSTIN on file")
        }
        return parts.joinToString(" · ")
    }

    /** §04 row five — the invite role chips, in frame order. */
    fun inviteSubtitle(subtypeCode: String): String =
        inviteRoles(subtypeCode).joinToString(" · ") { it.label }

    /** §04's closing footnote; the noun tracks the subtype. */
    fun networkFootnote(subtypeCode: String): String {
        val noun = if (isDistribution(subtypeCode)) "buyers" else "principals"
        return "Warehouses hold stock; routes carry it; $noun carry GSTIN for ITC on GRN. " +
            "GST posts on invoice or GRN — never on SO / PO save."
    }

    /** §03's why-block, verbatim from the frames. */
    const val TARGET_WHY_TITLE = "How your target is counted"
    const val TARGET_WHY_BODY =
        "Posted invoices count toward the target. Orders and delivery orders do not. " +
            "Purchases and expenses count once toward the budget."

    /** §02's why-block title, verbatim from the frames. */
    const val APPROVAL_WHY_TITLE = "No approval to start"

    fun approvalWhyBody(owner: String, name: String): String {
        val who = owner.ifBlank { "You" }
        val moment = name.ifBlank { "this moment" }
        return "$who can open $moment without a manager. Invite is not approval and not payment."
    }

    /** §05's skip caveat, verbatim from the frames. */
    const val GST_SKIP_CAVEAT =
        "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."

    const val GST_NOTE = "Your GSTIN stays the one entered above. It is stored as a reference only."

    /** The metric labels `targetMetric` can hold.
     *
     *  `REVENUE` and `ORDERS` print as words; anything else prints as `Count` because the
     *  backend accepts the code but the frame never shows a raw enum to an operator.
     */
    fun metricLabel(metric: String): String = when (metric.uppercase()) {
        "REVENUE" -> "Revenue"
        "ORDERS" -> "Orders"
        "CUSTOM_COUNT" -> "Count"
        // The frame never shows a raw enum, so anything unrecognised reads as a count.
        else -> if (metric.isBlank()) "Revenue" else "Count"
    }

    fun metricCode(label: String): String = when (label) {
        "Orders" -> "ORDERS"
        "Count" -> "CUSTOM_COUNT"
        else -> "REVENUE"
    }

    /** §05 `Prices include GST` / `GST is added separately` → the backend's `YES` / `NO`. */
    fun gstPricingCode(label: String): String =
        if (label.startsWith("Prices include", ignoreCase = true)) "YES" else "NO"

    /** §05 `Regular` / `Composition` / `Unregistered` / `Not sure` → the backend enum. */
    fun gstRegistrationCode(label: String): String = when (label) {
        "Regular" -> "REGULAR"
        "Composition" -> "COMPOSITION"
        "Unregistered" -> "UNREGISTERED"
        "Not sure" -> "NOT_SURE"
        else -> ""
    }

    /** §05 `Yes` / `Not now` → the backend enum. */
    fun gstTrackingCode(label: String): String = when (label) {
        "Yes" -> "YES"
        "Not now" -> "NOT_NOW"
        else -> ""
    }

    val gstRegistrationLabels = listOf("Regular", "Composition", "Unregistered", "Not sure")
    val gstPricingLabels = listOf("Prices include GST", "GST is added separately")
    val gstTrackingLabels = listOf("Yes", "Not now")
    val currencyLabels = listOf("INR · Indian Rupee (₹)", "USD · US Dollar", "AED · UAE Dirham")

    fun currencyCode(label: String): String =
        label.substringBefore(" ·").trim().uppercase().ifBlank { "INR" }

    /**
     * §06's second line: `Trading · INR · From 12 October 2026 · Still open`. Dates arrive
     * as `yyyy-MM-dd` from the setup state and print as `12 October 2026` like the frame.
     */
    fun momentLine(subtypeCode: String, currency: String, startDate: String, endDate: String): String {
        val kind = kindLabel(subtypeCode)
        val code = currency.ifBlank { "INR" }
        val from = if (startDate.isBlank()) "Today" else setupDateLabel(startDate)
        val to = if (endDate.isBlank()) "Still open" else "Until ${setupDateLabel(endDate)}"
        return "$kind · $code · From $from · $to"
    }

    /** §06's `October 2026 budget` / `October 2026 sales target` key prefixes. */
    fun monthLabel(yearMonth: String): String {
        if (yearMonth.isBlank()) return "This month"
        val parts = yearMonth.split("-")
        if (parts.size < 2) return yearMonth
        val year = parts[0].toIntOrNull() ?: return yearMonth
        val month = parts[1].toIntOrNull() ?: return yearMonth
        if (month !in 1..12) return yearMonth
        val name = java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        return "$name $year"
    }

    fun setupDateLabel(iso: String): String {
        val date = runCatching { java.time.LocalDate.parse(iso.take(10)) }.getOrNull() ?: return iso
        return date.dayOfMonth.toString() + " " +
            date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " +
            date.year
    }

    /** §02 avatar initials — `Kiran Rao` → `KR`, a single word → its first letter. */
    fun initials(name: String): String {
        val parts = name.trim().split(" ", "\t").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    /** §03's `N selected. …` helper line under the channel chips. */
    fun channelHint(selected: Int): String =
        "$selected selected. These are the places you sell or serve. They are not a different kind of business."

    /**
     * Every preference key the document emits. Asserted as a subset of
     * [OperatingMomentCatalog.preferenceKeys] so a new field can never be silently dropped by
     * the catalog allowlist on its way to the backend.
     */
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
        "counterparties",
        "gstin",
        "gstState",
        "gstStateCode",
        "gstRegistration",
        "gstTracking",
        "salePriceIncludesGst",
        "purchasePriceIncludesGst",
        "gstEffectiveFrom",
    )

    /**
     * The body the document sends. `partyName` carries the trading party (the company the
     * moment trades with) and `identityName` the moment name, exactly as §01 labels them.
     */
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
        targetMetric: String = "REVENUE",
        participants: List<Map<String, String>> = emptyList(),
        gstin: String = "",
        gstState: String = "",
        gstRegistration: String = "",
        gstTracking: String = "",
        salePriceIncludesGst: String = "",
        purchasePriceIncludesGst: String = salePriceIncludesGst,
        gstEffectiveFrom: String = "",
    ): Map<String, Any> = mapOf(
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
        "targetMetric" to targetMetric.trim().uppercase().ifBlank { "REVENUE" },
        "participants" to participants,
        "gstin" to gstin.trim().uppercase(),
        // Canonical name plus the two-digit code, so jurisdiction never depends on spelling.
        "gstState" to IndiaStateCatalog.canonical(gstState).ifBlank { gstState.trim() },
        "gstStateCode" to IndiaStateCatalog.codeOf(gstState),
        "gstRegistration" to gstRegistration.trim().uppercase(),
        "gstTracking" to gstTracking.trim().uppercase(),
        "salePriceIncludesGst" to salePriceIncludesGst.trim().uppercase(),
        "purchasePriceIncludesGst" to purchasePriceIncludesGst.trim().uppercase(),
        "gstEffectiveFrom" to gstEffectiveFrom.trim(),
    )

    private fun plural(count: Int): String = if (count == 1) "" else "s"
}
