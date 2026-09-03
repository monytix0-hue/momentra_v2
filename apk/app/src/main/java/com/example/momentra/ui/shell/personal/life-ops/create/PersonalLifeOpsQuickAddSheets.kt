package com.example.momentra.ui.shell.personal.lifeops.create

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.data.api.LifeOpsPriorityWeightBody
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.momentra.ui.shell.personal.lifeops.create.observationType

enum class LifeOpsQuickAddKind {
    RECOVERY,
    MOOD,
    ATTENTION,
    ADJUST,
}

fun LifeOpsQuickAddKind.observationType(): String = when (this) {
    LifeOpsQuickAddKind.RECOVERY -> "RECOVERY"
    LifeOpsQuickAddKind.MOOD -> "MOOD"
    LifeOpsQuickAddKind.ATTENTION -> "RHYTHM"
    LifeOpsQuickAddKind.ADJUST -> "RHYTHM"
}

private val LoBg = Color(0xFF14121B)
private val LoSurface = Color(0xFF201E28)
private val LoElevated = Color(0xFF3A3842)
private val LoText = Color(0xFFE5E0EE)
private val LoSecondary = Color(0xFFC9C4D8)
private val LoMuted = Color(0xFFA099B0)
private val LoBorder = Color(0xFF938EA1)
private val LoAccent = Color(0xFF7C5CFC)
private val LoBrand = Color(0xFFC9BFFF)
private val LoGreen = Color(0xFF10B981)
private val LoCardBorder = Color.White.copy(alpha = 0.08f)
private val LoError = Color(0xFFF87171)
private val SaveGradient = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))

private data class LoEmojiChip(val emoji: String, val label: String)
private data class LoIconChip(val icon: String, val label: String)

@Composable
fun PersonalLifeOpsQuickAddSheet(
    kind: LifeOpsQuickAddKind,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    when (kind) {
        LifeOpsQuickAddKind.RECOVERY -> RecoverySheet(momentId, onClose, onSaved, repository, modifier)
        LifeOpsQuickAddKind.MOOD -> MoodSheet(momentId, onClose, onSaved, repository, modifier)
        LifeOpsQuickAddKind.ATTENTION -> AttentionSheet(momentId, onClose, onSaved, repository, modifier)
        LifeOpsQuickAddKind.ADJUST -> AdjustSheet(momentId, onClose, onSaved, repository, modifier)
    }
}

// region Shared primitives

@Composable
private fun IntelligenceOsHeader(onClose: () -> Unit, badge: String = "Runtime learning") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Intelligence OS",
                color = LoText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LoSurface)
                        .border(1.dp, LoCardBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LoGreen),
                    )
                    Text(
                        badge,
                        color = LoSecondary,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "Record what shapes how your day runs.",
            color = LoSecondary,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LoSectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoSurface)
            .border(1.dp, LoCardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        content()
    }
}

