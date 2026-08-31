package com.example.momentra.ui.shell.empty.group

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.momentra.R

data class GroupTypeOption(
    val code: String,
    val label: String,
    val nameLabel: String,
    val defaultName: String,
    val defaultDates: String = "",
    val defaultNotes: String,
    val iconRes: Int,
    val cardGradient: Brush,
    val palette: GroupTypePalette,
    val selectedBorder: Color,
    val unselectedBorder: Color = GroupSetupTheme.Border,
    val selectedShadow: Color? = null,
)

data class GroupSetupVariant(
    val section: String,
    val headerTitle: String,
    val stepOneTitle: String,
    val stepOneSubtitle: String,
    val activateLabel: String,
    val budgetLocalKey: String,
    val types: List<GroupTypeOption>,
)

object GroupSetupCatalog {
    val experienceTypes = listOf(
        GroupTypeOption(
            code = "TRIP",
            label = "Trip/Vacation",
            nameLabel = "Experience Name",
            defaultName = "Goa Trip",
            defaultDates = "12-16 Dec 2025",
            defaultNotes = "Keep this fun, collaborative and unhurried.",
            iconRes = R.drawable.ges_type_trip,
            cardGradient = GroupSetupTheme.TripGradient,
            palette = GroupSetupTheme.tripPalette,
            selectedBorder = GroupSetupTheme.TripAccent,
            selectedShadow = Color(0x33E8744F),
        ),
        GroupTypeOption(
            code = "WEDDING",
            label = "Wedding",
            nameLabel = "Wedding Name",
            defaultName = "Sarah & Mike's Wedding",
            defaultDates = "14-16 Mar 2026",
            defaultNotes = "Keep the celebration intimate and joyful.",
            iconRes = R.drawable.ges_type_wedding,
            cardGradient = GroupSetupTheme.WeddingGradient,
            palette = GroupSetupTheme.weddingPalette,
            selectedBorder = GroupSetupTheme.WeddingAccent,
            selectedShadow = Color(0x33EC4899),
        ),
        GroupTypeOption(
            code = "HOUSE_PARTY",
            label = "Celebration/House Party",
            nameLabel = "Party Name",
            defaultName = "Rooftop House Party",
            defaultDates = "22 Nov 2025",
            defaultNotes = "Music, food, and easy RSVPs.",
            iconRes = R.drawable.ges_type_party,
            cardGradient = GroupSetupTheme.PartyGradient,
            palette = GroupSetupTheme.partyPalette,
            selectedBorder = GroupSetupTheme.PartyAccent,
            selectedShadow = Color(0x333B82F6),
        ),
        GroupTypeOption(
            code = "OFFICE_OUTING",
            label = "Office Outing",
            nameLabel = "Outing Name",
            defaultName = "Team Offsite",
            defaultDates = "05-07 Feb 2026",
            defaultNotes = "Workshops by day, dinner by night.",
            iconRes = R.drawable.ges_type_outing,
            cardGradient = GroupSetupTheme.OutingGradient,
            palette = GroupSetupTheme.outingPalette,
            selectedBorder = GroupSetupTheme.OutingAccent,
            selectedShadow = Color(0x3314B8A6),
        ),
    )

    val purchase = GroupSetupVariant(
        section = "purchase",
        headerTitle = "Purchase setup",
        stepOneTitle = "Choose Purchase Type",
        stepOneSubtitle = "Select how your group wants to save or buy together.",
        activateLabel = "Activate Purchase",
        budgetLocalKey = "localBudgetAmount",
        types = listOf(
            type("GIFT_POOL", "Gift Pool", "Pool Name", "Birthday Gift Pool", "Pool contributions for a shared gift.", R.drawable.ges_type_wedding, GroupSetupTheme.weddingPalette, GroupSetupTheme.WeddingGradient, GroupSetupTheme.WeddingAccent, Color(0x33EC4899)),
            type("GROUP_PURCHASE", "Group Purchase", "Purchase Name", "Apartment Sofa Purchase", "Plan and track a shared purchase together.", R.drawable.ges_type_trip, GroupSetupTheme.tripPalette, GroupSetupTheme.TripGradient, GroupSetupTheme.TripAccent, Color(0x33E8744F)),
            type("SHARED_ASSET", "Shared Asset", "Asset Name", "Family Camera Fund", "Buy and manage a shared asset.", R.drawable.ges_type_party, GroupSetupTheme.partyPalette, GroupSetupTheme.PartyGradient, GroupSetupTheme.PartyAccent, Color(0x333B82F6)),
            type("COMMUNITY_PURCHASE", "Custom Purchase", "Purchase Name", "Custom Purchase", "Define your own shared purchase moment.", R.drawable.ges_type_outing, GroupSetupTheme.outingPalette, GroupSetupTheme.OutingGradient, GroupSetupTheme.OutingAccent, Color(0x3314B8A6)),
        ),
    )

    val living = GroupSetupVariant(
        section = "living",
        headerTitle = "Living setup",
        stepOneTitle = "Choose Living Type",
        stepOneSubtitle = "Select the shared living arrangement you want to coordinate.",
        activateLabel = "Activate Living",
        budgetLocalKey = "localHouseholdBudget",
        types = listOf(
            type("FLATMATES", "Flatmates", "Household Name", "Flat 4B Shared Home", "Coordinate chores, bills and house rhythm.", R.drawable.ges_type_trip, GroupSetupTheme.tripPalette, GroupSetupTheme.TripGradient, GroupSetupTheme.TripAccent, Color(0x33E8744F)),
            type("FAMILY_HOUSEHOLD", "Family Household", "Household Name", "Family Household", "Shared family routines and responsibilities.", R.drawable.ges_type_wedding, GroupSetupTheme.weddingPalette, GroupSetupTheme.WeddingGradient, GroupSetupTheme.WeddingAccent, Color(0x33EC4899)),
            type("CO_LIVING", "Co-living", "Space Name", "Co-living Space", "Manage a co-living arrangement together.", R.drawable.ges_type_party, GroupSetupTheme.partyPalette, GroupSetupTheme.PartyGradient, GroupSetupTheme.PartyAccent, Color(0x333B82F6)),
            type("COMMUNITY_LIVING", "Custom Living", "Living Name", "Custom Living", "Define your own shared living moment.", R.drawable.ges_type_outing, GroupSetupTheme.outingPalette, GroupSetupTheme.OutingGradient, GroupSetupTheme.OutingAccent, Color(0x3314B8A6)),
        ),
    )

    private fun type(
        code: String,
        label: String,
        nameLabel: String,
        defaultName: String,
        defaultNotes: String,
        iconRes: Int,
        palette: GroupTypePalette,
        gradient: Brush,
        selectedBorder: Color,
        shadow: Color,
    ) = GroupTypeOption(code, label, nameLabel, defaultName, defaultNotes = defaultNotes, iconRes = iconRes, cardGradient = gradient, palette = palette, selectedBorder = selectedBorder, selectedShadow = shadow)

    fun findTypeOption(code: String): GroupTypeOption? =
        experienceTypes.find { it.code == code }
            ?: purchase.types.find { it.code == code }
            ?: living.types.find { it.code == code }
}
