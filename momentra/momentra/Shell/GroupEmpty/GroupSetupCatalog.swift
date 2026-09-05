import Foundation

struct GroupTypeOption: Identifiable, Equatable {
    let code: String
    let label: String
    let emoji: String
    let nameLabel: String
    let defaultName: String
    var defaultDates: String = ""
    let defaultNotes: String
    var iconName: String? = nil

    var id: String { code }
}

struct GroupSetupVariant {
    let section: String
    let headerTitle: String
    let stepOneTitle: String
    let stepOneSubtitle: String
    let activateLabel: String
    let budgetLocalKey: String
    let budgetNote: String
    let types: [GroupTypeOption]
}

enum GroupSetupCatalog {
    static let experienceTypes: [GroupTypeOption] = [
        .init(code: "TRIP", label: "Trip/Vacation", emoji: "✈️", nameLabel: "Experience Name", defaultName: "Goa Trip", defaultDates: "12-16 Dec 2025", defaultNotes: "Keep this fun, collaborative and unhurried.", iconName: "ges_type_trip"),
        .init(code: "WEDDING", label: "Wedding", emoji: "💍", nameLabel: "Wedding Name", defaultName: "Sarah & Mike's Wedding", defaultDates: "14-16 Mar 2026", defaultNotes: "Keep the celebration intimate and joyful.", iconName: "ges_type_wedding"),
        .init(code: "HOUSE_PARTY", label: "Celebration", emoji: "🎉", nameLabel: "Celebration Name", defaultName: "Rooftop House Party", defaultDates: "22 Nov 2025", defaultNotes: "Music, food, and easy RSVPs.", iconName: "ges_type_party"),
        .init(code: "OFFICE_OUTING", label: "Office Outing", emoji: "🏢", nameLabel: "Outing Name", defaultName: "Team Offsite", defaultDates: "05-07 Feb 2026", defaultNotes: "Workshops by day, dinner by night.", iconName: "ges_type_outing"),
    ]

    static let purchase = GroupSetupVariant(
        section: "purchase",
        headerTitle: "Purchase setup",
        stepOneTitle: "Choose Purchase Type",
        stepOneSubtitle: "Select how your group wants to save or buy together.",
        activateLabel: "Activate Purchase",
        budgetLocalKey: "localBudgetAmount",
        budgetNote: "Budget saves with the moment when you activate",
        types: [
            .init(code: "GIFT_POOL", label: "Gift Pool", emoji: "🎁", nameLabel: "Pool Name", defaultName: "Birthday Gift Pool", defaultNotes: "Pool contributions for a shared gift.", iconName: "ges_type_wedding"),
            .init(code: "GROUP_PURCHASE", label: "Group Purchase", emoji: "🛒", nameLabel: "Purchase Name", defaultName: "Apartment Sofa Purchase", defaultNotes: "Plan and track a shared purchase together.", iconName: "ges_type_trip"),
            .init(code: "SHARED_ASSET", label: "Shared Asset", emoji: "📦", nameLabel: "Asset Name", defaultName: "Family Camera Fund", defaultNotes: "Buy and manage a shared asset.", iconName: "ges_type_party"),
            .init(code: "COMMUNITY_PURCHASE", label: "Custom Purchase", emoji: "✨", nameLabel: "Purchase Name", defaultName: "Custom Purchase", defaultNotes: "Define your own shared purchase moment.", iconName: "ges_type_outing"),
        ]
    )

    static let living = GroupSetupVariant(
        section: "living",
        headerTitle: "Living setup",
        stepOneTitle: "Choose Living Type",
        stepOneSubtitle: "Select the shared living arrangement you want to coordinate.",
        activateLabel: "Activate Living",
        budgetLocalKey: "localHouseholdBudget",
        budgetNote: "Budget saves with the moment when you activate",
        types: [
            .init(code: "FLATMATES", label: "Flatmates", emoji: "🏠", nameLabel: "Household Name", defaultName: "Flat 4B Shared Home", defaultNotes: "Coordinate chores, bills and house rhythm.", iconName: "ges_type_trip"),
            .init(code: "FAMILY_HOUSEHOLD", label: "Family Household", emoji: "👨‍👩‍👧", nameLabel: "Household Name", defaultName: "Family Household", defaultNotes: "Shared family routines and responsibilities.", iconName: "ges_type_wedding"),
            .init(code: "CO_LIVING", label: "Co-living", emoji: "🏘️", nameLabel: "Space Name", defaultName: "Co-living Space", defaultNotes: "Manage a co-living arrangement together.", iconName: "ges_type_party"),
            .init(code: "COMMUNITY_LIVING", label: "Custom Living", emoji: "✨", nameLabel: "Living Name", defaultName: "Custom Living", defaultNotes: "Define your own shared living moment.", iconName: "ges_type_outing"),
        ]
    )
}

