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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import kotlin.math.roundToInt

/** Figma `353:6809` — full Life Operations setup body. */
@Composable
fun PersonalLifeOpsSetupContent(
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    val catalog = remember { PersonalSetupCatalog.forKind(PersonalSetupKind.LIFE_OPERATIONS) }
    val selections = remember {
        mutableStateMapOf<String, Any>().apply { putAll(catalog.defaultPreferences) }
    }
    var momentTitle by remember { mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: catalog.defaultTitle) }
    var category by remember { mutableStateOf("Financials") }
    var showHabit2 by remember { mutableStateOf(false) }
    val createState by createViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val sectionOffsets = remember { mutableStateMapOf<String, Int>() }
    val accent = PersonalSetupLongFormTokens.Purple

    val categoryTabs = listOf(
        "Financials" to Color(0xFF4CD6FF),
        "Wellbeing" to PersonalSetupLongFormTokens.Green,
        "Routine" to PersonalSetupLongFormTokens.Amber,
        "Personal" to PersonalSetupLongFormTokens.Orange,
    )
    val tabToSection = mapOf(
        "Financials" to "01",
        "Wellbeing" to "02",
        "Routine" to "03",
        "Personal" to "04",
    )
    val sectionKeys = listOf(
        setOf("lifeFocus", "currentRhythm", "primaryNeed", "healthEnergy", "timeBalance"),
        setOf(
            "shapesFocus", "shapesRhythm", "mainPressure", "recoveryWindow",
            "checkInRhythm", "helpfulSupport", "recoveryStyle",
        ),
        setOf(
            "habit", "habit2", "currentEnergy", "reflectWeekly",
            "stressCheckIn", "recoveryCheckIn", "reviewCadence",
        ),
        setOf("lifeFocus", "currentRhythm", "currentEnergy", "profile"),
    )
    val (sectionsConfigured, answersSaved) = setupStatusCounts(
        defaults = catalog.defaultPreferences,
        selections = selections.toMap(),
        sectionKeys = sectionKeys,
    )

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.PERSONAL_SETUP_LIFE_OPS)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.PERSONAL_SETUP_LIFE_OPS) }
    }

    fun scrollToSection(sectionId: String) {
        scope.launch {
            val y = sectionOffsets[sectionId] ?: return@launch
            scrollState.animateScrollTo(y.coerceAtLeast(0))
        }
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
                emoji = "🚀",
                title = "Set up Life Operations",
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
            PersonalSetupCategoryTabs(
                tabs = categoryTabs,
                selected = category,
                onSelect = { label ->
                    category = label
                    tabToSection[label]?.let { scrollToSection(it) }
                },
            )

            Box(
                modifier = Modifier.onGloballyPositioned { coords ->
                    sectionOffsets["01"] = coords.positionInParent().y.roundToInt()
                },
            ) {
                PersonalSetupSectionCard(number = "01", title = "Life Basics", accent = accent) {
                    PersonalSetupStageHeader(
                        "Life focus", "lifeFocus",
                        listOf("Daily balance", "Career focus", "Family first", "Health reset"),
                        selections,
                    )
                    PersonalSetupDualPills(
                        "Current rhythm", "currentRhythm",
                        listOf("Busy but manageable", "Building steadiness"),
                        selections, accent,
                    )
                    PersonalSetupFieldLabel("YOUR FOUNDATION", small = true)
                    PersonalSetupInlineDropdown(
                        "Life focus", "What needs attention?", "lifeFocus",
                        listOf("Daily balance", "Career focus", "Family first", "Health reset"),
                        selections, accent,
                    )
                    PersonalSetupInlineDropdown(
                        "Current pressure", "How does life feel?", "currentRhythm",
                        listOf("Busy but manageable", "Overwhelmed", "Calm", "Building steadiness"),
                        selections, accent,
                    )
                    PersonalSetupInlineDropdown(
                        "Primary need", "What would help most?", "primaryNeed",
                        listOf("More breathing room", "Clearer structure", "More energy", "Better support"),
                        selections, accent,
                    )
                    PersonalSetupFieldLabel("YOUR PRIORITIES", small = true)
                    PersonalSetupInlineDropdown(
                        "Health & energy", "Feel stronger day to day", "healthEnergy",
                        listOf("Selected", "Not selected"), selections, accent,
                    )
                    PersonalSetupInlineDropdown(
                        "Time & balance", "Create room for what matters", "timeBalance",
                        listOf("Selected", "Not selected"), selections, accent,
                    )
                }
            }

            PersonalSetupDiamondDivider(accent)

            Box(
                modifier = Modifier.onGloballyPositioned { coords ->
                    sectionOffsets["02"] = coords.positionInParent().y.roundToInt()
                },
            ) {
                PersonalSetupSectionCard(number = "02", title = "Pressures & Supports", accent = accent) {
                    PersonalSetupBorderedGroup(
                        title = "What Shapes Your Days",
                        border = PersonalSetupLongFormTokens.Teal,
                        glyph = "▣",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Life focus", "Your chosen priority", "shapesFocus",
                            listOf("Daily balance", "Career focus", "Family first", "Health reset"),
                            selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                        )
                        PersonalSetupInlineDropdown(
                            "Current rhythm", "Your starting point", "shapesRhythm",
                            listOf("Busy but manageable", "Building steadiness", "Overwhelmed", "Calm"),
                            selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                        )
                    }
                    PersonalSetupBorderedGroup(
                        title = "Pressure",
                        border = PersonalSetupLongFormTokens.Orange,
                        glyph = "⚠",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Main pressure", "What drains you now", "mainPressure",
                            listOf("Too many commitments", "Money stress", "Health load", "Work overload"),
                            selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                        )
                        PersonalSetupInlineDropdown(
                            "Recovery window", "When you can reset", "recoveryWindow",
                            listOf("Weekends", "Evenings", "Mornings", "Anytime"),
                            selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                        )
                        PersonalSetupInlineDropdown(
                            "Check-in rhythm", "How often to pause", "checkInRhythm",
                            listOf("Weekly", "Daily", "Monthly"),
                            selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                        )
                    }
                    PersonalSetupBorderedGroup(
                        title = "Support & Recovery",
                        border = PersonalSetupLongFormTokens.Green,
                        glyph = "♡",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Helpful support", "What makes life easier", "helpfulSupport",
                            listOf("Clear routines", "Accountability", "Quiet mornings", "Community"),
                            selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                        )
                        PersonalSetupInlineDropdown(
                            "Recovery style", "How you recharge", "recoveryStyle",
                            listOf("Quiet time", "Walks", "Sleep", "Movement", "Nature"),
                            selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                        )
                    }
                }
            }

            PersonalSetupDiamondDivider(accent)

            Box(
                modifier = Modifier.onGloballyPositioned { coords ->
                    sectionOffsets["03"] = coords.positionInParent().y.roundToInt()
                },
            ) {
                PersonalSetupSectionCard(number = "03", title = "Habits & Energy", accent = accent) {
                    PersonalSetupBorderedGroup(
                        title = "Energy Anchors",
                        border = PersonalSetupLongFormTokens.Pink,
                        glyph = "⚡",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Morning routine", "Selected anchor", "habit",
                            listOf("Morning routine", "Planning routine", "Evening wind-down", "Movement block"),
                            selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                        )
                        if (showHabit2 || selectionString(selections, "habit2").isNotBlank()) {
                            PersonalSetupInlineDropdown(
                                "Second habit", "Another energy anchor", "habit2",
                                listOf("Morning routine", "Planning routine", "Evening wind-down", "Movement block"),
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
                        title = "Wellbeing",
                        border = PersonalSetupLongFormTokens.Blue,
                        glyph = "◇",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Current energy", "How you feel most days", "currentEnergy",
                            listOf("Steady", "Low", "High", "Variable"),
                            selections, PersonalSetupLongFormTokens.Blue, PersonalSetupLongFormTokens.Blue,
                        )
                        PersonalSetupToggleRow(
                            "Remind me weekly",
                            "Keep a light pulse check",
                            selections["reflectWeekly"] as? Boolean == true,
                            { selections["reflectWeekly"] = it },
                            PersonalSetupLongFormTokens.Blue,
                        )
                    }
                    PersonalSetupBorderedGroup(
                        title = "Reflection Preferences",
                        border = PersonalSetupLongFormTokens.Indigo,
                        glyph = "✦",
                    ) {
                        PersonalSetupInlineDropdown(
                            "Stress check-in", "Notice pressure early", "stressCheckIn",
                            listOf("Enabled", "Disabled"),
                            selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                        )
                        PersonalSetupInlineDropdown(
                            "Recovery check-in", "Protect time to recharge", "recoveryCheckIn",
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
            }

            PersonalSetupDiamondDivider(accent)

            Box(
                modifier = Modifier.onGloballyPositioned { coords ->
                    sectionOffsets["04"] = coords.positionInParent().y.roundToInt()
                },
            ) {
                PersonalSetupSectionCard(number = "04", title = "Life Summary", accent = accent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, accent, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        PersonalSetupSummaryRow("Life focus", selectionString(selections, "lifeFocus"), big = true)
                        PersonalSetupSummaryRow("Current rhythm", selectionString(selections, "currentRhythm"))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalSetupLongFormTokens.Border))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Energy",
                                color = PersonalSetupLongFormTokens.Green,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                selectionString(selections, "currentEnergy"),
                                color = PersonalSetupLongFormTokens.Blue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        PersonalSetupSummaryProgress(Color(0xFFA286FA))
                    }
                }
            }

            PersonalSetupActivateFooter(
                statusLine = "$sectionsConfigured sections configured • $answersSaved answers saved",
                readyLine = "Your life plan is ready",
                ctaLabel = catalog.activateLabel,
                footerTagline = catalog.footerTagline,
                ctaBrush = Brush.horizontalGradient(listOf(accent, Color(0xFFF472B6))),
                submitting = createState.submitting,
                error = createState.error,
                onActivate = {
                    if (createState.submitting) return@PersonalSetupActivateFooter
                    createViewModel.submitPersonalSetup(
                        kind = PersonalSetupKind.LIFE_OPERATIONS,
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
