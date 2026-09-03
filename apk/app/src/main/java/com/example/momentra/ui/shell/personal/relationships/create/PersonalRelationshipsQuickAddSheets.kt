package com.example.momentra.ui.shell.personal.relationships.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.personal.shared.RelationshipsQuickAddKind
import com.example.momentra.ui.shell.personal.shared.apiActivityKind

/**
 * Figma Relationships quick-add sheets:
 * Adjust `439:9468`, Support `439:9574`, Connection `439:9666`,
 * Shared Exp `439:9798`, Investment `439:9907`.
 */
private val RlBg = Color(0xFF14121B)
private val RlSurface = Color(0xFF1C1926)
private val RlElevated = Color(0xFF14121B)
private val RlDarkSurface = Color(0xFF14131B)
private val RlText = Color(0xFFE5E0EE)
private val RlSecondary = Color(0xFFC9C4D8)
private val RlMuted = Color(0xFFA099B0)
private val RlDim = Color(0xFFA39EB9)
private val RlBorder = Color(0xFF2A2538)
private val RlCardBorder = Color.White.copy(alpha = 0.08f)
private val RlPink = Color(0xFFE12A9E)
private val RlPinkSoft = Color(0x1AE12A9E)
private val RlError = Color(0xFFF87171)
private val RlSaveGradient = Brush.horizontalGradient(listOf(RlPink, Color(0xFFF472B6)))

