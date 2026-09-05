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

import com.example.momentra.ui.shell.PersonalLifeEmptyContent

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
import com.example.momentra.ui.shell.circle.CircleComingSoonContent
import com.example.momentra.ui.theme.ShellTokens

@Composable
fun ContextEmptyExperience(
    context: AppContext,
    destination: BottomDestination,
    experience: MomentExperienceKind,
    moments: List<MomentSummary>,
    hasCompany: Boolean,
    companyId: String? = null,
    onCreateMoment: () -> Unit,
    onCreateBack: () -> Unit = {},
    onCompanyActivated: (com.example.momentra.domain.CompanySummary) -> Unit = {},
    onMomentCreated: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    onJoinGroupCode: (String) -> Unit = {},
    groupCreatePhase: GroupCreatePhase = GroupCreatePhase.CHOOSER,
    onGroupCreatePhase: (GroupCreatePhase) -> Unit = {},
    /** Opens Group Create tab without resetting phase (Pulse type cards → setup). */
    onOpenGroupCreateTab: () -> Unit = onCreateMoment,
    modifier: Modifier = Modifier,
) {
    val accent = ShellTokens.contextSelectedColor(context)
    val history = recentHistoryMoments(moments)
    val first = experience == MomentExperienceKind.FIRST_MOMENT
    val between = experience == MomentExperienceKind.BETWEEN_MOMENTS ||
        experience == MomentExperienceKind.PAUSED_ONLY

    when (context) {
        AppContext.PERSONAL -> PersonalEmpty(
            destination = destination,
            between = between,
            history = history,
            onCreateMoment = onCreateMoment,
            onMomentCreated = onMomentCreated,
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
            modifier = modifier,
        )

        AppContext.BUSINESS -> BusinessEmpty(
            destination = destination,
            first = first,
            history = history,
            hasCompany = hasCompany,
            companyId = companyId,
            onCreateMoment = onCreateMoment,
            onCreateBack = onCreateBack,
            onCompanyActivated = onCompanyActivated,
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

    onMomentCreated: (String, String, String?, String) -> Unit,

    modifier: Modifier,

) {

    when (destination) {

        BottomDestination.LIFE -> PersonalLifeEmptyContent(

            onStartCta = onCreateMoment,

            history = if (between) history else emptyList(),

            modifier = modifier,

        )

        BottomDestination.CREATE -> PersonalCreateEmptyContent(

            history = if (between) history else emptyList(),

            onMomentCreated = onMomentCreated,

            modifier = modifier,

        )

        BottomDestination.PULSE -> PersonalPulseEmptyContent(

            onCreateMoment = onCreateMoment,

            history = if (between) history else emptyList(),

            modifier = modifier,

        )

        BottomDestination.MOMENTS -> PersonalMomentsEmptyContent(

            onCreateMoment = onCreateMoment,

            history = if (between) history else emptyList(),

            modifier = modifier,

        )

        BottomDestination.MEMORY -> PersonalMemoryEmptyContent(

            onCreateMoment = onCreateMoment,

            history = if (between) history else emptyList(),

            modifier = modifier,

        )

    }

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
            GroupBetweenEmpty(history, onCreateMoment, modifier)
        }
        BottomDestination.MOMENTS -> if (first) {
            GroupMomentsEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, modifier)
        }
        BottomDestination.LIFE -> if (first) {
            GroupLifeEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, modifier)
        }
        BottomDestination.MEMORY -> if (first) {
            GroupMemoryEmptyContent(onCreateMoment = onCreateMoment, modifier = modifier)
        } else {
            GroupBetweenEmpty(history, onCreateMoment, modifier)
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

    modifier: Modifier,

) {

    MomentEmptyState(

        config = MomentEmptyConfig(

            eyebrow = "GROUP",

            title = "Between group moments",

            body = "Nothing is active together right now. Recent moments together stay here.",

            primaryLabel = "+ Start something new",

            onPrimary = onCreateMoment,

            historyTitle = "Recent moments together",

            history = history,

            accent = Color(0xFFE8621A),

        ),

        modifier = modifier,

    )

}