@Composable
private fun LoFieldLabel(text: String) {
    Text(
        text,
        color = LoBrand,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun LoGradientSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = value / 10f,
        animationSpec = spring(dampingRatio = 0.75f),
        label = "slider",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "$value",
                color = LoText,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text("/10", color = LoMuted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444)),
                    ),
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValueChange((offset.x / size.width * 10).roundToInt().coerceIn(0, 10))
                    }
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValueChange((change.position.x / size.width * 10).roundToInt().coerceIn(0, 10))
                    }
                },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-22).dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (animatedFraction * 300).dp.coerceAtMost(280.dp))
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, LoAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$value", color = LoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..10).forEach { i ->
                Text("$i", color = LoMuted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoEmojiChipRow(
    chips: List<LoEmojiChip>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            val isSelected = selected == chip.label
            val bg by animateColorAsState(if (isSelected) LoAccent else LoElevated, label = "chipBg")
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) LoAccent else LoBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(chip.label) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(chip.emoji, fontSize = 20.sp)
                Text(
                    chip.label,
                    color = if (isSelected) Color.White else LoText,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoTextChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    emojiPrefix: Map<String, String> = emptyMap(),
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = selected == option
            val bg by animateColorAsState(if (isSelected) LoAccent else LoElevated, label = "chip")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) LoAccent else LoBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                emojiPrefix[option]?.let { Text(it, fontSize = 14.sp) }
                Text(
                    option,
                    color = if (isSelected) Color.White else LoText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun LoNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Int = 100,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LoElevated)
            .border(1.dp, LoBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = LoSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = LoText, fontSize = 13.sp, fontFamily = PlusJakartaSans),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoSaveButton(
    label: String,
    enabled: Boolean,
    submitting: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clip(RoundedCornerShape(14.dp))
            .background(SaveGradient)
            .testTag(testTag)
            .clickable(enabled = enabled && !submitting, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (submitting) "Saving…" else label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

// endregion

// region Recovery — Figma `353:11408`

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecoverySheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier,
) {
    val activities = listOf("Walk", "Nap", "Meditate", "Stretch", "Sleep", "Social")
    var activity by remember { mutableStateOf("Walk") }
    var quality by remember { mutableIntStateOf(7) }
    var duration by remember { mutableStateOf("30 min") }
    var energy by remember { mutableIntStateOf(6) }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ringProgress by animateFloatAsState(
        targetValue = when (duration) {
            "15 min" -> 0.25f
            "60 min" -> 1f
            else -> 0.5f
        },
        animationSpec = spring(dampingRatio = 0.7f),
        label = "ring",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LoBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IntelligenceOsHeader(onClose)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoSectionCard {
                Text("Recovery", color = LoText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Capture rest and recharge activities.", color = LoSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            LoSectionCard {
                LoFieldLabel("ACTIVITY")
                Spacer(Modifier.height(10.dp))
                LoTextChipRow(activities, activity, onSelect = { activity = it })
            }
            LoSectionCard {
                LoFieldLabel("QUALITY")
                Spacer(Modifier.height(10.dp))
                LoGradientSlider(quality, onValueChange = { quality = it })
            }
            LoSectionCard {
                LoFieldLabel("DURATION")
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            drawArc(color = LoElevated, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(color = LoAccent, startAngle = -90f, sweepAngle = 360f * ringProgress, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text(duration.replace(" min", "m"), color = LoText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("15 min", "30 min", "60 min").forEach { d ->
                            val sel = duration == d
                            Text(
                                d,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) LoAccent else LoElevated)
                                    .clickable { duration = d }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (sel) Color.White else LoText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            LoSectionCard {
                LoFieldLabel("ENERGY RESTORED")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(10) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i < energy) LoGreen else LoElevated)
                                .clickable { energy = i + 1 },
                        )
                    }
                }
            }
            LoSectionCard {
                LoFieldLabel("NOTES")
                Spacer(Modifier.height(8.dp))
                LoNoteField(note, { note = it }, "How did this recovery feel?", modifier = Modifier.testTag(MaestroIds.PERSONAL_LIFEOPS_RECOVERY_NOTE))
            }
            error?.let { Text(it, color = LoError, fontSize = 12.sp) }
            LoSaveButton("Save Recovery", !submitting, submitting, MaestroIds.PERSONAL_LIFEOPS_RECOVERY_SUBMIT) {
                submitting = true
                error = null
                scope.launch {
                    repository.recordObservation(
                        momentId = momentId,
                        observationType = LifeOpsQuickAddKind.RECOVERY.observationType(),
                        numericValue = quality.toDouble(),
                        textValue = "$activity · $duration · energy:$energy",
                        note = note.ifBlank { null },
                        activityTypeCode = when (activity) {
                            "Walk", "Stretch" -> "EXERCISE"
                            "Nap", "Sleep" -> "SLEEP"
                            "Meditate" -> "MEDITATION"
                            "Social" -> "SOCIAL"
                            else -> "OTHER"
                        },
                        durationMinutes = when (duration) {
                            "15 min" -> 15
                            "60 min" -> 60
                            "2 hr" -> 120
                            else -> 30
                        },
                        energyAfterPct = energy * 10.0,
                    ).fold(
                        onSuccess = { submitting = false; onSaved() },
                        onFailure = { e -> submitting = false; error = e.message },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// endregion

// region Mood — Figma `353:11452`

@Composable
private fun MoodSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier,
) {
    val feelings = listOf(
        LoEmojiChip("😄", "Great"), LoEmojiChip("😌", "Calm"), LoEmojiChip("😐", "Neutral"),
        LoEmojiChip("😔", "Low"), LoEmojiChip("😰", "Stressed"),
    )
    val shapedBy = listOf("Work", "Health", "Relationships", "Money", "Rest", "Weather")
    val shapedEmoji = mapOf("Work" to "💼", "Health" to "💪", "Relationships" to "💕", "Money" to "💰", "Rest" to "😴", "Weather" to "🌤️")
    var feeling by remember { mutableStateOf("Calm") }
    var intensity by remember { mutableIntStateOf(8) }
    var shaped by remember { mutableStateOf("Relationships") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val heroEmoji = feelings.firstOrNull { it.label == feeling }?.emoji ?: "😌"

    Column(
        modifier = modifier.fillMaxWidth().background(LoBg).navigationBarsPadding().verticalScroll(rememberScrollState()),
    ) {
        IntelligenceOsHeader(onClose)
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LoSectionCard {
                Text("Mood", color = LoText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Reflect on your emotional state and what shaped it.", color = LoSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(heroEmoji, fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            LoSectionCard {
                LoFieldLabel("HOW ARE YOU FEELING?")
                Spacer(Modifier.height(10.dp))
                LoEmojiChipRow(feelings, feeling, onSelect = { feeling = it })
            }
            LoSectionCard {
                LoFieldLabel("INTENSITY")
                Spacer(Modifier.height(10.dp))
                LoGradientSlider(intensity, onValueChange = { intensity = it })
            }
            LoSectionCard {
                LoFieldLabel("WHAT SHAPED THIS MOOD?")
                Spacer(Modifier.height(10.dp))
                LoTextChipRow(shapedBy, shaped, onSelect = { shaped = it }, emojiPrefix = shapedEmoji)
            }
            LoSectionCard {
                LoFieldLabel("THIS WEEK")
                Spacer(Modifier.height(8.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    val pts = listOf(0.65f, 0.55f, 0.7f, 0.45f, 0.55f, 0.45f, 0.45f)
                    val step = size.width / (pts.size - 1)
                    for (i in 0 until pts.size - 1) {
                        drawLine(LoAccent, Offset(i * step, size.height * (1 - pts[i])), Offset((i + 1) * step, size.height * (1 - pts[i + 1])), strokeWidth = 2.dp.toPx())
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                        Text(it, color = LoMuted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Average: 7.0", color = LoSecondary, fontSize = 12.sp)
                    Text("Trending: ↑ Up", color = LoGreen, fontSize = 12.sp)
                }
            }
            LoSectionCard {
                LoFieldLabel("REFLECTION NOTE")
                Spacer(Modifier.height(8.dp))
                LoNoteField(note, { note = it }, "Feeling balanced after a good walk...", modifier = Modifier.testTag(MaestroIds.PERSONAL_LIFEOPS_MOOD_NOTE))
            }
            error?.let { Text(it, color = LoError, fontSize = 12.sp) }
            LoSaveButton("Save Reflection", !submitting, submitting, MaestroIds.PERSONAL_LIFEOPS_MOOD_SUBMIT) {
                submitting = true
                scope.launch {
                    repository.recordObservation(
                        momentId = momentId,
                        observationType = LifeOpsQuickAddKind.MOOD.observationType(),
                        numericValue = intensity.toDouble(),
                        textValue = "$feeling · shaped:$shaped",
                        note = note.ifBlank { null },
                        feelingStateCode = when (feeling) {
                            "Great" -> "GREAT"
                            "Calm" -> "CALM"
                            "Neutral" -> "NEUTRAL"
                            "Low" -> "LOW"
                            "Stressed" -> "STRESSED"
                            else -> "OTHER"
                        },
                        moodDrivers = listOf(shaped.uppercase().replace(" ", "_").replace("&", "AND")),
                    ).fold(
                        onSuccess = { submitting = false; onSaved() },
                        onFailure = { e -> submitting = false; error = e.message },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// endregion

// region Attention — Figma `353:11361`

@Composable
private fun AttentionSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier,
) {
    val targets = listOf("Work", "Health", "Family", "Money", "Learning", "Rest")
    var target by remember { mutableStateOf("Work") }
    var depth by remember { mutableIntStateOf(7) }
    var duration by remember { mutableStateOf("60 min") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth().background(LoBg).navigationBarsPadding().verticalScroll(rememberScrollState()),
    ) {
        IntelligenceOsHeader(onClose)
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LoSectionCard {
                Text("Attention", color = LoText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Set where your focus should land next.", color = LoSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            LoSectionCard {
                LoFieldLabel("FOCUS TARGET")
                Spacer(Modifier.height(10.dp))
                LoTextChipRow(targets, target, onSelect = { target = it })
            }
            LoSectionCard {
                LoFieldLabel("DEPTH")
                Spacer(Modifier.height(10.dp))
                LoGradientSlider(depth, onValueChange = { depth = it })
            }
            LoSectionCard {
                LoFieldLabel("DURATION")
                Spacer(Modifier.height(10.dp))
                LoTextChipRow(listOf("15 min", "30 min", "60 min", "2 hr"), duration, onSelect = { duration = it })
            }
            LoSectionCard {
                LoFieldLabel("NOTES")
                Spacer(Modifier.height(8.dp))
                LoNoteField(note, { note = it }, "What deserves your attention right now?", modifier = Modifier.testTag(MaestroIds.PERSONAL_LIFEOPS_ATTENTION_NOTE))
            }
            error?.let { Text(it, color = LoError, fontSize = 12.sp) }
            LoSaveButton("Save Focus", !submitting, submitting, MaestroIds.PERSONAL_LIFEOPS_ATTENTION_SUBMIT) {
                submitting = true
                scope.launch {
                    val intensity = when {
                        depth <= 3 -> "LIGHT"
                        depth <= 7 -> "MODERATE"
                        else -> "HEAVY"
                    }
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val timeBlock = when (hour) {
                        in 5..11 -> "MORNING"
                        in 12..16 -> "AFTERNOON"
                        in 17..21 -> "EVENING"
                        else -> "NIGHT"
                    }
                    repository.recordAttentionCapture(
                        momentId = momentId,
                        categoryCode = target.uppercase(),
                        intensityCode = intensity,
                        timeBlockCode = timeBlock,
                        energyRemaining = (5 - (depth / 2)).coerceIn(0, 5),
                        note = note.ifBlank { null },
                    ).fold(
                        onSuccess = { submitting = false; onSaved() },
                        onFailure = { e -> submitting = false; error = e.message },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// endregion

// region Adjust — Figma `353:11680`

@Composable
private fun AdjustSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier,
) {
    val rhythmActions = listOf(
        LoIconChip("−", "Reduce load"),
        LoIconChip("↑", "Increase intensity"),
        LoIconChip("⏸", "Pause"),
        LoIconChip("↺", "Reset"),
    )
    val priorities = listOf("Health & Energy" to 80, "Career" to 60, "Relationships" to 40, "Finance" to 30, "Learning" to 20)
    var rhythmAction by remember { mutableStateOf("Reduce load") }
    var signal by remember { mutableFloatStateOf(0.5f) }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth().background(LoBg).navigationBarsPadding().verticalScroll(rememberScrollState()),
    ) {
        IntelligenceOsHeader(onClose)
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(LoAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("☰", fontSize = 18.sp)
                    }
                    Column {
                        Text("Tune Rhythm & Priorities", color = LoText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text("Change how your current operating rhythm should respond.", color = LoSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
            LoSectionCard {
                Text("Rhythm Action", color = LoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rhythmActions.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { action ->
                                val sel = rhythmAction == action.label
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) LoAccent.copy(alpha = 0.25f) else LoElevated)
                                        .border(1.dp, if (sel) LoAccent else LoCardBorder, RoundedCornerShape(12.dp))
                                        .clickable { rhythmAction = action.label }
                                        .padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(action.icon, fontSize = 18.sp)
                                    Text(action.label, color = LoText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                                }
                            }
                        }
                    }
                }
            }
            LoSectionCard {
                Text("Priorities", color = LoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(10.dp))
                priorities.forEach { (label, pct) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = LoSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        Text("$pct%", color = LoText, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(LoElevated),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pct / 100f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LoAccent),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            LoSectionCard {
                Text("Signal Direction", color = LoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Decrease" to Color(0xFF3B82F6), "Maintain" to LoMuted, "Increase" to Color(0xFFEF4444)).forEach { (label, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .background(color.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = LoText, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                        }
                    }
                }
                val animatedSignal by animateFloatAsState(signal, spring(dampingRatio = 0.75f), label = "signal")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset -> signal = (offset.x / size.width).coerceIn(0f, 1f) }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (animatedSignal * 280).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, LoAccent, CircleShape),
                    )
                }
            }
            LoSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡", fontSize = 16.sp)
                    Text("INSIGHT", color = LoBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Based on 3 days of low recovery + high work attention, we suggest: Recovery focused mode with decreased pressure.",
                    color = LoSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            LoSectionCard {
                Text("Adjustment Reason", color = LoText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(8.dp))
                LoNoteField(reason, { reason = it }, "Feeling overwhelmed this week, need to slow down.", modifier = Modifier.testTag(MaestroIds.PERSONAL_LIFEOPS_ADJUST_NOTE))
            }
            error?.let { Text(it, color = LoError, fontSize = 12.sp) }
            LoSaveButton("Update Rhythm", !submitting, submitting, MaestroIds.PERSONAL_LIFEOPS_ADJUST_SUBMIT) {
                submitting = true
                scope.launch {
                    val actionCode = when (rhythmAction) {
                        "Reduce load" -> "REDUCE_LOAD"
                        "Increase intensity" -> "INCREASE_INTENSITY"
                        "Pause" -> "PAUSE"
                        "Reset" -> "RESET"
                        else -> null
                    }
                    val signalCode = when {
                        signal < 0.35f -> "DECREASE_PRESSURE"
                        signal > 0.65f -> "INCREASE_PRESSURE"
                        else -> "MAINTAIN"
                    }
                    repository.recordLifeOpsAdjust(
                        momentId = momentId,
                        rhythmActionCode = actionCode,
                        signalDirectionCode = signalCode,
                        reason = reason.ifBlank { null },
                        priorityWeights = priorities.map { (label, weight) ->
                            LifeOpsPriorityWeightBody(
                                priorityCode = label.uppercase()
                                    .replace("&", "AND")
                                    .replace(Regex("[^A-Z0-9]+"), "_")
                                    .trim('_'),
                                weightPct = weight.toDouble(),
                            )
                        },
                    ).fold(
                        onSuccess = { submitting = false; onSaved() },
                        onFailure = { e -> submitting = false; error = e.message },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// endregion
