package com.example.momentra.ui.shell.personal.lifeops.pulse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ShellTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import com.example.momentra.ui.shell.personal.future.create.FutureQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.LifeOpsQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived
import com.example.momentra.ui.shell.personal.shared.LifestyleQuickAddKind
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.personal.shared.PersonalPulseFamily
import com.example.momentra.ui.shell.personal.shared.personalPulseFamilyFor
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache
import com.example.momentra.ui.shell.personal.shared.theme
import com.example.momentra.ui.shell.personal.shared.heroBrush

private val PulseBg = Color(0xFF14121B)
private val PulseCard = Color(0xFF1A1726)
private val PulseText = Color(0xFFE5E0EE)
private val PulseMuted = Color(0xFFC9C4D8)
private val PulsePurple = Color(0xFF7C5CFC)
private val PulsePurpleSoft = Color(0xFFA78BFA)
private val PulseCyan = Color(0xFF4CD6FF)
private val PulseBlue = Color(0xFF2196F3)
private val PulseOrange = Color(0xFFFF9800)
private val PulseGreen = Color(0xFF10B981)
private val PulseRed = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

@Composable
fun PersonalPulseActiveContent(
    refreshToken: Long,
    momentTitle: String?,
    momentId: String?,
    momentTypeCode: String? = null,
    onAddExpense: () -> Unit,
    onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Unit = {},
    onFutureQuickAdd: (FutureQuickAddKind) -> Unit = {},
    onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val family = personalPulseFamilyFor(momentTypeCode)
    val theme = family.theme()
    val isLifeOps = family == PersonalPulseFamily.LIFE_OPERATIONS
    val isFuture = family == PersonalPulseFamily.FUTURE_BUILDING
    val isLifestyle = family == PersonalPulseFamily.LIFESTYLE
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<PersonalPulseDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        error = null
        PersonalTabDataCache.peek(momentId)?.let { cached ->
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } ?: run { loading = true }
        loadPersonalPulseTab(repository, momentId).fold(
            onSuccess = { (p, items) ->
                pulse = p
                activities = items
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PulsePurple)
        }
        return
    }

    val spend = (pulse?.widgetPayload?.get("spendByCurrency") as? Map<*, *>)
        ?.entries
        ?.mapNotNull { e ->
            val code = e.key?.toString() ?: return@mapNotNull null
            val amount = e.value?.toString() ?: return@mapNotNull null
            code to amount
        }
        .orEmpty()
    val hideBalances = SecurityPreferences(LocalContext.current).hideBalances()
    val wellbeing = PersonalLifeOpsDerived.displayScore(pulse?.wellbeingScore)
    val recovery = PersonalLifeOpsDerived.displayScore(pulse?.recoveryScore)
    val rhythm = PersonalLifeOpsDerived.displayScore(pulse?.rhythmScore)
    val attention = if (isLifeOps || isFuture || isLifestyle) {
        PersonalLifeOpsDerived.attentionDisplay(pulse?.attentionCount)
    } else {
        pulse?.attentionCount?.takeIf { it > 0 }?.toString() ?: "—"
    }
    val mood = pulse?.moodState?.takeIf { it.isNotBlank() } ?: "—"
    val pressure = PersonalLifeOpsDerived.pressure(pulse?.recoveryScore)
    val vision = wellbeing
    val growth = recovery
    val momentum = rhythm
    val discipline = attention
    val joy = recovery
    val fulfillment = wellbeing
    val vitality = rhythm
    val exploration = attention
    val streak = PersonalLifeOpsDerived.streakDays(activities.map { it.occurredAt })
    val todayLabel = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
        .format(java.time.LocalDate.now())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        error?.let {
            Text(it, color = PulseRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        // Family hero
        val heroValues = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(pressure, recovery, rhythm, attention)
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(vision, growth, momentum, discipline)
            PersonalPulseFamily.LIFESTYLE -> listOf(joy, fulfillment, vitality, exploration)
            else -> listOf(recovery, rhythm, attention, mood)
        }
        val tileValues = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(pressure, recovery, rhythm, attention)
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(vision, growth, momentum, discipline)
            PersonalPulseFamily.LIFESTYLE -> listOf(joy, fulfillment, vitality, exploration)
            else -> listOf(
                recovery,
                attention,
                rhythm,
                "${pulse?.activeMomentCount ?: 0}",
            )
        }
        val tileAccents = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
                PulsePurple, PulseCyan, PulseBlue, PulseOrange,
            )
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(
                PulsePurple, PulseCyan, PulseBlue, PulseOrange,
            )
            PersonalPulseFamily.LIFESTYLE -> listOf(
                Color(0xFFEC4899), Color(0xFFA78BFA), PulseGreen, Color(0xFFF59E0B),
            )
            else -> listOf(PulseCyan, PulseOrange, PulseBlue, theme.accent)
        }
        val pressureRaw = PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore)?.let { (100.0 - it).toString() }
        val tileProgress = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
                scoreFraction(pressureRaw),
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.rhythmScore),
                if (attention != "—") min(1f, (pulse?.attentionCount ?: 0) / 10f) else 0f,
            )
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(
                scoreFraction(pulse?.wellbeingScore),
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.rhythmScore),
                if (attention != "—") min(1f, (pulse?.attentionCount ?: 0) / 10f) else 0f,
            )
            PersonalPulseFamily.LIFESTYLE -> listOf(
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.wellbeingScore),
                scoreFraction(pulse?.rhythmScore),
                if (attention != "—") min(1f, (pulse?.attentionCount ?: 0) / 10f) else 0f,
            )
            else -> listOf(
                scoreFraction(pulse?.recoveryScore),
                if (attention != "—") min(1f, (pulse?.attentionCount ?: 0) / 10f) else 0f,
                scoreFraction(pulse?.rhythmScore),
                min(1f, (pulse?.activeMomentCount ?: 0) / 4f),
            )
        }
        val tileBadges = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
                PersonalLifeOpsDerived.statusBadge(pressureRaw, "pressure"),
                PersonalLifeOpsDerived.statusBadge(pulse?.recoveryScore, "recovery"),
                PersonalLifeOpsDerived.statusBadge(pulse?.rhythmScore, "discipline"),
                PersonalLifeOpsDerived.statusBadge(attention.takeIf { it != "—" }, "attention"),
            )
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(
                PersonalLifeOpsDerived.statusBadge(pulse?.wellbeingScore, "vision"),
                PersonalLifeOpsDerived.statusBadge(pulse?.recoveryScore, "growth"),
                PersonalLifeOpsDerived.statusBadge(pulse?.rhythmScore, "discipline"),
                PersonalLifeOpsDerived.statusBadge(attention.takeIf { it != "—" }, "attention"),
            )
            PersonalPulseFamily.LIFESTYLE -> listOf(
                PersonalLifeOpsDerived.statusBadge(pulse?.recoveryScore, "recovery"),
                PersonalLifeOpsDerived.statusBadge(pulse?.wellbeingScore, "discipline"),
                PersonalLifeOpsDerived.statusBadge(pulse?.rhythmScore, "discipline"),
                PersonalLifeOpsDerived.statusBadge(attention.takeIf { it != "—" }, "attention"),
            )
            else -> tileValues.map { if (it != "—" && it != "0") "Live" else "Empty" }
        }
        val tileIcons = when (family) {
            PersonalPulseFamily.LIFE_OPERATIONS -> listOf(
                R.drawable.ic_pulse_zap,
                R.drawable.ic_pulse_activity,
                R.drawable.ic_pulse_target,
                R.drawable.ic_pulse_trending,
            )
            PersonalPulseFamily.FUTURE_BUILDING -> listOf(
                R.drawable.ic_pulse_zap,
                R.drawable.ic_pulse_activity,
                R.drawable.ic_pulse_trending,
                R.drawable.ic_pulse_target,
            )
            else -> listOf(
                R.drawable.ic_pulse_activity,
                R.drawable.ic_pulse_target,
                R.drawable.ic_pulse_trending,
                R.drawable.ic_pulse_zap,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.heroBrush())
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        theme.heroTitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        wellbeing,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            if (wellbeing != "—") theme.heroSubtitleFilled else theme.heroSubtitleEmpty,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    if (streak > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(PulseGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "$streak Day Streak",
                                color = PulseGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_pulse_zap),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            todayLabel,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                theme.heroMetrics.forEachIndexed { index, label ->
                    HeroMetricChip(label, heroValues.getOrElse(index) { "—" }, Modifier.weight(1f))
                }
            }
        }

        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            theme.tileLabels.take(2).forEachIndexed { index, label ->
                MetricTile(
                    label = label,
                    value = tileValues[index],
                    accent = tileAccents[index],
                    iconRes = tileIcons[index],
                    badge = tileBadges.getOrElse(index) {
                        if (tileValues[index] != "—" && tileValues[index] != "0") "Live" else "Empty"
                    },
                    progress = tileProgress[index],
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            theme.tileLabels.drop(2).forEachIndexed { i, label ->
                val index = i + 2
                MetricTile(
                    label = label,
                    value = tileValues[index],
                    accent = tileAccents[index],
                    iconRes = tileIcons[index],
                    badge = tileBadges.getOrElse(index) {
                        if (tileValues[index] != "—" && tileValues[index] != "0") "Live" else "Empty"
                    },
                    progress = tileProgress[index],
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Today's Momentum — Figma 353:8893 four pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14121C))
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_pulse_zap),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "TODAY'S MOMENTUM",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MomentumPill(
                    label = when {
                        isFuture -> if (growth == "—") "Career pending" else "Career Rising"
                        isLifestyle -> if (joy == "—") "Joy pending" else "Joy Rising"
                        else -> if (recovery == "—") "Recovery pending" else "Recovery Rising"
                    },
                    tint = PulseGreen,
                    modifier = Modifier.weight(1f),
                )
                MomentumPill(
                    label = when {
                        isFuture -> if (discipline == "—") "Skills quiet" else "Skills Improving"
                        isLifestyle -> if (vitality == "—") "Ritual quiet" else "Ritual Steady"
                        else -> if (pressure == "—") "Pressure quiet" else "Pressure Stable"
                    },
                    tint = PulseOrange,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MomentumPill(
                    label = when {
                        isFuture -> if (spend.isEmpty()) "Savings quiet" else "Savings Strong"
                        else -> if (mood == "—") "Mood pending" else "Mood · $mood"
                    },
                    tint = if (isFuture && spend.isEmpty()) PulseOrange else PulseGreen,
                    modifier = Modifier.weight(1f),
                )
                MomentumPill(
                    label = when {
                        isFuture -> if (momentum == "—") "Network quiet" else "Network Growing"
                        else -> if (spend.isEmpty()) "Budget quiet" else "Budget Strong"
                    },
                    tint = when {
                        isFuture -> PulseGreen
                        spend.isEmpty() -> PulseOrange
                        else -> PulseGreen
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (isLifeOps || isFuture || isLifestyle) {
            val (helping, hurting) = PersonalLifeOpsDerived.helpingHurting(
                activities.map { it.activityCode to it.title },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DriverColumn(
                    title = "HELPING",
                    tint = PulseGreen,
                    items = helping.map { it.label },
                    empty = when {
                        isFuture -> "Log a milestone or learning"
                        isLifestyle -> "Log an experience or wellbeing"
                        else -> "Log recovery or mood"
                    },
                    modifier = Modifier.weight(1f),
                )
                DriverColumn(
                    title = "HURTING",
                    tint = PulseRed,
                    items = hurting.map { it.label },
                    empty = if (isFuture || isLifestyle) "No drag signals" else "No pressure signals",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Recent Activity — Figma 1009:7590
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "RECENT ACTIVITY",
                    color = PulseMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "View All",
                    color = PulsePurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onViewAllActivity),
                )
            }
            if (activities.isEmpty()) {
                Text(
                    "No activity yet.",
                    color = PulseMuted,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                activities.take(8).forEachIndexed { index, item ->
                    ActivityRowFigma(
                        item = item,
                        showDivider = index < activities.take(8).lastIndex,
                    )
                }
            }
        }

        // Money Snapshot — real spendByCurrency
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    theme.moneyTitle,
                    color = PulseMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (spend.isEmpty()) "—" else spend.joinToString(" · ") {
                        "${it.first} ${BalanceMask.mask(formatMoney(it.second), hideBalances)}"
                    },
                    color = PulseText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            if (spend.isEmpty()) {
                Text(
                    "No spend recorded for this moment yet.",
                    color = PulseMuted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                spend.forEach { (currency, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(currency, color = PulseText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(formatMoney(amount), hideBalances),
                            color = PulseText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        // Smart Nudge — family copy; CTA visual until matching write API exists
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(theme.heroStart, theme.heroEnd)))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_pulse_shield),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    theme.nudgeTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                theme.nudgeBody,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(enabled = (isLifeOps || isFuture || isLifestyle) && momentId != null) {
                        when {
                            isLifeOps -> onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY)
                            isFuture -> onFutureQuickAdd(FutureQuickAddKind.MILESTONE)
                            isLifestyle -> onLifestyleQuickAdd(LifestyleQuickAddKind.EXPERIENCE)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    theme.nudgeCta,
                    color = PulsePurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        // AI Insights — Figma Coming Soon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AI Insights",
                    color = PulseText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(PulsePurple.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Coming Soon",
                        color = PulsePurpleSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Text(
                when {
                    isFuture -> "Patterns across milestones, learning, progress, and capital will surface here."
                    isLifestyle -> "Patterns across experiences, wellbeing, discovery, and spend will surface here."
                    else -> "Patterns across pressure, recovery, mood, and money will surface here."
                },
                color = PulseMuted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        // Quick action row — Money opens expense when present for this family
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val actionColors = listOf(PulseCyan, PulseOrange, PulsePurpleSoft, PulseGreen, PulseMuted)
            theme.quickActions.forEachIndexed { index, label ->
                val icon = quickActionIcon(label)
                val tint = actionColors.getOrElse(index) { PulseMuted }
                val onClick: (() -> Unit)? = when (label) {
                    "Money" -> onAddExpense
                    "Recovery" -> ({ onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY) })
                    "Mood" -> ({ onLifeOpsQuickAdd(LifeOpsQuickAddKind.MOOD) })
                    "Attention" -> ({ onLifeOpsQuickAdd(LifeOpsQuickAddKind.ATTENTION) })
                    "Adjust" -> if (isLifestyle) {
                        ({ onLifestyleQuickAdd(LifestyleQuickAddKind.ADJUST) })
                    } else {
                        ({ onLifeOpsQuickAdd(LifeOpsQuickAddKind.ADJUST) })
                    }
                    "Milestone" -> ({ onFutureQuickAdd(FutureQuickAddKind.MILESTONE) })
                    "Opportunity" -> ({ onFutureQuickAdd(FutureQuickAddKind.OPPORTUNITY) })
                    "Pivot" -> ({ onFutureQuickAdd(FutureQuickAddKind.PIVOT) })
                    "Progress" -> ({ onFutureQuickAdd(FutureQuickAddKind.PROGRESS) })
                    "Learning" -> ({ onFutureQuickAdd(FutureQuickAddKind.LEARNING) })
                    "Experience" -> ({ onLifestyleQuickAdd(LifestyleQuickAddKind.EXPERIENCE) })
                    "Wellbeing" -> ({ onLifestyleQuickAdd(LifestyleQuickAddKind.WELLBEING) })
                    "Discovery" -> ({ onLifestyleQuickAdd(LifestyleQuickAddKind.DISCOVERY) })
                    "Create", "Expression" -> ({ onLifestyleQuickAdd(LifestyleQuickAddKind.EXPRESSION) })
                    else -> null
                }
                QuickActionDot(
                    label = label,
                    tint = tint,
                    iconRes = icon,
                    onClick = onClick,
                    enabled = momentId != null || onClick == null,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HeroMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    iconRes: Int,
    badge: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(accent),
                )
                Spacer(Modifier.width(6.dp))
                Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            }
        }
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun DriverColumn(
    title: String,
    tint: Color,
    items: List<String>,
    empty: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        if (items.isEmpty()) {
            Text(empty, color = PulseMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        } else {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(tint),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item,
                        color = PulseText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentumPill(label: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_pulse_arrow_up),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun ActivityRowFigma(item: ActivityItemDto, showDivider: Boolean) {
    val isExpense = item.activityCode.contains("EXPENSE", ignoreCase = true)
    val badgeColor = if (isExpense) PulseRed else PulseCyan
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (isExpense) R.drawable.ic_pulse_cart else R.drawable.ic_pulse_zap,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = PulseText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    formatOccurredAt(item.occurredAt),
                    color = PulseMuted,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    item.activityCode.replace('_', ' ').lowercase(Locale.getDefault())
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    color = PulseBg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f)),
            )
        }
    }
}

@Composable
private fun QuickActionDot(
    label: String,
    tint: Color,
    iconRes: Int,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f))
                .border(1.dp, tint.copy(alpha = 0.3f), CircleShape)
                .then(
                    if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                    else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(tint),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = PulseMuted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
    }
}

private fun quickActionIcon(label: String): Int = when (label.lowercase()) {
    "recovery" -> R.drawable.ic_pulse_activity
    "attention" -> R.drawable.ic_pulse_target
    "mood", "reflection", "reflect" -> R.drawable.ic_pulse_smile
    "money" -> R.drawable.ic_money_wallet
    "adjust", "inbox" -> R.drawable.ic_pulse_settings
    "create", "expression", "milestone", "progress", "growth", "learning", "opportunity", "post" -> R.drawable.ic_pulse_trending
    "experience", "wellbeing", "discovery" -> R.drawable.ic_pulse_smile
    "chat", "check-in", "plan", "support", "presence" -> R.drawable.ic_pulse_smile
    else -> R.drawable.ic_pulse_zap
}

private fun displayScore(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return raw.trim().removeSuffix(".0").removeSuffix(".00")
}

/** Figma Pressure tile — inverse of recovery when score is present. */
private fun pressureDisplay(recoveryRaw: String?): String {
    if (recoveryRaw.isNullOrBlank()) return "—"
    val n = recoveryRaw.trim().removeSuffix(".0").removeSuffix(".00").toDoubleOrNull() ?: return "—"
    return (100.0 - n).coerceIn(0.0, 100.0).toInt().toString()
}

private fun scoreFraction(raw: String?): Float {
    val n = raw?.toFloatOrNull() ?: return 0f
    return (n / 100f).coerceIn(0f, 1f)
}

private fun formatMoney(amount: String): String {
    val n = amount.toDoubleOrNull() ?: return amount
    return String.format(Locale.getDefault(), "%,.2f", n)
}

private fun formatOccurredAt(iso: String): String = try {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    iso
}
