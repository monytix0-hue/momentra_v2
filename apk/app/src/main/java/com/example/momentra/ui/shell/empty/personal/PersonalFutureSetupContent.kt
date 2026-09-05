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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.data.api.ApiClient
import com.example.momentra.analytics.MomentraAnalytics
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Figma `353:6905` — full Future Building setup body. */
@Composable
fun PersonalFutureSetupContent(
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    val catalog = remember { PersonalSetupCatalog.forKind(PersonalSetupKind.FUTURE_BUILDING) }
    val selections = remember {
        mutableStateMapOf<String, Any>().apply { putAll(catalog.defaultPreferences) }
    }
    var momentTitle by remember { mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: catalog.defaultTitle) }
    var showHabit2 by remember { mutableStateOf(false) }
    val createState by createViewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val accent = PersonalSetupLongFormTokens.Teal

    val sectionKeys = listOf(
        setOf("building", "today", "primaryValue", "valueGrowth", "valueSecurity"),
        setOf("building", "today", "futureFeel", "focusHorizon", "progressRhythm", "mainFriction", "supportStyle"),
        setOf(
            "momentumDriver", "habit2", "futureFeel", "remindWeekly",
            "learningCheckIn", "focusTimeCheckIn", "reviewCadence",
        ),
        setOf("building", "today", "futureFeel", "profile"),
    )
    val (sectionsConfigured, answersSaved) = setupStatusCounts(
        defaults = catalog.defaultPreferences,
        selections = selections.toMap(),
        sectionKeys = sectionKeys,
    )

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.PERSONAL_SETUP_FUTURE)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.PERSONAL_SETUP_FUTURE) }
    }

    LaunchedEffect(editingMomentId) {
        val momentId = editingMomentId ?: return@LaunchedEffect
        runCatching {
            ApiClient.apiService.getPersonalSetups().data.items.firstOrNull { it.momentId == momentId }
        }.getOrNull()?.let { setup ->
            selections.clear()
            selections.putAll(
                mergePersonalSetupPreferences(catalog.defaultPreferences, setup.preferences),
            )
            if (initialTitle.isNullOrBlank()) {
                momentTitle = setup.title
            }
            showHabit2 = selectionString(selections, "habit2").isNotBlank()
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
                emoji = "🌱",
                title = "Set up Future Building",
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

            PersonalSetupSectionCard(number = "01", title = "Building Basics", accent = accent) {
                PersonalSetupStageHeader(
                    "Building focus", "building",
                    listOf("Career growth", "Financial freedom", "Creative work", "Family chapter"),
                    selections,
                )
                PersonalSetupDualPills(
                    "Today", "today",
                    listOf("Making progress", "Building momentum"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR FOCUS", small = true)
                PersonalSetupInlineDropdown(
                    "Building", "What are you building?", "building",
                    listOf("Career growth", "Financial freedom", "Creative work", "Family chapter"),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Today", "Where are you now?", "today",
                    listOf("Making progress", "Just starting", "Stuck", "Restarting"),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Primary value", "What matters most?", "primaryValue",
                    listOf("Freedom", "Growth", "Stability", "Impact"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR VALUES", small = true)
                PersonalSetupInlineDropdown(
                    "Growth", "Lean into expansion", "valueGrowth",
                    listOf("Selected", "Not selected"), selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Security", "Protect what you've built", "valueSecurity",
                    listOf("Selected", "Not selected"), selections, accent,
                )
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "02", title = "Goals & Friction", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "What Matters Most",
                    border = PersonalSetupLongFormTokens.Teal,
                    glyph = "▣",
                ) {
                    PersonalSetupInlineDropdown(
                        "Building", "Your chosen direction", "building",
                        listOf("Career growth", "Financial freedom", "Creative work", "Family chapter"),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                    PersonalSetupInlineDropdown(
                        "Today", "Your starting point", "today",
                        listOf("Making progress", "Just starting", "Stuck", "Restarting", "Building momentum"),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Goals",
                    border = PersonalSetupLongFormTokens.Blue,
                    glyph = "◇",
                ) {
                    PersonalSetupInlineDropdown(
                        "Future feel", "How should the future feel?", "futureFeel",
                        listOf("Hopeful", "Confident", "Calm", "Ambitious", "Grounded"),
                        selections, PersonalSetupLongFormTokens.Blue, PersonalSetupLongFormTokens.Blue,
                    )
                    PersonalSetupInlineDropdown(
                        "Focus horizon", "How far ahead?", "focusHorizon",
                        listOf("12 months", "6 months", "3 months", "This year"),
                        selections, PersonalSetupLongFormTokens.Blue, PersonalSetupLongFormTokens.Blue,
                    )
                    PersonalSetupInlineDropdown(
                        "Progress rhythm", "How often you move", "progressRhythm",
                        listOf("Weekly", "Daily", "Monthly"),
                        selections, PersonalSetupLongFormTokens.Blue, PersonalSetupLongFormTokens.Blue,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Friction & Support",
                    border = PersonalSetupLongFormTokens.Indigo,
                    glyph = "⚠",
                ) {
                    PersonalSetupInlineDropdown(
                        "Main friction", "What slows you down", "mainFriction",
                        listOf("Lack of time", "Lack of clarity", "Overcommitment", "Fear", "Distraction"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                    PersonalSetupInlineDropdown(
                        "Support style", "What keeps you moving", "supportStyle",
                        listOf("Daily progress", "Focus time", "Accountability", "Learning"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                }
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "03", title = "Momentum & Outlook", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "Momentum Drivers",
                    border = PersonalSetupLongFormTokens.Pink,
                    glyph = "⚡",
                ) {
                    PersonalSetupInlineDropdown(
                        "Momentum driver", "Selected driver", "momentumDriver",
                        listOf("Daily progress", "Focus time", "Accountability", "Learning"),
                        selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                    )
                    if (showHabit2 || selectionString(selections, "habit2").isNotBlank()) {
                        PersonalSetupInlineDropdown(
                            "Second driver", "Another momentum driver", "habit2",
                            listOf("Daily progress", "Focus time", "Accountability", "Learning"),
                            selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                        )
                    }
                    PersonalSetupAddRow(
                        "+ ADD another driver",
                        PersonalSetupLongFormTokens.Pink,
                        onClick = { showHabit2 = true },
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Outlook",
                    border = PersonalSetupLongFormTokens.Blue,
                    glyph = "◇",
                ) {
                    PersonalSetupInlineDropdown(
                        "Future feel", "How you want to feel", "futureFeel",
                        listOf("Hopeful", "Confident", "Calm", "Ambitious", "Grounded"),
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
                        "Learning check-in", "Notice growth early", "learningCheckIn",
                        listOf("Enabled", "Disabled"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                    PersonalSetupInlineDropdown(
                        "Focus time check-in", "Protect deep work", "focusTimeCheckIn",
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

            PersonalSetupSectionCard(number = "04", title = "Future Summary", accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, accent, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PersonalSetupSummaryRow("Building", selectionString(selections, "building"), big = true)
                    PersonalSetupSummaryRow("Today", selectionString(selections, "today"))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalSetupLongFormTokens.Border))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Future feel",
                            color = PersonalSetupLongFormTokens.Green,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            selectionString(selections, "futureFeel"),
                            color = PersonalSetupLongFormTokens.Blue,
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
                readyLine = "Your future map is ready",
                ctaLabel = catalog.activateLabel,
                footerTagline = catalog.footerTagline,
                ctaBrush = Brush.horizontalGradient(listOf(accent, PersonalSetupLongFormTokens.Blue)),
                submitting = createState.submitting,
                error = createState.error,
                onActivate = {
                    if (createState.submitting) return@PersonalSetupActivateFooter
                    createViewModel.submitPersonalSetup(
                        kind = PersonalSetupKind.FUTURE_BUILDING,
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
