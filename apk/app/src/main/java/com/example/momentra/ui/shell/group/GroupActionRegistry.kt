package com.example.momentra.ui.shell.group

import com.example.momentra.R

/**
 * Mapper from V019 Group capability / action codes to Quick Add destinations.
 * SETTLEMENT_RECORD is LIVE (V047) — enable Settle when capability present.
 */
object GroupActionRegistry {

    const val EXPENSE_CREATE = "EXPENSE_CREATE"
    const val CONTRIBUTION_RECORD = "CONTRIBUTION_RECORD"
    const val SETTLEMENT_RECORD = "SETTLEMENT_RECORD"
    const val PARTICIPANT_MANAGE = "PARTICIPANT_MANAGE"
    const val PLANNING_ITEM_CREATE = "PLANNING_ITEM_CREATE"
    const val BOOKING_CREATE = "BOOKING_CREATE"
    const val POLL_CREATE = "POLL_CREATE"
    const val UPDATE_CREATE = "UPDATE_CREATE"
    const val MEMORY_CREATE = "MEMORY_CREATE"
    const val PURCHASE_ITEM_CREATE = "PURCHASE_ITEM_CREATE"
    const val RESIDENT_MANAGE = "RESIDENT_MANAGE"

    enum class Destination {
        EXPENSE,
        CONTRIBUTION,
        SETTLEMENT,
        PARTICIPANTS,
        BUDGET,
        PLANNING,
        BOOKING,
        POLL,
        MEMORY,
        UPDATE,
        INVITE,
        PURCHASE_ITEM,
        RESIDENT,
    }

    data class HubTileSpec(
        val id: String,
        val label: String,
        val subtitle: String,
        val iconRes: Int,
        val gradientStart: androidx.compose.ui.graphics.Color,
        val gradientEnd: androidx.compose.ui.graphics.Color,
        val destination: Destination,
        val capabilityCode: String? = null,
        val apiGap: Boolean = false,
    )

    /** Figma 575:14655 — Trip Action Center 3×3 grid. */
    val tripHubTileIds: Set<String> = setOf(
        "expense", "planning", "budget", "booking", "poll", "memory", "update", "contribution", "invite",
    )

