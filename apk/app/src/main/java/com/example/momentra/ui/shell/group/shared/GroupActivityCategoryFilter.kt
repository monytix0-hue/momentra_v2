package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.ui.shell.group.experience.create.ExperienceQuickAddKind
import com.example.momentra.ui.shell.group.experience.create.emoji
import com.example.momentra.ui.shell.group.living.create.LivingQuickAddKind
import com.example.momentra.ui.shell.group.living.create.emoji
import com.example.momentra.ui.shell.group.living.create.label
import com.example.momentra.ui.shell.group.purchase.create.PurchaseQuickAddKind
import com.example.momentra.ui.shell.group.purchase.create.emoji
import com.example.momentra.ui.shell.group.purchase.create.label
import com.example.momentra.ui.shell.group.wedding.create.WeddingQuickAddKind
import java.util.Locale

/** Category chips for Group “All activity”, sourced from that moment’s Quick Add hub names. */
object GroupActivityCategoryFilter {
    const val ALL_ID = "All"

    data class FilterChip(
        val id: String,
        val label: String,
        val emoji: String,
    )

    fun chips(momentTypeCode: String?): List<FilterChip> {
        val all = FilterChip(ALL_ID, "All", "📋")
        val family = groupExperienceFamilyFor(momentTypeCode)
        val hub = when {
            family.isWedding() -> WeddingQuickAddKind.entries.map {
                FilterChip(chipId(it), weddingHubLabel(it), weddingEmoji(it))
            }
            family.isThemedExperience() -> ExperienceQuickAddKind.entries.map {
                FilterChip(chipId(it), experienceHubLabel(it), it.emoji())
            }
            family.isThemedPurchase() -> PurchaseQuickAddKind.entries.map {
                FilterChip(chipId(it), it.label(), it.emoji())
            }
            family.isThemedLiving() -> LivingQuickAddKind.entries.map {
                FilterChip(chipId(it), it.label(), it.emoji())
            }
            else -> tripHubChips()
        }
        return listOf(all) + hub
    }

    fun matches(item: ActivityItemDto, chipId: String): Boolean =
        matches(item.activityCode, chipId)

    fun matches(activityCode: String, chipId: String): Boolean {
        if (chipId == ALL_ID || chipId.isBlank()) return true
        return activityBelongs(chipId, activityCode)
    }

    private fun tripHubChips(): List<FilterChip> {
        val emojiById = mapOf(
            "expense" to "💳",
            "planning" to "📋",
            "budget" to "💰",
            "booking" to "🧳",
            "poll" to "📊",
            "memory" to "📷",
            "update" to "📢",
            "contribution" to "🎁",
            "invite" to "👤",
        )
        // figmaHubTiles order already matches trip hub layout for these ids.
        return GroupActionRegistry.figmaTripHubTiles.map {
            FilterChip(it.id, it.label, emojiById[it.id] ?: "📌")
        }
    }

    private fun chipId(kind: WeddingQuickAddKind): String =
        kind.name.lowercase(Locale.US)

    private fun chipId(kind: ExperienceQuickAddKind): String =
        kind.name.lowercase(Locale.US)

    private fun chipId(kind: LivingQuickAddKind): String =
        kind.name.lowercase(Locale.US)

    private fun chipId(kind: PurchaseQuickAddKind): String = when (kind) {
        PurchaseQuickAddKind.PURCHASE_ITEM -> "purchaseItem"
        else -> kind.name.lowercase(Locale.US)
    }

    private fun weddingHubLabel(kind: WeddingQuickAddKind): String = when (kind) {
        WeddingQuickAddKind.PARTICIPANT -> "Invite"
        WeddingQuickAddKind.PLANNING -> "Planning"
        WeddingQuickAddKind.EXPENSE -> "Expense"
        WeddingQuickAddKind.BUDGET -> "Budget"
        WeddingQuickAddKind.CONTRIBUTION -> "Contribution"
        WeddingQuickAddKind.SETTLE -> "Settle"
        WeddingQuickAddKind.VENDOR -> "Vendor"
        WeddingQuickAddKind.ATTENDANCE -> "Attendance"
        WeddingQuickAddKind.UPDATE -> "Update"
        WeddingQuickAddKind.POLL -> "Poll"
        WeddingQuickAddKind.MEMORY -> "Memory"
    }

    private fun weddingEmoji(kind: WeddingQuickAddKind): String = when (kind) {
        WeddingQuickAddKind.PARTICIPANT -> "👤"
        WeddingQuickAddKind.PLANNING -> "📋"
        WeddingQuickAddKind.EXPENSE -> "💳"
        WeddingQuickAddKind.BUDGET -> "💰"
        WeddingQuickAddKind.CONTRIBUTION -> "🎁"
        WeddingQuickAddKind.SETTLE -> "⚖️"
        WeddingQuickAddKind.VENDOR -> "🏪"
        WeddingQuickAddKind.ATTENDANCE -> "✅"
        WeddingQuickAddKind.UPDATE -> "📢"
        WeddingQuickAddKind.POLL -> "📊"
        WeddingQuickAddKind.MEMORY -> "📷"
    }

    private fun experienceHubLabel(kind: ExperienceQuickAddKind): String = when (kind) {
        ExperienceQuickAddKind.PARTICIPANT -> "Invite"
        ExperienceQuickAddKind.PLANNING -> "Planning"
        ExperienceQuickAddKind.EXPENSE -> "Expense"
        ExperienceQuickAddKind.BUDGET -> "Budget"
        ExperienceQuickAddKind.CONTRIBUTION -> "Contribution"
        ExperienceQuickAddKind.SETTLE -> "Settle"
        ExperienceQuickAddKind.VENDOR -> "Vendor"
        ExperienceQuickAddKind.ATTENDANCE -> "Attendance"
        ExperienceQuickAddKind.UPDATE -> "Update"
        ExperienceQuickAddKind.POLL -> "Poll"
        ExperienceQuickAddKind.MEMORY -> "Memory"
        ExperienceQuickAddKind.BOOKING -> "Booking"
    }

    private fun activityBelongs(chipId: String, activityCode: String): Boolean {
        val upper = activityCode.uppercase(Locale.US)
        return when (chipId) {
            "expense" -> upper.contains("EXPENSE")
            "contribution" -> upper.contains("CONTRIB")
            "settle" -> upper.contains("SETTLE")
            "planning" -> upper.contains("PLANNING")
            "task" -> upper.contains("TASK") && !upper.contains("PLANNING")
            "booking" -> upper.contains("BOOKING")
            "poll" -> upper.contains("POLL")
            "memory" -> upper.contains("MEMORY")
            "update" -> upper.contains("UPDATE")
            "invite", "participant", "resident", "contributor" ->
                upper.contains("MEMBER") ||
                    upper.contains("RESIDENT") ||
                    upper.contains("PARTICIPANT") ||
                    upper.contains("INVITE") ||
                    upper.contains("CONTRIBUTOR")
            "vendor" -> upper.contains("VENDOR")
            "attendance" -> upper.contains("ATTENDANCE")
            "purchaseItem", "purchase" -> upper.contains("PURCHASE")
            "delivery" -> upper.contains("DELIVERY")
            "ownership" -> upper.contains("OWNERSHIP") || upper.contains("TRANSFER")
            "asset" -> upper.contains("ASSET")
            "maintenance" -> upper.contains("MAINTENANCE")
            "rule" -> upper.contains("RULE")
            "budget" -> upper.contains("BUDGET")
            else -> false
        }
    }
}
