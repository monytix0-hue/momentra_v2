package com.example.momentra.ui.shell.empty



import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.core.tween

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.slideInHorizontally

import androidx.compose.animation.slideOutHorizontally

import androidx.compose.animation.togetherWith

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import com.example.momentra.domain.AppContext

import com.example.momentra.domain.BottomDestination

import com.example.momentra.domain.MomentExperienceKind

import com.example.momentra.domain.MomentSummary

import com.example.momentra.domain.recentHistoryMoments

import com.example.momentra.ui.shell.empty.personal.PersonalLifeEmptyContent

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.empty.group.GroupCreateMomentContent
import com.example.momentra.ui.shell.empty.group.GroupCreatePhase
import com.example.momentra.ui.shell.empty.group.GroupExperienceSetupContent
import com.example.momentra.ui.shell.empty.group.GroupLifeEmptyContent
import com.example.momentra.ui.shell.empty.group.GroupLivingSetupContent
import com.example.momentra.ui.shell.empty.group.GroupMemoryEmptyContent
import com.example.momentra.ui.shell.empty.group.GroupMomentsEmptyContent
import com.example.momentra.ui.shell.empty.group.GroupPulseEmptyContent
import com.example.momentra.ui.shell.empty.group.GroupPurchaseSetupContent
import com.example.momentra.ui.shell.empty.group.GroupSetupBottomSheet
import com.example.momentra.ui.shell.empty.personal.PersonalCreateEmptyContent
import com.example.momentra.ui.shell.empty.personal.PersonalMemoryEmptyContent
import com.example.momentra.ui.shell.empty.personal.PersonalMomentsEmptyContent
import com.example.momentra.ui.shell.empty.personal.PersonalPulseEmptyContent
import com.example.momentra.ui.shell.personal.experience.PersonalExperienceHost
import com.example.momentra.ui.shell.circle.CircleComingSoonContent
import com.example.momentra.domain.ShellContentState
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateHost

@Composable
fun ContextEmptyExperience(
    context: AppContext,
    destination: BottomDestination,
    experience: MomentExperienceKind,
    moments: List<MomentSummary>,
    hasCompany: Boolean,
    companyId: String? = null,
    /** Operating subtype of the selected moment; picks the App States accent. */
    momentTypeCode: String? = null,
    onCreateMoment: () -> Unit,
    onCreateBack: () -> Unit = {},
    onCompanyActivated: (com.example.momentra.domain.CompanySummary) -> Unit = {},
    onMomentCreated: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    onJoinGroupCode: (String) -> Unit = {},
    /** Opens CREATE → guided business selection → setup shell (NoCompany). */
    onCreateBusiness: () -> Unit = {},
    /** Unused — company join sheet removed; kept for call-site compatibility. */
    onJoinCompany: () -> Unit = {},
    /** Moves the shell to another bottom destination (kit "Back to Pulse" CTAs). */
    onSelectDestination: (BottomDestination) -> Unit = {},
    groupCreatePhase: GroupCreatePhase = GroupCreatePhase.CHOOSER,
    onGroupCreatePhase: (GroupCreatePhase) -> Unit = {},
    /** Opens Group Create tab without resetting phase (Pulse type cards → setup). */
    onOpenGroupCreateTab: () -> Unit = onCreateMoment,
    /** Open Active Moments directory on Completed tab (Group between / completed-only). */
    onOpenCompletedMoments: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val history = recentHistoryMoments(moments)
    val between = experience == MomentExperienceKind.BETWEEN_MOMENTS ||
        experience == MomentExperienceKind.PAUSED_ONLY

    when (context) {
        AppContext.PERSONAL -> PersonalEmpty(
            destination = destination,
            between = between,
            history = history,
            onCreateMoment = onCreateMoment,
            modifier = modifier,
        )
        AppContext.GROUP -> GroupEmpty(
            destination = destination,
            experience = experience,
            history = history,
            onCreateMoment = onCreateMoment,
            onCreateBack = onCreateBack,
            onMomentCreated = onMomentCreated,
            onJoinGroupCode = onJoinGroupCode,
            groupCreatePhase = groupCreatePhase,
            onGroupCreatePhase = onGroupCreatePhase,
            onOpenGroupCreateTab = onOpenGroupCreateTab,
            onOpenCompletedMoments = onOpenCompletedMoments,
            modifier = modifier,
        )

        AppContext.BUSINESS -> BusinessAppStateHost(
            destination = destination,
            content = ShellContentState.Empty,
            hasCompany = hasCompany,
            momentTypeCode = momentTypeCode,
            onRetry = {},
            onPrimary = onCreateMoment,
            onSecondary = { onSelectDestination(BottomDestination.CREATE) },
            onCreateBusiness = { onSelectDestination(BottomDestination.CREATE) },
            onJoinCompany = onJoinCompany,
            onOpenQuickAdds = { onSelectDestination(BottomDestination.CREATE) },
            onBackToPulse = { onSelectDestination(BottomDestination.PULSE) },
            onCreateBack = onCreateBack,
            companyId = companyId,
            onMomentCreated = onMomentCreated,
            modifier = modifier,
        )

        AppContext.CIRCLE -> CircleComingSoonContent(modifier = modifier)

    }

}



@Composable

private fun PersonalEmpty(

    destination: BottomDestination,

    between: Boolean,

    history: List<MomentSummary>,

    onCreateMoment: () -> Unit,

    modifier: Modifier,

) {

    PersonalExperienceHost(destination = destination, modifier = modifier)

}



