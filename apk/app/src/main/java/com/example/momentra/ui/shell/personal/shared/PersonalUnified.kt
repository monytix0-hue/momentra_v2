package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.ui.shell.empty.personal.PersonalSetupSystem
import com.example.momentra.ui.shell.empty.personal.toPulseFamily

/**
 * Unified Personal shell helpers — prefer Life Ops, family target pick for Add,
 * missing-area detection for setup overlay.
 *
 * Moment lists are treated as newest-first (bootstrap order) when no updatedAt exists.
 */
fun activePersonalMoments(moments: List<MomentSummary>): List<MomentSummary> =
    moments.filter { it.isActiveStatus() }

/**
 * Preferred Personal moment:
 * 1. Active Life Ops / Everyday
 * 2. Else current selection if still active Personal
 * 3. Else most recently updated active Personal (first in newest-first list)
 */
fun resolvePreferredPersonalMoment(
    moments: List<MomentSummary>,
    currentSelectedId: String?,
): MomentSummary? {
    val active = activePersonalMoments(moments)
    if (active.isEmpty()) return null
    active.firstOrNull {
        personalPulseFamilyFor(it.momentTypeCode) == PersonalPulseFamily.LIFE_OPERATIONS
    }?.let { return it }
    active.firstOrNull { it.momentId == currentSelectedId }?.let { return it }
    return active.firstOrNull()
}

/** Target moment for a family Add tile when multiple moments exist in that family. */
fun resolveFamilyTargetMoment(
    moments: List<MomentSummary>,
    family: PersonalPulseFamily,
    currentSelectedId: String?,
): MomentSummary? {
    val inFamily = activePersonalMoments(moments).filter {
        personalPulseFamilyFor(it.momentTypeCode) == family
    }
    if (inFamily.isEmpty()) return null
    inFamily.firstOrNull { it.momentId == currentSelectedId }?.let { return it }
    return inFamily.firstOrNull()
}

fun presentPersonalFamilies(moments: List<MomentSummary>): Set<PersonalPulseFamily> =
    activePersonalMoments(moments)
        .map { personalPulseFamilyFor(it.momentTypeCode) }
        .toSet()

fun missingPersonalSetupSystems(moments: List<MomentSummary>): List<PersonalSetupSystem> {
    val present = presentPersonalFamilies(moments)
    return PersonalSetupSystem.entries.filter { it.toPulseFamily() !in present }
}

fun PersonalPulseFamily.toSetupSystem(): PersonalSetupSystem = when (this) {
    PersonalPulseFamily.LIFE_OPERATIONS -> PersonalSetupSystem.LIFE_OPERATIONS
    PersonalPulseFamily.FUTURE_BUILDING -> PersonalSetupSystem.FUTURE_BUILDING
    PersonalPulseFamily.LIFESTYLE -> PersonalSetupSystem.LIFESTYLE
    PersonalPulseFamily.RELATIONSHIPS -> PersonalSetupSystem.RELATIONSHIPS
}