    val figmaHubTiles: List<HubTileSpec> = listOf(
        HubTileSpec("expense", "Expense", "Split a cost", R.drawable.ic_group_qa_wallet, androidx.compose.ui.graphics.Color(0xFF33C759), androidx.compose.ui.graphics.Color(0xFF0F766E), Destination.EXPENSE, EXPENSE_CREATE),
        HubTileSpec("planning", "Planning", "Itinerary & tasks", R.drawable.ic_group_qa_calendar, androidx.compose.ui.graphics.Color(0xFF14B8A6), androidx.compose.ui.graphics.Color(0xFF0F766E), Destination.PLANNING, PLANNING_ITEM_CREATE),
        HubTileSpec("budget", "Budget", "Edit planned total", R.drawable.ic_group_qa_chartbar, androidx.compose.ui.graphics.Color(0xFFFFB598), androidx.compose.ui.graphics.Color(0xFFE8621A), Destination.BUDGET),
        HubTileSpec("booking", "Booking", "Reservations", R.drawable.ic_group_qa_ticket, androidx.compose.ui.graphics.Color(0xFFFF7A3D), androidx.compose.ui.graphics.Color(0xFFE85940), Destination.BOOKING, BOOKING_CREATE),
        HubTileSpec("poll", "Poll", "Group decisions", R.drawable.ic_group_qa_vote, androidx.compose.ui.graphics.Color(0xFFA855F7), androidx.compose.ui.graphics.Color(0xFF7C3AED), Destination.POLL, POLL_CREATE),
        HubTileSpec("memory", "Memory", "Capture a moment", R.drawable.ic_group_qa_camera, androidx.compose.ui.graphics.Color(0xFFFF8E63), androidx.compose.ui.graphics.Color(0xFFE8744F), Destination.MEMORY, MEMORY_CREATE),
        HubTileSpec("update", "Update", "Share status", R.drawable.ic_group_qa_megaphone, androidx.compose.ui.graphics.Color(0xFF3B82F6), androidx.compose.ui.graphics.Color(0xFF1D4ED8), Destination.UPDATE, UPDATE_CREATE),
        HubTileSpec("contribution", "Contribution", "Record money in", R.drawable.ic_group_qa_handshake, androidx.compose.ui.graphics.Color(0xFF10B981), androidx.compose.ui.graphics.Color(0xFF047857), Destination.CONTRIBUTION, CONTRIBUTION_RECORD),
        HubTileSpec("invite", "Invite", "Add people", R.drawable.ic_group_qa_userplus, androidx.compose.ui.graphics.Color(0xFFFFB598), androidx.compose.ui.graphics.Color(0xFFE8621A), Destination.INVITE, PARTICIPANT_MANAGE),
        HubTileSpec("settle", "Settle", "Pay down balances", R.drawable.ic_group_qa_wallet, androidx.compose.ui.graphics.Color(0xFF059669), androidx.compose.ui.graphics.Color(0xFF10B981), Destination.SETTLEMENT, SETTLEMENT_RECORD),
        HubTileSpec("purchase", "Purchase item", "Track a buy", R.drawable.ic_group_qa_chartbar, androidx.compose.ui.graphics.Color(0xFFF59E0B), androidx.compose.ui.graphics.Color(0xFFD97706), Destination.PURCHASE_ITEM, PURCHASE_ITEM_CREATE),
        HubTileSpec("resident", "Resident", "Add a housemate", R.drawable.ic_group_qa_userplus, androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF4338CA), Destination.RESIDENT, RESIDENT_MANAGE),
    )

    val figmaTripHubTiles: List<HubTileSpec>
        get() = figmaHubTiles.filter { it.id in tripHubTileIds }

    fun destinationFor(capabilityCode: String): Destination? = when (capabilityCode.uppercase()) {
        EXPENSE_CREATE -> Destination.EXPENSE
        CONTRIBUTION_RECORD -> Destination.CONTRIBUTION
        SETTLEMENT_RECORD -> Destination.SETTLEMENT
        PARTICIPANT_MANAGE -> Destination.PARTICIPANTS
        PLANNING_ITEM_CREATE -> Destination.PLANNING
        BOOKING_CREATE -> Destination.BOOKING
        POLL_CREATE -> Destination.POLL
        UPDATE_CREATE -> Destination.UPDATE
        MEMORY_CREATE -> Destination.MEMORY
        PURCHASE_ITEM_CREATE -> Destination.PURCHASE_ITEM
        RESIDENT_MANAGE -> Destination.RESIDENT
        else -> null
    }

    fun isDestinationEnabled(capabilities: List<String>, destination: Destination): Boolean {
        val tile = figmaHubTiles.find { it.destination == destination } ?: return false
        if (tile.apiGap) return false
        // Empty capabilities must NOT enable everything — fail closed until bootstrap fills V019 codes.
        if (capabilities.isEmpty()) return tile.capabilityCode == null
        return tile.capabilityCode?.let { code ->
            capabilities.any { it.equals(code, ignoreCase = true) }
        } ?: true
    }

    fun hubTileEnabled(hasActiveMoment: Boolean, capabilities: List<String>, tile: HubTileSpec): Boolean {
        if (!hasActiveMoment) return false
        if (tile.apiGap) return false
        if (capabilities.isEmpty()) return tile.capabilityCode == null
        return tile.capabilityCode?.let { code ->
            capabilities.any { it.equals(code, ignoreCase = true) }
        } ?: true
    }

    fun enabledDestinations(capabilities: List<String>): Set<Destination> {
        return figmaHubTiles.filter { tile ->
            hubTileEnabled(hasActiveMoment = true, capabilities = capabilities, tile = tile)
        }.map { it.destination }.toSet()
    }
}
