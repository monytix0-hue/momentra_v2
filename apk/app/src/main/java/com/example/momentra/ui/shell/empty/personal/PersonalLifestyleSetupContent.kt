package com.example.momentra.ui.shell.empty.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.MomentraAnalytics
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Figma `353:7075` — full Lifestyle setup body. */
@Composable
fun PersonalLifestyleSetupContent(
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    val catalog = remember { PersonalSetupCatalog.forKind(PersonalSetupKind.LIFESTYLE) }
    val selections = remember {
        mutableStateMapOf<String, Any>().apply { putAll(catalog.defaultPreferences) }
    }
    var momentTitle by remember { mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: catalog.defaultTitle) }
    var showHabit2 by remember { mutableStateOf(false) }
    val createState by createViewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val accent = PersonalSetupLongFormTokens.Purple

    val sectionKeys = listOf(
        setOf("vision", "current", "primaryPriority", "workLifeBalance", "homeEnvironment"),
        setOf("vision", "current", "healthEnergy", "socialRhythm", "homeRhythm", "topPriority", "neglectedArea"),
        setOf(
            "habit", "habit2", "desiredFeeling", "remindWeekly",
            "energyCheckIn", "balanceCheckIn", "reviewCadence",
        ),
        setOf("vision", "current", "topPriority", "profile"),
    )
    val (sectionsConfigured, answersSaved) = setupStatusCounts(
        defaults = catalog.defaultPreferences,
        selections = selections.toMap(),
        sectionKeys = sectionKeys,
    )

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.PERSONAL_SETUP_LIFESTYLE)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.PERSONAL_SETUP_LIFESTYLE) }
    }

    Column(modifier = modifier.fillMaxSize().background(PersonalSetupLongFormTokens.Bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PersonalSetupCloseRow(onBack = onBack, enabled = !createState.submitting)
            PersonalSetupHero(
                emoji = "✨",
                title = "Set up Lifestyle",
                subtitle = catalog.subtitle,
                accent = accent,
            )
            SetupTitleField(
                label = "Moment title",
                value = momentTitle,
                onValueChange = { momentTitle = it },
                placeholder = catalog.defaultTitle,
                testTag = MaestroIds.SETUP_TITLE,
            )

            PersonalSetupSectionCard(number = "01", title = "Lifestyle Basics", accent = accent) {
                PersonalSetupStageHeader(
                    "Lifestyle vision", "vision",
                    listOf("Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"),
                    selections,
                )
                PersonalSetupDualPills(
                    "Current", "current",
                    listOf("Good, with room to grow", "Designing more balance"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR LIFESTYLE", small = true)
                PersonalSetupInlineDropdown(
                    "Vision", "How do you want life to feel?", "vision",
                    listOf("Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Current", "Where are you today?", "current",
                    listOf(
                        "Good, with room to grow",
                        "Steady",
                        "Transition",
                        "Overloaded",
                        "Renewing",
                    ),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Primary priority", "What comes first?", "primaryPriority",
                    listOf("Health & energy", "Connection", "Work", "Creativity", "Home"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR PREFERENCES", small = true)
                PersonalSetupInlineDropdown(
                    "Work–life balance", "Protect room to breathe", "workLifeBalance",
                    listOf("Selected", "Not selected"), selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Home environment", "Shape where you live", "homeEnvironment",
                    listOf("Selected", "Not selected"), selections, accent,
                )
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "02", title = "Preferences & Balance", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "How You Want to Live",
                    border = PersonalSetupLongFormTokens.Teal,
                    glyph = "▣",
                ) {
                    PersonalSetupInlineDropdown(
                        "Vision", "Your chosen direction", "vision",
                        listOf("Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                    PersonalSetupInlineDropdown(
                        "Current", "Your starting point", "current",
                        listOf(
                            "Good, with room to grow",
                            "Designing more balance",
                            "Steady",
                            "Transition",
                            "Overloaded",
                            "Renewing",
                        ),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Balance",
                    border = PersonalSetupLongFormTokens.Orange,
                    glyph = "⚠",
                ) {
                    PersonalSetupInlineDropdown(
                        "Health & energy", "How strong you feel", "healthEnergy",
                        listOf("Strong and consistent", "Steady", "Variable", "Low"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                    PersonalSetupInlineDropdown(
                        "Social rhythm", "How often you connect", "socialRhythm",
                        listOf("A few times a week", "Daily", "Weekends", "Rarely"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                    PersonalSetupInlineDropdown(
                        "Home rhythm", "How home feels", "homeRhythm",
                        listOf("Calm & organized", "Busy", "Minimal", "Creative"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Priorities & Focus",
                    border = PersonalSetupLongFormTokens.Green,
                    glyph = "♡",
                ) {
                    PersonalSetupInlineDropdown(
                        "Top priority", "Where attention goes", "topPriority",
                        listOf("Health & energy", "Connection", "Work", "Creativity", "Home"),
                        selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                    )
                    PersonalSetupInlineDropdown(
                        "Neglected area", "What needs more care", "neglectedArea",
                        listOf("Rest & recovery", "Self care", "Social time", "Planning"),
                        selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                    )
                }
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "03", title = "Habits & Outlook", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "Lifestyle Anchors",
                    border = PersonalSetupLongFormTokens.Pink,
                    glyph = "⚡",
                ) {
                    PersonalSetupInlineDropdown(
                        "Habit", "Selected anchor", "habit",
                        listOf("Movement routine", "Sleep routine", "Nutrition", "Learning", "Rest"),
                        selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                    )
                    if (showHabit2 || selectionString(selections, "habit2").isNotBlank()) {
                        PersonalSetupInlineDropdown(
                            "Second habit", "Another lifestyle anchor", "habit2",
                            listOf("Movement routine", "Sleep routine", "Nutrition", "Learning", "Rest"),
                            selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                        )
                    }
                    PersonalSetupAddRow(
                        "+ ADD another habit",
                        PersonalSetupLongFormTokens.Pink,
                        onClick = { showHabit2 = true },
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Future Lifestyle",
                    border = PersonalSetupLongFormTokens.Blue,
                    glyph = "◇",
                ) {
                    PersonalSetupInlineDropdown(
                        "Desired feeling", "How you want to feel", "desiredFeeling",
                        listOf("Balanced", "Minimal", "Adventurous", "Grounded"),
                        selections, PersonalSetupLongFormTokens.Blue, PersonalSetupLongFormTokens.Blue,
                    )
                    PersonalSetupToggleRow(
                        "Remind me weekly",
                        "Keep a light pulse check",
                        selections["remindWeekly"] as? Boolean == true,
                        { selections["remindWeekly"] = it },
                        PersonalSetupLongFormTokens.Blue,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Reflection Preferences",
                    border = PersonalSetupLongFormTokens.Indigo,
                    glyph = "✦",
                ) {
                    PersonalSetupInlineDropdown(
                        "Energy check-in", "Notice energy early", "energyCheckIn",
                        listOf("Enabled", "Disabled"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                    PersonalSetupInlineDropdown(
                        "Balance check-in", "Protect life balance", "balanceCheckIn",
                        listOf("Enabled", "Disabled"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                    PersonalSetupInlineDropdown(
                        "Review cadence", "How often to reflect", "reviewCadence",
                        listOf("Every week", "Every month", "On demand"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                }
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "04", title = "Lifestyle Summary", accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, accent, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PersonalSetupSummaryRow("Vision", selectionString(selections, "vision"), big = true)
                    PersonalSetupSummaryRow("Current", selectionString(selections, "current"))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalSetupLongFormTokens.Border))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Top priority",
                            color = PersonalSetupLongFormTokens.Green,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            selectionString(selections, "topPriority"),
                            color = accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    PersonalSetupSummaryProgress(accent)
                }
            }

            PersonalSetupActivateFooter(
                statusLine = "$sectionsConfigured sections configured • $answersSaved answers saved",
                readyLine = "Your lifestyle plan is ready",
                ctaLabel = catalog.activateLabel,
                footerTagline = catalog.footerTagline,
                ctaBrush = Brush.horizontalGradient(listOf(accent, Color(0xFFF472B6))),
                submitting = createState.submitting,
                error = createState.error,
                onActivate = {
                    if (createState.submitting) return@PersonalSetupActivateFooter
                    createViewModel.submitPersonalSetup(
                        kind = PersonalSetupKind.LIFESTYLE,
                        preferences = selections.toMap(),
                        title = momentTitle.trim().ifBlank { catalog.defaultTitle },
                        editingMomentId = editingMomentId,
                        onSuccess = { outcome: CreateMomentOutcome ->
                            onCreated(outcome.momentId, outcome.title, outcome.momentTypeCode ?: catalog.momentTypeCode)
                        },
                    )
                },
            )
        }
    }
}