private data class RelEmojiChip(val emoji: String, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalRelationshipsQuickAddSheet(
    kind: RelationshipsQuickAddKind,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    when (kind) {
        RelationshipsQuickAddKind.CONNECTION -> RelationshipsConnectionSheet(
            momentId, onClose, onSaved, repository, modifier,
        )
        RelationshipsQuickAddKind.SUPPORT -> RelationshipsSupportSheet(
            momentId, onClose, onSaved, repository, modifier,
        )
        RelationshipsQuickAddKind.SHARED -> RelationshipsSharedExpSheet(
            momentId, onClose, onSaved, repository, modifier,
        )
        RelationshipsQuickAddKind.INVESTMENT -> RelationshipsInvestmentSheet(
            momentId, onClose, onSaved, repository, modifier,
        )
        RelationshipsQuickAddKind.ADJUST -> RelationshipsAdjustSheet(
            momentId, onClose, onSaved, repository, modifier,
        )
    }
}

@Composable
private fun RelationshipsConnectionSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val typeChips = remember {
        listOf(
            RelEmojiChip("💬", "Conversation"), RelEmojiChip("🍽️", "Meal Together"),
            RelEmojiChip("💌", "Message"), RelEmojiChip("📞", "Call"),
            RelEmojiChip("🎉", "Celebration"), RelEmojiChip("✅", "Check-in"),
            RelEmojiChip("⏰", "Shared Time"), RelEmojiChip("🏠", "Visit"),
        )
    }
    val relTypes = remember {
        listOf("Child", "Community", "Family", "Friend", "Mentor", "Parent", "Partner", "Professional")
    }
    val relEmojis = mapOf(
        "Child" to "👶", "Community" to "🌍", "Family" to "👨‍👩‍👧", "Friend" to "🤗",
        "Mentor" to "🎓", "Parent" to "👨‍👦", "Partner" to "💑", "Professional" to "💼",
    )
    val depthLabels = remember { listOf("Distant", "Meaningful", "Memorable", "Routine") }
    val toneChips = remember {
        listOf("Warm", "Calm", "Joyful", "Serious", "Tense", "Supportive")
    }
    val toneEmojis = mapOf("Warm" to "🔥", "Calm" to "😌", "Joyful" to "😊", "Serious" to "🧠", "Tense" to "💚", "Supportive" to "🤗")
    val timeOptions = remember { listOf("5 min", "15 min", "30 min", "1 Hour", "2+ hours") }

    var who by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var connectionType by remember { mutableStateOf("Conversation") }
    var relationshipType by remember { mutableStateOf("Friend") }
    var depth by remember { mutableIntStateOf(3) }
    var tone by remember { mutableStateOf("Warm") }
    var timeInvested by remember { mutableStateOf("1 Hour") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RelationshipsSheetScaffold(modifier = modifier) {
        CaptureRelationshipsHeader(onClose = onClose)
        RelationshipsHeroCard("👥", "Connection", "A meaningful contact or presence.")
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("👤", "WHO?")
            Spacer(Modifier.height(10.dp))
            RelTextField(who, { who = it }, "Who did you connect with?", focused = true)
            Spacer(Modifier.height(10.dp))
            RelTextField(context, { context = it }, "Caught up after several months")
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("🤝", "CONNECTION TYPE")
            Spacer(Modifier.height(10.dp))
            RelEmojiChipGrid(typeChips, connectionType, { connectionType = it })
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("💕", "RELATIONSHIP TYPE")
            Spacer(Modifier.height(10.dp))
            RelPillChips(relTypes.map { "${relEmojis[it].orEmpty()} $it" }, "${relEmojis[relationshipType].orEmpty()} $relationshipType", {
                relationshipType = it.substringAfter(" ").trim()
            })
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("💫", "HOW CONNECTED DID IT FEEL?")
            Spacer(Modifier.height(10.dp))
            RelDepthMeter(depthLabels, depth, { depth = it })
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("🎭", "WHAT WAS THE TONE?")
            Spacer(Modifier.height(10.dp))
            RelPillChips(toneChips.map { "${toneEmojis[it].orEmpty()} $it" }, "${toneEmojis[tone].orEmpty()} $tone", {
                tone = it.substringAfter(" ").trim()
            })
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("⏱️", "TIME INVESTED")
            Spacer(Modifier.height(10.dp))
            RelPillChips(timeOptions, timeInvested, { timeInvested = it })
            Spacer(Modifier.height(10.dp))
            RelTimeTimeline(timeOptions, timeInvested)
        }
        RelDivider()
        RelationshipsSectionCard {
            RelSectionHeader("✍️", "NOTES")
            Spacer(Modifier.height(10.dp))
            RelTextArea(note, { note = it }, "Why did this connection matter?")
        }
        RelSaveError(error)
        RelationshipsSaveButton(
            label = if (submitting) "Saving…" else "Save Connection ✨",
            enabled = !submitting && (who.isNotBlank() || connectionType.isNotBlank()),
            submitting = submitting,
        ) {
            saveRelationship(
                scope, repository, momentId, RelationshipsQuickAddKind.CONNECTION,
                displayParts = buildList {
                    val name = who.trim().ifBlank { connectionType }
                    add(name)
                    add(connectionType)
                    add(relationshipType)
                    add(depthLabels.getOrNull(depth).orEmpty())
                    add(tone)
                    add(timeInvested)
                    if (context.isNotBlank()) add(context)
                },
                note = note.trim().ifBlank { null },
                onSubmitting = { submitting = it },
                onError = { error = it },
                onSaved = onSaved,
            )
        }
    }
}

