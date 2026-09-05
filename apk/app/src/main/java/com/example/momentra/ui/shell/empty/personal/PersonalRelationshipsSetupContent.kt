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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.data.api.ApiClient
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Figma `353:7217` — full Relationships setup body. */
@Composable
fun PersonalRelationshipsSetupContent(
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    val catalog = remember { PersonalSetupCatalog.forKind(PersonalSetupKind.RELATIONSHIPS) }
    val selections = remember {
        mutableStateMapOf<String, Any>().apply { putAll(catalog.defaultPreferences) }
    }
    var momentTitle by remember { mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: catalog.defaultTitle) }
    var showHabit2 by remember { mutableStateOf(false) }
    val createState by createViewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val accent = PersonalSetupLongFormTokens.Pink

    val sectionKeys = listOf(
        setOf("relationshipFocus", "current", "primaryCircle", "partnerFamily", "friendsCommunity"),
        setOf(
            "relationshipFocus", "current", "timeTogether", "reachOutRhythm",
            "communicationStyle", "strongestConnection", "needsInvestment",
        ),
        setOf(
            "ritual", "habit2", "desiredFeeling", "remindWeekly",
            "connectionCheckIn", "reachOutReminder", "reviewCadence",
        ),
        setOf("relationshipFocus", "current", "primaryCircle", "profile"),
    )
    val (sectionsConfigured, answersSaved) = setupStatusCounts(
        defaults = catalog.defaultPreferences,
        selections = selections.toMap(),
        sectionKeys = sectionKeys,
    )

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.PERSONAL_SETUP_RELATIONSHIPS)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.PERSONAL_SETUP_RELATIONSHIPS) }
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
                emoji = "💞",
                title = "Set up Relationships",
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

            PersonalSetupSectionCard(number = "01", title = "Relationship Basics", accent = accent) {
                PersonalSetupStageHeader(
                    "Relationship focus", "relationshipFocus",
                    listOf("Deeper connection", "More presence", "Better repair", "Wider community"),
                    selections,
                )
                PersonalSetupDualPills(
                    "Current", "current",
                    listOf("Connected, but busy", "Making room for people"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR CONNECTIONS", small = true)
                PersonalSetupInlineDropdown(
                    "Relationship focus", "What do you want more of?", "relationshipFocus",
                    listOf("Deeper connection", "More presence", "Better repair", "Wider community"),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Current", "How connected do you feel?", "current",
                    listOf(
                        "Connected, but busy",
                        "Growing",
                        "Repairing",
                        "Distant",
                        "Deepening",
                    ),
                    selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Primary circle", "Who comes first?", "primaryCircle",
                    listOf("Family", "Friendship", "Partnership", "Community"),
                    selections, accent,
                )
                PersonalSetupFieldLabel("YOUR CIRCLES", small = true)
                PersonalSetupInlineDropdown(
                    "Partner & family", "Closest bonds", "partnerFamily",
                    listOf("Selected", "Not selected"), selections, accent,
                )
                PersonalSetupInlineDropdown(
                    "Friends & community", "Wider circle", "friendsCommunity",
                    listOf("Selected", "Not selected"), selections, accent,
                )
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "02", title = "People & Circles", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "Who Matters Most",
                    border = PersonalSetupLongFormTokens.Teal,
                    glyph = "▣",
                ) {
                    PersonalSetupInlineDropdown(
                        "Relationship focus", "Your chosen priority", "relationshipFocus",
                        listOf("Deeper connection", "More presence", "Better repair", "Wider community"),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                    PersonalSetupInlineDropdown(
                        "Current", "Your starting point", "current",
                        listOf(
                            "Connected, but busy",
                            "Making room for people",
                            "Growing",
                            "Repairing",
                            "Distant",
                            "Deepening",
                        ),
                        selections, PersonalSetupLongFormTokens.Teal, PersonalSetupLongFormTokens.Teal,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Connection",
                    border = PersonalSetupLongFormTokens.Orange,
                    glyph = "⚠",
                ) {
                    PersonalSetupInlineDropdown(
                        "Time together", "How you show up", "timeTogether",
                        listOf("Meaningful moments", "Quality time", "Fun", "Support"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                    PersonalSetupInlineDropdown(
                        "Reach-out rhythm", "How often you connect", "reachOutRhythm",
                        listOf("Every week", "Daily", "Monthly"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                    PersonalSetupInlineDropdown(
                        "Communication style", "How you relate", "communicationStyle",
                        listOf("Thoughtful check-ins", "Direct", "Playful", "Listening"),
                        selections, PersonalSetupLongFormTokens.Orange, PersonalSetupLongFormTokens.Orange,
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Strengths & Investment",
                    border = PersonalSetupLongFormTokens.Green,
                    glyph = "♡",
                ) {
                    PersonalSetupInlineDropdown(
                        "Strongest connection", "Where bonds are solid", "strongestConnection",
                        listOf("Family", "Friends", "Partner", "Community"),
                        selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                    )
                    PersonalSetupInlineDropdown(
                        "Needs investment", "Where to put more care", "needsInvestment",
                        listOf("Friends", "Family", "Partner", "Community"),
                        selections, PersonalSetupLongFormTokens.Green, PersonalSetupLongFormTokens.Green,
                    )
                }
            }

            PersonalSetupDiamondDivider(accent)

            PersonalSetupSectionCard(number = "03", title = "Care & Outlook", accent = accent) {
                PersonalSetupBorderedGroup(
                    title = "Connection Anchors",
                    border = PersonalSetupLongFormTokens.Pink,
                    glyph = "⚡",
                ) {
                    PersonalSetupInlineDropdown(
                        "Ritual", "Selected ritual", "ritual",
                        listOf("Weekly check-in", "Date night", "Call a friend", "Shared meal"),
                        selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                    )
                    if (showHabit2 || selectionString(selections, "habit2").isNotBlank()) {
                        PersonalSetupInlineDropdown(
                            "Second ritual", "Another connection ritual", "habit2",
                            listOf("Weekly check-in", "Date night", "Call a friend", "Shared meal"),
                            selections, PersonalSetupLongFormTokens.Pink, PersonalSetupLongFormTokens.Pink,
                        )
                    }
                    PersonalSetupAddRow(
                        "+ ADD another ritual",
                        PersonalSetupLongFormTokens.Pink,
                        onClick = { showHabit2 = true },
                    )
                }
                PersonalSetupBorderedGroup(
                    title = "Connection Outlook",
                    border = PersonalSetupLongFormTokens.Blue,
                    glyph = "◇",
                ) {
                    PersonalSetupInlineDropdown(
                        "Desired feeling", "How you want to feel", "desiredFeeling",
                        listOf("Close & supported", "Playful", "Calm", "Deep"),
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
                        "Connection check-in", "Notice closeness early", "connectionCheckIn",
                        listOf("Enabled", "Disabled"),
                        selections, PersonalSetupLongFormTokens.Indigo, PersonalSetupLongFormTokens.Indigo,
                    )
                    PersonalSetupInlineDropdown(
                        "Reach-out reminder", "Protect time for people", "reachOutReminder",
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

            PersonalSetupSectionCard(number = "04", title = "Relationships Summary", accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, accent, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PersonalSetupSummaryRow(
                        "Relationship focus",
                        selectionString(selections, "relationshipFocus"),
                        big = true,
                    )
                    PersonalSetupSummaryRow("Current", selectionString(selections, "current"))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalSetupLongFormTokens.Border))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Primary circle",
                            color = PersonalSetupLongFormTokens.Green,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            selectionString(selections, "primaryCircle"),
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
                readyLine = "Your relationship plan is ready",
                ctaLabel = catalog.activateLabel,
                footerTagline = catalog.footerTagline,
                ctaBrush = Brush.horizontalGradient(listOf(accent, Color(0xFFF472B6))),
                submitting = createState.submitting,
                error = createState.error,
                onActivate = {
                    if (createState.submitting) return@PersonalSetupActivateFooter
                    createViewModel.submitPersonalSetup(
                        kind = PersonalSetupKind.RELATIONSHIPS,
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
