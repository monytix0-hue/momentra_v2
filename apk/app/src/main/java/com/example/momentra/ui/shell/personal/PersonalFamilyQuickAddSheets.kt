package com.example.momentra.ui.shell.personal

/** Lifestyle quick-adds from Pulse `505:12365` actions. */
enum class LifestyleQuickAddKind {
    EXPERIENCE,
    WELLBEING,
    DISCOVERY,
    EXPRESSION,
    ADJUST,
}

fun LifestyleQuickAddKind.apiContext(): String = when (this) {
    LifestyleQuickAddKind.EXPERIENCE -> "EXPERIENCE"
    LifestyleQuickAddKind.WELLBEING -> "WELLBEING"
    LifestyleQuickAddKind.DISCOVERY -> "DISCOVERY"
    LifestyleQuickAddKind.EXPRESSION -> "CREATION"
    LifestyleQuickAddKind.ADJUST -> "LIFESTYLE"
}

/** Relationships quick-adds from Pulse `505:11793` actions. */
enum class RelationshipsQuickAddKind {
    CONNECTION,
    SHARED,
    INVESTMENT,
    SUPPORT,
    ADJUST,
}

fun RelationshipsQuickAddKind.apiActivityKind(): String = when (this) {
    RelationshipsQuickAddKind.CONNECTION -> "CONNECTION"
    RelationshipsQuickAddKind.SUPPORT -> "SUPPORT"
    RelationshipsQuickAddKind.SHARED -> "SHARED_EXPERIENCE"
    RelationshipsQuickAddKind.INVESTMENT -> "INVESTMENT"
    RelationshipsQuickAddKind.ADJUST -> "INTERACTION"
}