@Composable
private fun RelationshipsSupportSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val typeChips = remember {
        listOf(
            RelEmojiChip("💡", "Advice"), RelEmojiChip("💝", "Care"), RelEmojiChip("🎊", "Celebration"),
            RelEmojiChip("🫂", "Emotional"), RelEmojiChip("💪", "Encouragement"), RelEmojiChip("💰", "Financial"),
            RelEmojiChip("🔧", "Practical"),
        )
    }
    val directionOptions = remember { listOf("Given", "Received", "Mutual") }
    val directionEmojis = mapOf("Given" to "➡️", "Received" to "⬅️", "Mutual" to "↔️")
    val helpfulnessOptions = remember { listOf("Small", "Meaningful", "Important", "Transformational") }

    var what by remember { mutableStateOf("") }
    var supportType by remember { mutableStateOf("Care") }
    var direction by remember { mutableStateOf("Given") }
    var helpfulness by remember { mutableStateOf("Meaningful") }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RelationshipsSheetScaffold(modifier = modifier) {
        SupportHeader(onClose = onClose)
        RelDivider()
        RelationshipsHeroCard("🫶", "Support", "Care or help was given, received, or shared.")
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text("WHAT HAPPENED?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))
            RelTextField(what, { what = it }, "Helped a friend prepare for an interview")
        }
        Column {
            Text("SUPPORT TYPE", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelEmojiChipGrid(typeChips, supportType, { supportType = it })
        }
        Column {
            Text("WHICH DIRECTION DID THE SUPPORT FLOW?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelationshipsSectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    directionOptions.forEach { option ->
                        val selectedNow = direction == option
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selectedNow) RlPink else RlElevated)
                                .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(16.dp))
                                .clickable { direction = option }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(directionEmojis[option].orEmpty(), fontSize = 16.sp)
                            Text(option, color = if (selectedNow) Color.White else RlDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }
        Column {
            Text("HOW HELPFUL WAS IT?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelationshipsSectionCard {
                RelSegmentedScale(helpfulnessOptions, helpfulness, { helpfulness = it })
            }
        }
        if (showNote) {
            RelTextArea(note, { note = it }, "Add note - optional")
        } else {
            Text(
                "+ Add note - optional",
                modifier = Modifier.clickable { showNote = true },
                color = RlPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        RelSaveError(error)
        RelationshipsSaveButton(
            label = if (submitting) "Saving…" else "Save Support",
            enabled = !submitting && (what.isNotBlank() || supportType.isNotBlank()),
            submitting = submitting,
        ) {
            saveRelationship(
                scope, repository, momentId, RelationshipsQuickAddKind.SUPPORT,
                displayParts = buildList {
                    add(what.trim().ifBlank { supportType })
                    add(supportType)
                    add(direction)
                    add(helpfulness)
                },
                note = note.trim().ifBlank { null },
                onSubmitting = { submitting = it },
                onError = { error = it },
                onSaved = onSaved,
            )
        }
    }
}

@Composable
private fun RelationshipsSharedExpSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val typeChips = remember {
        listOf(
            RelEmojiChip("🏔️", "Adventure"), RelEmojiChip("🎉", "Celebration"), RelEmojiChip("💬", "Conversation"),
            RelEmojiChip("🎨", "Creative"), RelEmojiChip("🍽️", "Meal"), RelEmojiChip("🚗", "Outing"),
            RelEmojiChip("⭐", "Quality Time"), RelEmojiChip("🏃", "Sport"), RelEmojiChip("✈️", "Travel"),
            RelEmojiChip("🤔", "Other"),
        )
    }
    val whoOptions = remember { listOf("Child", "Community", "Family", "Friend", "Mentor", "Parent", "Partner") }
    val faceOptions = remember { listOf("Ordinary" to "😐", "Enjoyable" to "😊", "Memorable" to "🤩", "Exceptional" to "🌟") }
    val impactOptions = remember { listOf("Neutral", "Warm", "Uplifting", "Deeply Meaningful") }

    var what by remember { mutableStateOf("") }
    var expType by remember { mutableStateOf("Conversation") }
    var whoWith by remember { mutableStateOf("Family") }
    var feeling by remember { mutableStateOf("Memorable") }
    var impact by remember { mutableStateOf("Uplifting") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RelationshipsSheetScaffold(modifier = modifier) {
        SharedExpHeader(onClose = onClose)
        RelationshipsHeroCard("✨", "Shared Experience", "Capture a shared moment and its emotional return.")
        RelationshipsSectionCard {
            Text("WHAT DID YOU DO?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelTextField(what, { what = it }, "Family dinner, weekend hike...")
        }
        RelationshipsSectionCard {
            Text("EXPERIENCE TYPE", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelEmojiChipGrid(typeChips, expType, { expType = it }, rounded = 16.dp)
        }
        RelationshipsSectionCard {
            Text("WHO WAS IT WITH?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelPillChips(whoOptions, whoWith, { whoWith = it })
        }
        RelationshipsSectionCard {
            Text("HOW DID IT FEEL?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelFaceRow(faceOptions, feeling, { feeling = it })
        }
        RelationshipsSectionCard {
            Text("EMOTIONAL IMPACT", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelPillChips(impactOptions, impact, { impact = it })
        }
        RelationshipsSectionCard {
            Text("NOTES", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelTextArea(note, { note = it }, "Why did this change matter?")
        }
        RelSaveError(error)
        RelationshipsSaveButton(
            label = if (submitting) "Saving…" else "Save Shared Experience",
            enabled = !submitting && (what.isNotBlank() || expType.isNotBlank()),
            submitting = submitting,
        ) {
            saveRelationship(
                scope, repository, momentId, RelationshipsQuickAddKind.SHARED,
                displayParts = buildList {
                    add(what.trim().ifBlank { expType })
                    add(expType)
                    add(whoWith)
                    add(feeling)
                    add(impact)
                },
                note = note.trim().ifBlank { null },
                onSubmitting = { submitting = it },
                onError = { error = it },
                onSaved = onSaved,
            )
        }
    }
}

@Composable
private fun RelationshipsInvestmentSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val areaChips = remember {
        listOf(
            RelEmojiChip("⏰", "Quality Time"), RelEmojiChip("💝", "Emotional Support"), RelEmojiChip("💰", "Financial"),
            RelEmojiChip("🎁", "Gift"), RelEmojiChip("💪", "Effort"), RelEmojiChip("📚", "Listening"),
            RelEmojiChip("📋", "Planning"), RelEmojiChip("🔍", "Other"),
        )
    }
    val whoOptions = remember { listOf("Child", "Family", "Friend", "Mentor", "Parent", "Partner", "Professional") }
    val timeOptions = remember { listOf("15 min", "30 min", "1 hour", "2 hours", "Half Day", "Full Day") }
    val meaningfulOptions = remember { listOf("Small", "Moderate", "Significant", "Transformational") }

    var what by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Emotional Support") }
    var who by remember { mutableStateOf("Partner") }
    var time by remember { mutableStateOf("1 hour") }
    var meaningful by remember { mutableStateOf("Moderate") }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RelationshipsSheetScaffold(modifier = modifier, bg = Color(0xFF0B0A12)) {
        CaptureRelationshipsHeader(onClose = onClose)
        RelationshipsHeroCard("✨", "Investment", "Capture time, care, or resources intentionally invested.", subtitleAccent = true)
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("What did you invest?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))
            RelTextField(what, { what = it }, "Anniversary planning session")
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("Investment Area", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelEmojiPillFlow(areaChips, area, { area = it })
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("Who did you invest in?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelPillChips(whoOptions, who, { who = it })
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("Time Invested", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelPillChips(timeOptions, time, { time = it })
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("How meaningful?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelSegmentedScale(meaningfulOptions, meaningful, { meaningful = it })
        }
        if (showNote) {
            RelTextArea(note, { note = it }, "Add note - optional")
        } else {
            Text(
                "Add note - optional",
                modifier = Modifier.clickable { showNote = true },
                color = RlPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        RelSaveError(error)
        RelationshipsSaveButton(
            label = if (submitting) "Saving…" else "Save Relationship Investment",
            enabled = !submitting && (what.isNotBlank() || area.isNotBlank()),
            submitting = submitting,
        ) {
            saveRelationship(
                scope, repository, momentId, RelationshipsQuickAddKind.INVESTMENT,
                displayParts = buildList {
                    add(what.trim().ifBlank { area })
                    add(area)
                    add(who)
                    add(time)
                    add(meaningful)
                },
                note = note.trim().ifBlank { null },
                onSubmitting = { submitting = it },
                onError = { error = it },
                onSaved = onSaved,
            )
        }
    }
}

@Composable
private fun RelationshipsAdjustSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val areaChips = remember {
        listOf(
            RelEmojiChip("💬", "Better Communication"), RelEmojiChip("🙏", "More Appreciation"),
            RelEmojiChip("📅", "More Consistency"), RelEmojiChip("🎉", "More Fun"),
            RelEmojiChip("👋", "More Presence"), RelEmojiChip("🤝", "More Shared Experiences"),
            RelEmojiChip("🫂", "More Support"), RelEmojiChip("⏰", "More Time Together"),
        )
    }
    val attentionOptions = remember { listOf("Child", "Family", "Friend", "Parent", "Partner") }
    val importanceOptions = remember { listOf("Low", "Medium", "High") }
    val confidenceOptions = remember { listOf("Not Sure", "Somewhat Sure", "Very Sure") }

    var whatToChange by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("More Appreciation") }
    var attention by remember { mutableStateOf("Friend") }
    var importance by remember { mutableStateOf("Medium") }
    var confidence by remember { mutableStateOf("Very Sure") }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RelationshipsSheetScaffold(modifier = modifier, bg = Color(0xFF0B0A12)) {
        CaptureRelationshipsHeader(onClose = onClose)
        RelationshipsHeroCard("💗", "Adjust", "Change a relationship priority.")
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("What to change?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))
            RelTextField(whatToChange, { whatToChange = it }, "Make more time for close friends")
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("Adjustment Area", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelEmojiChipGrid(areaChips, area, { area = it })
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("Who should get more attention?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelPillChips(attentionOptions, attention, { attention = it })
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("How important is this change?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelSegmentedScale(importanceOptions, importance, { importance = it }, outlineSelected = true)
        }
        RelationshipsSectionCard(surface = RlDarkSurface) {
            Text("How confident are you?", color = RlPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            RelSegmentedScale(confidenceOptions, confidence, { confidence = it }, outlineSelected = true)
        }
        if (showNote) {
            RelTextArea(note, { note = it }, "Add note - optional")
        } else {
            Text(
                "Add note - optional",
                modifier = Modifier.clickable { showNote = true },
                color = RlPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        RelSaveError(error)
        RelationshipsSaveButton(
            label = if (submitting) "Saving…" else "Update Relationship",
            enabled = !submitting && (whatToChange.isNotBlank() || area.isNotBlank()),
            submitting = submitting,
        ) {
            saveRelationship(
                scope, repository, momentId, RelationshipsQuickAddKind.ADJUST,
                displayParts = buildList {
                    add(whatToChange.trim().ifBlank { area })
                    add(area)
                    add(attention)
                    add(importance)
                    add(confidence)
                },
                note = note.trim().ifBlank { null },
                onSubmitting = { submitting = it },
                onError = { error = it },
                onSaved = onSaved,
            )
        }
    }
}

private fun saveRelationship(
    scope: kotlinx.coroutines.CoroutineScope,
    repository: PersonalSliceRepository,
    momentId: String,
    kind: RelationshipsQuickAddKind,
    displayParts: List<String>,
    note: String?,
    onSubmitting: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSaved: () -> Unit,
) {
    onSubmitting(true)
    onError(null)
    val displayName = displayParts.filter { it.isNotBlank() }.joinToString(" · ").take(200)
    scope.launch {
        repository.recordRelationshipActivity(
            momentId = momentId,
            activityKind = kind.apiActivityKind(),
            displayName = displayName.ifBlank { kind.name },
            note = note,
        ).fold(
            onSuccess = { onSubmitting(false); onSaved() },
            onFailure = { e -> onSubmitting(false); onError(e.message ?: "Could not save") },
        )
    }
}

@Composable
private fun RelationshipsSheetScaffold(
    modifier: Modifier = Modifier,
    bg: Color = RlBg,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
    }
}

@Composable
private fun CaptureRelationshipsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Capture Relationships", color = RlPink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text("Record the moments and actions that shape your connections.", color = RlMuted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        RelCloseButton(onClose)
    }
}

@Composable
private fun SupportHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(RlSurface)
                    .border(1.dp, RlCardBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🫶", fontSize = 22.sp)
            }
            Column {
                Text("Capture Relationships", color = RlText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text("Record the moments and actions that shape your connections.", color = RlSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
        RelCloseButton(onClose)
    }
}

@Composable
private fun SharedExpHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(RlSurface)
                    .border(1.dp, RlCardBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✨", fontSize = 22.sp)
            }
            Column {
                Text("Shared Experience", color = RlText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text("Record a moment that strengthened a connection.", color = RlSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
        RelCloseButton(onClose)
    }
}

@Composable
private fun RelCloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(RlSurface)
            .border(1.dp, RlBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun RelationshipsHeroCard(
    emoji: String,
    title: String,
    body: String,
    subtitleAccent: Boolean = false,
) {
    RelationshipsSectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RlPinkSoft)
                    .border(1.dp, RlPink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = RlText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(
                    body,
                    color = if (subtitleAccent) RlPink else RlMuted,
                    fontSize = if (subtitleAccent) 14.sp else 13.sp,
                    fontWeight = if (subtitleAccent) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun RelationshipsSectionCard(
    surface: Color = RlSurface,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(1.dp, RlBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun RelSectionHeader(emoji: String, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RlPinkSoft)
                .border(1.dp, RlPink, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Text(title, color = RlText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun RelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

@Composable
private fun RelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focused: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RlElevated)
            .border(1.dp, if (focused) RlPink else RlBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = RlMuted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = RlText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(RlPink),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun RelTextArea(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RlElevated)
            .border(1.dp, RlBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = RlMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = RlText, fontSize = 13.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(RlPink),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelEmojiChipGrid(
    chips: List<RelEmojiChip>,
    selected: String,
    onSelect: (String) -> Unit,
    rounded: Dp = 999.dp,
) {
    chips.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { chip ->
                val selectedNow = selected == chip.label
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(rounded))
                        .background(if (selectedNow) RlPink else RlElevated)
                        .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(rounded))
                        .clickable { onSelect(chip.label) }
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(chip.emoji, fontSize = 16.sp)
                    Text(
                        chip.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelEmojiPillFlow(chips: List<RelEmojiChip>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            val selectedNow = selected == chip.label
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedNow) RlPink else Color(0xFF191724))
                    .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(chip.label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(chip.emoji, fontSize = 14.sp)
                Text(chip.label, color = if (selectedNow) Color.White else RlDim, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelPillChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selectedNow = selected == option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedNow) RlPink else RlElevated)
                    .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    option,
                    color = if (selectedNow) Color.White else RlSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun RelDepthMeter(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val sizes = listOf(28.dp, 36.dp, 44.dp, 52.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((selectedIndex + 1) / labels.size.toFloat())
                .height(6.dp)
                .background(RlPink),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(sizes[index])
                        .clip(RoundedCornerShape(sizes[index] / 2))
                        .background(if (index == selectedIndex) RlPink else RlElevated)
                        .border(1.dp, if (index == selectedIndex) RlPink else RlBorder, RoundedCornerShape(sizes[index] / 2))
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                Text(label, color = RlMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun RelFaceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (label, emoji) ->
            val selectedNow = selected == label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(label) },
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (selectedNow) RlPinkSoft else RlElevated)
                        .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 18.sp, color = if (selectedNow) RlPink else RlText)
                }
                Text(label, color = if (selectedNow) RlPink else RlSecondary, fontSize = 11.sp, fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.Normal, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun RelTimeTimeline(options: List<String>, selected: String) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((index + 1) / options.size.toFloat())
                .height(6.dp)
                .background(RlPink),
        )
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("5m", "15m", "30m", "1h", "2+").forEach { label ->
            Text(label, color = RlMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun RelSegmentedScale(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    outlineSelected: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selectedNow = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            selectedNow && outlineSelected -> RlPinkSoft
                            selectedNow -> RlPink
                            else -> RlElevated
                        },
                    )
                    .border(1.dp, if (selectedNow) RlPink else RlCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = if (selectedNow && outlineSelected) RlPink else if (selectedNow) Color.White else RlDim,
                    fontSize = 13.sp,
                    fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun RelSaveError(error: String?) {
    error?.let {
        Text(it, color = RlError, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun RelationshipsSaveButton(
    label: String,
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clip(RoundedCornerShape(16.dp))
            .background(RlPink)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}
