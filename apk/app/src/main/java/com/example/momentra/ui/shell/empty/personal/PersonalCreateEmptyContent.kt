package com.example.momentra.ui.shell.empty.personal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.ui.shell.personal.shared.personalPulseFamilyFor
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Figma `353:452` — Personal Create body only (shell chrome stays in AppShell).
 * Setup wizards open as a bottom sheet popup over Create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalCreateEmptyContent(
    history: List<MomentSummary> = emptyList(),
    onMomentCreated: (momentId: String, title: String, momentTypeCode: String?) -> Unit = { _, _, _ -> },
    onOpenExisting: (momentId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var wizard by remember { mutableStateOf<PersonalSetupSystem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun activeMoment(system: PersonalSetupSystem): MomentSummary? =
        history.firstOrNull {
            it.isActiveStatus() && personalPulseFamilyFor(it.momentTypeCode) == system.toPulseFamily()
        }

    fun selectOrCreate(system: PersonalSetupSystem) {
        val existing = activeMoment(system)
        if (existing != null) {
            onOpenExisting(existing.momentId)
        } else {
            wizard = system
        }
    }

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.PERSONAL_CREATE)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.PERSONAL_CREATE) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PersonalCreateChooser(
            history = history,
            activeMomentFor = { activeMoment(it) },
            onSelect = { selectOrCreate(it) },
            modifier = Modifier.fillMaxSize(),
        )

        wizard?.let { system ->
            ModalBottomSheet(
                onDismissRequest = { wizard = null },
                sheetState = sheetState,
                containerColor = PeBg,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.28f)),
                    )
                },
            ) {
                PersonalSetupWizardContent(
                    system = system,
                    onBack = { wizard = null },
                    onCreated = { id, title, typeCode ->
                        wizard = null
                        onMomentCreated(id, title, typeCode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun PersonalCreateChooser(
    history: List<MomentSummary>,
    activeMomentFor: (PersonalSetupSystem) -> MomentSummary?,
    onSelect: (PersonalSetupSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    PeAppear {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PeBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Create a Moment",
                    color = PeText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Choose a life system to begin",
                    color = PeSubtle,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LifeSystemCard(
                        title = "Life Operations",
                        subtitle = if (activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null) {
                            "Open existing"
                        } else {
                            "Daily commitments & money"
                        },
                        subtitleAccent = activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null,
                        glyph = "▣",
                        accent = PePurple,
                        accentDeep = Color(0xFF4F46E5),
                        thumbRes = R.drawable.personal_create_thumb_life_ops,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(PersonalSetupSystem.LIFE_OPERATIONS) },
                    )
                    LifeSystemCard(
                        title = "Future Building",
                        subtitle = if (activeMomentFor(PersonalSetupSystem.FUTURE_BUILDING) != null) {
                            "Open existing"
                        } else {
                            "Goals, growth & progress"
                        },
                        subtitleAccent = activeMomentFor(PersonalSetupSystem.FUTURE_BUILDING) != null,
                        glyph = "↗",
                        accent = PeGreen,
                        accentDeep = Color(0xFF0F766E),
                        thumbRes = R.drawable.personal_create_thumb_future,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(PersonalSetupSystem.FUTURE_BUILDING) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LifeSystemCard(
                        title = "Lifestyle",
                        subtitle = if (activeMomentFor(PersonalSetupSystem.LIFESTYLE) != null) {
                            "Open existing"
                        } else {
                            "Experiences & wellbeing"
                        },
                        subtitleAccent = activeMomentFor(PersonalSetupSystem.LIFESTYLE) != null,
                        glyph = "◈",
                        accent = PeAmber,
                        accentDeep = Color(0xFFEA580C),
                        thumbRes = R.drawable.personal_create_thumb_lifestyle,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(PersonalSetupSystem.LIFESTYLE) },
                    )
                    LifeSystemCard(
                        title = "Relationships",
                        subtitle = if (activeMomentFor(PersonalSetupSystem.RELATIONSHIPS) != null) {
                            "Open existing"
                        } else {
                            "Care & shared moments"
                        },
                        subtitleAccent = activeMomentFor(PersonalSetupSystem.RELATIONSHIPS) != null,
                        glyph = "♡",
                        accent = PePink,
                        accentDeep = Color(0xFFBE185D),
                        thumbRes = R.drawable.personal_create_thumb_relationships,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(PersonalSetupSystem.RELATIONSHIPS) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚡", color = PePurple, fontSize = 16.sp)
                    Text(
                        "Quick Start",
                        color = PeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                QuickStartRow(
                    emoji = "☀️",
                    title = "Morning check-in",
                    subtitle = if (activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null) {
                        "Open existing Life Operations"
                    } else {
                        "How are you feeling today?"
                    },
                    cta = if (activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null) "Open" else "Log",
                    ctaColor = PePurple,
                    onClick = { onSelect(PersonalSetupSystem.LIFE_OPERATIONS) },
                )
                QuickStartRow(
                    emoji = "💰",
                    title = "Track expense",
                    subtitle = if (activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null) {
                        "Open existing Life Operations"
                    } else {
                        "Record a transaction"
                    },
                    cta = if (activeMomentFor(PersonalSetupSystem.LIFE_OPERATIONS) != null) "Open" else "Add",
                    ctaColor = PeGreen,
                    onClick = { onSelect(PersonalSetupSystem.LIFE_OPERATIONS) },
                )
                QuickStartRow(
                    emoji = "🤝",
                    title = "Log connection",
                    subtitle = if (activeMomentFor(PersonalSetupSystem.RELATIONSHIPS) != null) {
                        "Open existing Relationships"
                    } else {
                        "Capture a shared moment"
                    },
                    cta = if (activeMomentFor(PersonalSetupSystem.RELATIONSHIPS) != null) "Open" else "Start",
                    ctaColor = PeAmber,
                    onClick = { onSelect(PersonalSetupSystem.RELATIONSHIPS) },
                    showDivider = false,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, PePurple, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(PePurple, Color(0xFF4F46E5))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Tip: Start with what's on your mind right now.\nMomentra adapts to you.",
                    color = PeSecondary,
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = PlusJakartaSans,
                    lineHeight = 20.sp,
                )
            }

            if (history.isNotEmpty()) {
                PersonalHistoryBlock(title = "Past moments", history = history)
            }
        }
    }
}

@Composable
private fun LifeSystemCard(
    title: String,
    subtitle: String,
    glyph: String,
    accent: Color,
    accentDeep: Color,
    @DrawableRes thumbRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleAccent: Boolean = false,
) {
    Column(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PeCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .semantics {
                role = Role.Button
                contentDescription = title
            }
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(thumbRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(accent, accentDeep))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(glyph, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    title,
                    color = PeText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                subtitle,
                color = if (subtitleAccent) accent else PeSubtle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent),
        )
    }
}

@Composable
private fun QuickStartRow(
    emoji: String,
    title: String,
    subtitle: String,
    cta: String,
    ctaColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(emoji, fontSize = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        color = PeText,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        subtitle,
                        color = PeSubtle,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ctaColor)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    cta,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.04f)),
            )
        }
    }
}