@Composable
private fun GroupEmpty(
    destination: BottomDestination,
    experience: MomentExperienceKind,
    history: List<MomentSummary>,
    onCreateMoment: () -> Unit,
    onCreateBack: () -> Unit,
    onMomentCreated: (String, String, String?, String) -> Unit,
    onJoinGroupCode: (String) -> Unit,
    groupCreatePhase: GroupCreatePhase,
    onGroupCreatePhase: (GroupCreatePhase) -> Unit,
    onOpenGroupCreateTab: () -> Unit,
    onOpenCompletedMoments: (() -> Unit)? = null,
    modifier: Modifier,
) {
    val first = experience == MomentExperienceKind.FIRST_MOMENT
    when (destination) {
        BottomDestination.CREATE -> GroupCreateFlow(
            phase = groupCreatePhase,
            onPhase = onGroupCreatePhase,
            onCreateBack = onCreateBack,
            onMomentCreated = onMomentCreated,
            onJoinCode = onJoinGroupCode,
            modifier = modifier,
        )
        BottomDestination.PULSE -> if (first) {
            GroupPulseEmptyContent(
                onCreateMoment = onCreateMoment,
                onJoinCode = onJoinGroupCode,
                onSelectExperience = {
                    onGroupCreatePhase(GroupCreatePhase.EXPERIENCE_SETUP)
                    onOpenGroupCreateTab()
                },
                onSelectPurchase = {
                    onGroupCreatePhase(GroupCreatePhase.PURCHASE_SETUP)
                    onOpenGroupCreateTab()
                },
                onSelectLiving = {
                    onGroupCreatePhase(GroupCreatePhase.LIVING_SETUP)
                    onOpenGroupCreateTab()
                },
                modifier = modifier,
            )
        } else {
            GroupBetweenEmpty(history, onCreateMoment, onOpenCompletedMoments, modifier)
        }
        BottomDestination.MOMENTS -> if (first) {
            GroupMomentsEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, onOpenCompletedMoments, modifier)
        }
        BottomDestination.LIFE -> if (first) {
            GroupLifeEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, onOpenCompletedMoments, modifier)
        }
        BottomDestination.MEMORY -> if (first) {
            GroupMemoryEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, onOpenCompletedMoments, modifier)
        }
    }
}

@Composable
fun GroupCreateFlow(
    phase: GroupCreatePhase,
    onPhase: (GroupCreatePhase) -> Unit,
    onCreateBack: () -> Unit,
    onMomentCreated: (String, String, String?, String) -> Unit,
    onJoinCode: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    createViewModel: MomentCreateViewModel = viewModel(),
) {
    val createState by createViewModel.state.collectAsState()
    val handleCreated: (CreateMomentOutcome) -> Unit = { outcome ->
        onPhase(GroupCreatePhase.CHOOSER)
        onMomentCreated(outcome.momentId, outcome.title, outcome.momentTypeCode, outcome.status)
    }
    Box(modifier) {
        GroupCreateMomentContent(
            onBack = onCreateBack,
            onSelectExperience = { onPhase(GroupCreatePhase.EXPERIENCE_SETUP) },
            onSelectPurchase = { onPhase(GroupCreatePhase.PURCHASE_SETUP) },
            onSelectLiving = { onPhase(GroupCreatePhase.LIVING_SETUP) },
            onJoinCode = onJoinCode,
            modifier = Modifier.fillMaxSize(),
        )
        when (phase) {
            GroupCreatePhase.CHOOSER -> Unit
            GroupCreatePhase.EXPERIENCE_SETUP -> GroupSetupBottomSheet(
                onDismiss = { onPhase(GroupCreatePhase.CHOOSER) },
            ) {
                GroupExperienceSetupContent(
                    onBack = { onPhase(GroupCreatePhase.CHOOSER) },
                    onCreated = handleCreated,
                    createViewModel = createViewModel,
                    submitting = createState.submitting,
                    error = createState.error,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            GroupCreatePhase.PURCHASE_SETUP -> GroupSetupBottomSheet(
                onDismiss = { onPhase(GroupCreatePhase.CHOOSER) },
            ) {
                GroupPurchaseSetupContent(
                    onBack = { onPhase(GroupCreatePhase.CHOOSER) },
                    onCreated = handleCreated,
                    createViewModel = createViewModel,
                    submitting = createState.submitting,
                    error = createState.error,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            GroupCreatePhase.LIVING_SETUP -> GroupSetupBottomSheet(
                onDismiss = { onPhase(GroupCreatePhase.CHOOSER) },
            ) {
                GroupLivingSetupContent(
                    onBack = { onPhase(GroupCreatePhase.CHOOSER) },
                    onCreated = handleCreated,
                    createViewModel = createViewModel,
                    submitting = createState.submitting,
                    error = createState.error,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}



@Composable
private fun GroupBetweenEmpty(
    history: List<MomentSummary>,
    onCreateMoment: () -> Unit,
    onOpenCompletedMoments: (() -> Unit)? = null,
    modifier: Modifier,
) {
    MomentEmptyState(
        config = MomentEmptyConfig(
            eyebrow = "GROUP",
            title = "Between group moments",
            body = "Nothing is active together right now. Recent moments together stay here.",
            primaryLabel = "+ Start something new",
            onPrimary = onCreateMoment,
            secondaryLabel = onOpenCompletedMoments?.let { "View completed moments" },
            onSecondary = onOpenCompletedMoments,
            historyTitle = "Recent moments together",
            history = history,
            accent = Color(0xFFE8621A),
        ),
        modifier = modifier,
    )
}



