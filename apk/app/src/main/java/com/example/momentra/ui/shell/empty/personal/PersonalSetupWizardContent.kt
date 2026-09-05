package com.example.momentra.ui.shell.empty.personal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.personal.shared.PersonalPulseFamily

/**
 * Interactive Personal setup — Figma long-form sheets (353:6809+).
 * Shell chrome stays native; bodies are dedicated setup screens + Activate.
 */
enum class PersonalSetupSystem(
    val code: String,
    val momentTypeCode: String,
    val defaultTitle: String,
    val label: String,
    val setupTitle: String,
    val analyticsScreen: String,
) {
    LIFE_OPERATIONS(
        "LIFE_OPERATIONS", "LIFE_RHYTHM", "My life operations rhythm", "Life Operations",
        "Set up Life Operations", com.example.momentra.analytics.AnalyticsScreens.PERSONAL_SETUP_LIFE_OPS,
    ),
    FUTURE_BUILDING(
        "FUTURE_BUILDING", "FUTURE_GOAL", "My future building", "Future Building",
        "Set up Future Building", com.example.momentra.analytics.AnalyticsScreens.PERSONAL_SETUP_FUTURE,
    ),
    LIFESTYLE(
        "LIFESTYLE", "LIFESTYLE", "My intentional lifestyle", "Lifestyle",
        "Set up Lifestyle", com.example.momentra.analytics.AnalyticsScreens.PERSONAL_SETUP_LIFESTYLE,
    ),
    RELATIONSHIPS(
        "RELATIONSHIPS", "RELATIONSHIP_CONNECTION", "My relationships", "Relationships",
        "Set up Relationships", com.example.momentra.analytics.AnalyticsScreens.PERSONAL_SETUP_RELATIONSHIPS,
    ),
}

enum class PersonalSetupKind(val systemCode: String) {
    LIFE_OPERATIONS("LIFE_OPERATIONS"),
    FUTURE_BUILDING("FUTURE_BUILDING"),
    LIFESTYLE("LIFESTYLE"),
    RELATIONSHIPS("RELATIONSHIPS"),
}

fun PersonalSetupSystem.toKind(): PersonalSetupKind = when (this) {
    PersonalSetupSystem.LIFE_OPERATIONS -> PersonalSetupKind.LIFE_OPERATIONS
    PersonalSetupSystem.FUTURE_BUILDING -> PersonalSetupKind.FUTURE_BUILDING
    PersonalSetupSystem.LIFESTYLE -> PersonalSetupKind.LIFESTYLE
    PersonalSetupSystem.RELATIONSHIPS -> PersonalSetupKind.RELATIONSHIPS
}

fun PersonalSetupSystem.toPulseFamily(): PersonalPulseFamily = when (this) {
    PersonalSetupSystem.LIFE_OPERATIONS -> PersonalPulseFamily.LIFE_OPERATIONS
    PersonalSetupSystem.FUTURE_BUILDING -> PersonalPulseFamily.FUTURE_BUILDING
    PersonalSetupSystem.LIFESTYLE -> PersonalPulseFamily.LIFESTYLE
    PersonalSetupSystem.RELATIONSHIPS -> PersonalPulseFamily.RELATIONSHIPS
}

@Composable
fun PersonalSetupWizardContent(
    system: PersonalSetupSystem,
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    when (system) {
        PersonalSetupSystem.LIFE_OPERATIONS -> PersonalLifeOpsSetupContent(
            onBack = onBack,
            onCreated = onCreated,
            createViewModel = createViewModel,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
        )
        PersonalSetupSystem.FUTURE_BUILDING -> PersonalFutureSetupContent(
            onBack = onBack,
            onCreated = onCreated,
            createViewModel = createViewModel,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
        )
        PersonalSetupSystem.LIFESTYLE -> PersonalLifestyleSetupContent(
            onBack = onBack,
            onCreated = onCreated,
            createViewModel = createViewModel,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
        )
        PersonalSetupSystem.RELATIONSHIPS -> PersonalRelationshipsSetupContent(
            onBack = onBack,
            onCreated = onCreated,
            createViewModel = createViewModel,
            modifier = modifier,
            editingMomentId = editingMomentId,
            initialTitle = initialTitle,
        )
    }
}