@Composable

private fun BusinessEmpty(
    destination: BottomDestination,
    first: Boolean,
    history: List<MomentSummary>,
    hasCompany: Boolean,
    companyId: String? = null,
    onCreateMoment: () -> Unit,
    onCreateBack: () -> Unit,
    onCompanyActivated: (com.example.momentra.domain.CompanySummary) -> Unit = {},
    onMomentCreated: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier,
) {
    val accent = Color(0xFF818CF8)

    if (destination == BottomDestination.CREATE) {
        AnimatedContent(
            targetState = hasCompany,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it / 5 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally { -it / 6 } + fadeOut(tween(240)))
                } else {
                    (slideInHorizontally { -it / 5 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally { it / 6 } + fadeOut(tween(240)))
                }
            },
            label = "businessCompanyGate",
            modifier = modifier,
        ) { companyReady ->
            if (!companyReady) {
                CompanySetupContent(
                    onClose = onCreateBack,
                    onActivated = onCompanyActivated,
                )
            } else {
                BusinessCreateFlow(
                    companyId = companyId,
                    onCreateBack = onCreateBack,
                    onMomentCreated = onMomentCreated,
                    createViewModel = createViewModel,
                )
            }
        }
        return
    }



    if (first) {

        when (destination) {

            BottomDestination.PULSE -> BusinessPulseEmptyContent(onStartCta = onCreateMoment, modifier = modifier)

            BottomDestination.MOMENTS -> BusinessMomentsEmptyContent(onStartCta = onCreateMoment, modifier = modifier)

            BottomDestination.CREATE -> Unit

            BottomDestination.LIFE -> BusinessLifeEmptyContent(onStartCta = onCreateMoment, modifier = modifier)

            BottomDestination.MEMORY -> BusinessMemoryEmptyContent(onStartCta = onCreateMoment, modifier = modifier)

        }

        return

    }



    when (destination) {

        BottomDestination.PULSE -> MomentEmptyState(

            config = MomentEmptyConfig(

                eyebrow = "PULSE",

                title = "All quiet for now",

                body = "There isn't an active work signal needing your attention.",

                primaryLabel = "+ Start a Business Moment",

                onPrimary = onCreateMoment,

                historyTitle = "Recent work",

                history = history,

                accent = accent,

            ),

            modifier = modifier,

        )

        BottomDestination.MOMENTS -> MomentEmptyState(

            config = MomentEmptyConfig(

                eyebrow = "BUSINESS",

                title = "No active work moments",

                body = "Create a moment for something your business is working on — a launch, campaign, event, project or initiative.",

                primaryLabel = "+ Create Business Moment",

                onPrimary = onCreateMoment,

                historyTitle = "Recent work",

                history = history,

                accent = accent,

            ),

            modifier = modifier,

        )

        BottomDestination.CREATE -> Unit

        BottomDestination.LIFE -> MomentEmptyState(

            config = MomentEmptyConfig(

                eyebrow = "LIFE",

                title = "Your business story continues",

                body = "As work moments accumulate, Life connects people, finances, and operations into one picture.",

                primaryLabel = "+ Start a new Moment",

                onPrimary = onCreateMoment,

                historyTitle = "Recent work",

                history = history,

                accent = accent,

            ),

            modifier = modifier,

        )

        BottomDestination.MEMORY -> MomentEmptyState(

            config = MomentEmptyConfig(

                eyebrow = "MEMORY",

                title = "Patterns wait quietly",

                body = "Nothing needs your attention right now. Past work stays ready when you want to revisit it.",

                primaryLabel = "+ Start a new Moment",

                onPrimary = onCreateMoment,

                historyTitle = "Past work",

                history = history,

                accent = accent,

            ),

            modifier = modifier,

        )

    }

}

