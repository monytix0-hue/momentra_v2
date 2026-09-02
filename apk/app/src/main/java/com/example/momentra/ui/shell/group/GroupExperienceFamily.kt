package com.example.momentra.ui.shell.group

/** Group experience / purchase / living family variants — Figma populated frames. */
enum class GroupExperienceFamily {
    SHARED_GENERIC,
    WEDDING,
    HOUSE_PARTY,
    OFFICE_OUTING,
    GIFT_POOL,
    GROUP_PURCHASE,
    SHARED_ASSET,
    CUSTOM_PURCHASE,
    FLATMATES,
    FAMILY_HOUSEHOLD,
    CO_LIVING,
    CUSTOM_LIVING,
}

fun groupExperienceFamilyFor(momentTypeCode: String?): GroupExperienceFamily {
    val code = momentTypeCode?.uppercase() ?: return GroupExperienceFamily.SHARED_GENERIC
    return when {
        code.contains("WEDDING") -> GroupExperienceFamily.WEDDING
        code.contains("HOUSE_PARTY") -> GroupExperienceFamily.HOUSE_PARTY
        code.contains("OFFICE_OUTING") -> GroupExperienceFamily.OFFICE_OUTING
        code.contains("GIFT_POOL") -> GroupExperienceFamily.GIFT_POOL
        code.contains("GROUP_PURCHASE") -> GroupExperienceFamily.GROUP_PURCHASE
        code.contains("SHARED_ASSET") -> GroupExperienceFamily.SHARED_ASSET
        code.contains("COMMUNITY_PURCHASE") -> GroupExperienceFamily.CUSTOM_PURCHASE
        code.contains("FLATMATES") -> GroupExperienceFamily.FLATMATES
        code.contains("FAMILY_HOUSEHOLD") -> GroupExperienceFamily.FAMILY_HOUSEHOLD
        code.contains("CO_LIVING") -> GroupExperienceFamily.CO_LIVING
        code.contains("COMMUNITY_LIVING") -> GroupExperienceFamily.CUSTOM_LIVING
        code.contains("SHARED_LIVING") -> GroupExperienceFamily.FLATMATES
        code.contains("SHARED_RENTAL") -> GroupExperienceFamily.FLATMATES
        code == "CUSTOM" -> GroupExperienceFamily.CUSTOM_LIVING
        else -> GroupExperienceFamily.SHARED_GENERIC
    }
}

fun GroupExperienceFamily.isWedding(): Boolean = this == GroupExperienceFamily.WEDDING

fun GroupExperienceFamily.isHouseParty(): Boolean = this == GroupExperienceFamily.HOUSE_PARTY

fun GroupExperienceFamily.isOfficeOuting(): Boolean = this == GroupExperienceFamily.OFFICE_OUTING

fun GroupExperienceFamily.isThemedExperience(): Boolean =
    this == GroupExperienceFamily.HOUSE_PARTY || this == GroupExperienceFamily.OFFICE_OUTING

fun GroupExperienceFamily.isThemedPurchase(): Boolean =
    this == GroupExperienceFamily.GIFT_POOL ||
        this == GroupExperienceFamily.GROUP_PURCHASE ||
        this == GroupExperienceFamily.SHARED_ASSET ||
        this == GroupExperienceFamily.CUSTOM_PURCHASE

fun GroupExperienceFamily.isThemedLiving(): Boolean =
    this == GroupExperienceFamily.FLATMATES ||
        this == GroupExperienceFamily.FAMILY_HOUSEHOLD ||
        this == GroupExperienceFamily.CO_LIVING ||
        this == GroupExperienceFamily.CUSTOM_LIVING

fun GroupExperienceFamily.invitePeopleSubtitle(): String = when {
    isThemedLiving() -> "Share a link or add someone to this household"
    isThemedPurchase() -> "Share a link or add someone to this purchase"
    else -> "Share a link or add someone to this trip"
}
