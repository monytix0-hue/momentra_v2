package com.example.momentra.ui.shell.personal

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/**
 * Figma Lifestyle quick-add sheets:
 * Experience `433:9723`, Wellbeing `433:9852`, Discovery `433:10057`,
 * Create `433:9969`, Adjust `433:9637`.
 */
private val LsBg = Color(0xFF14121B)
private val LsSurface = Color(0xFF1C1926)
private val LsElevated = Color(0xFF14121B)
private val LsField = Color(0xFF3A3842)
private val LsText = Color(0xFFE5E0EE)
private val LsSecondary = Color(0xFFC9C4D8)
private val LsMuted = Color(0xFF9FA0A6)
private val LsDim = Color(0xFF6B6A73)
private val LsBorder = Color(0xFF2A2538)
private val LsCardBorder = Color.White.copy(alpha = 0.08f)
private val LsPink = Color(0xFFEC4899)
private val LsPurple = Color(0xFF7C5CFC)
private val LsLavender = Color(0xFFA78BFA)
private val LsMagenta = Color(0xFFC026D3)
private val LsRose = Color(0xFFF43F5E)
private val LsIndigo = Color(0xFF6366F1)
private val LsError = Color(0xFFF87171)
private val LsSaveGradient = Brush.horizontalGradient(listOf(LsPink, LsPurple))

private data class EmojiChip(val emoji: String, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalLifestyleQuickAddSheet(
    kind: LifestyleQuickAddKind,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    when (kind) {
        LifestyleQuickAddKind.EXPERIENCE -> LifestyleExperienceSheet(
            momentId = momentId,
            onClose = onClose,
            onSaved = onSaved,
            repository = repository,
            modifier = modifier,
        )
        LifestyleQuickAddKind.WELLBEING -> LifestyleWellbeingSheet(
            momentId = momentId,
            onClose = onClose,
            onSaved = onSaved,
            repository = repository,
            modifier = modifier,
        )
        LifestyleQuickAddKind.DISCOVERY -> LifestyleDiscoverySheet(
            momentId = momentId,
            onClose = onClose,
            onSaved = onSaved,
            repository = repository,
            modifier = modifier,
        )
        LifestyleQuickAddKind.EXPRESSION -> LifestyleCreateSheet(
            momentId = momentId,
            onClose = onClose,
            onSaved = onSaved,
            repository = repository,
            modifier = modifier,
        )
        LifestyleQuickAddKind.ADJUST -> LifestyleAdjustSheet(
            momentId = momentId,
            onClose = onClose,
            onSaved = onSaved,
            repository = repository,
            modifier = modifier,
        )
    }
}

@Composable
private fun LifestyleExperienceSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val accent = LsPink
    val typeChips = remember {
        listOf(
            EmojiChip("✈️", "Travel"), EmojiChip("🍽️", "Food"), EmojiChip("🌿", "Nature"),
            EmojiChip("🏔️", "Adventure"), EmojiChip("🎭", "Entertainment"), EmojiChip("👥", "Social"),
            EmojiChip("👨‍👩‍👧", "Family"), EmojiChip("🧘", "Personal"), EmojiChip("🎨", "Hobby"),
            EmojiChip("🤔", "Other"),
        )
    }
    val faceOptions = remember { listOf("Ordinary" to "😐", "Enjoyable" to "😊", "Memorable" to "🤩", "Exceptional" to "🌟") }
    val energyLabels = remember { listOf("Drained", "Neutral", "Refreshed", "Energized") }
    val whoOptions = remember { listOf("Alone", "Partner", "Friends", "Family", "Group") }
    val valueOptions = remember { listOf("Not Worth It", "Okay", "Worth It", "Excellent Value", "Life Enriching") }

    var what by remember { mutableStateOf("") }
    var experienceType by remember { mutableStateOf("Travel") }
    var howWasIt by remember { mutableStateOf("Memorable") }
    var energy by remember { mutableIntStateOf(2) }
    var whoWith by remember { mutableStateOf("Partner") }
    var value by remember { mutableStateOf("Life Enriching") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LifestyleSheetScaffold(modifier = modifier) {
        CaptureLifestyleHeader(onClose = onClose)
        LifestyleHeroCard(emoji = "✨", title = "Experience", body = "Save a memorable moment.", accent = accent)
        LifestyleSectionCard {
            LifestyleSectionLabel("WHAT DID YOU EXPERIENCE?", accent)
            Spacer(Modifier.height(10.dp))
            LifestyleTextField(what, { what = it }, "Weekend hike, live music night...", accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("EXPERIENCE TYPE", accent)
            Spacer(Modifier.height(10.dp))
            EmojiChipGrid(typeChips, experienceType, { experienceType = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("HOW WAS IT?", accent)
            Spacer(Modifier.height(10.dp))
            FaceRatingRow(faceOptions, howWasIt, { howWasIt = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("HOW DID IT AFFECT YOUR ENERGY?", accent)
            Spacer(Modifier.height(10.dp))
            SegmentedScale(energyLabels, energy.coerceIn(0, energyLabels.lastIndex), { energy = it }, accent, segments = 5)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("WHO WERE YOU WITH?", accent)
            Spacer(Modifier.height(10.dp))
            PillChips(whoOptions, whoWith, { whoWith = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("WHAT DID YOU GET FROM IT?", accent)
            Spacer(Modifier.height(10.dp))
            PillChips(valueOptions, value, { value = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("ADD NOTE - OPTIONAL", accent)
            Spacer(Modifier.height(8.dp))
            LifestyleTextField(note, { note = it }, "Add note - optional", accent)
        }
        LifestyleSaveError(error)
        LifestyleSaveButton(
            label = if (submitting) "Saving…" else "Save Experience",
            enabled = !submitting && (what.isNotBlank() || experienceType.isNotBlank()),
            submitting = submitting,
            accent = accent,
            solid = true,
        ) {
            submitting = true
            error = null
            val trimmed = note.trim()
            val title = what.trim().ifBlank { experienceType }.take(120)
            val description = buildList {
                add(experienceType)
                add(howWasIt)
                add(energyLabels.getOrNull(energy.coerceIn(0, energyLabels.lastIndex)).orEmpty())
                add(whoWith)
                add(value)
                if (trimmed.isNotEmpty()) add(trimmed)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            scope.launch {
                repository.createLifestyleActivity(
                    momentId = momentId,
                    lifestyleContext = LifestyleQuickAddKind.EXPERIENCE.apiContext(),
                    title = title,
                    description = description,
                    wellbeingRating = wellbeingRatingFrom(howWasIt, LifestyleQuickAddKind.EXPERIENCE),
                ).fold(
                    onSuccess = { submitting = false; onSaved() },
                    onFailure = { e -> submitting = false; error = e.message ?: "Could not save" },
                )
            }
        }
    }
}

@Composable
private fun LifestyleWellbeingSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val accent = LsLavender
    val areaChips = remember {
        listOf(
            EmojiChip("❤️", "Health"), EmojiChip("💕", "Relationships"), EmojiChip("💼", "Work"),
            EmojiChip("💰", "Money"), EmojiChip("🏠", "Home"), EmojiChip("👥", "Social"),
            EmojiChip("😴", "Rest"), EmojiChip("📈", "Growth"),
        )
    }
    val feelingLabels = remember { listOf("Low", "Moderate", "Good", "Excellent") }
    val shapingChips = remember {
        listOf(
            EmojiChip("😴", "Sleep"), EmojiChip("💼", "Workload"), EmojiChip("💕", "Relationships"),
            EmojiChip("💰", "Money"), EmojiChip("❤️", "Health"), EmojiChip("🌍", "Environment"),
            EmojiChip("📋", "Routine"),
        )
    }

    var feels by remember { mutableStateOf("") }
    var lifeArea by remember { mutableStateOf("Health") }
    var feeling by remember { mutableIntStateOf(2) }
    var shaping by remember { mutableStateOf(setOf("Sleep", "Routine")) }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LifestyleSheetScaffold(modifier = modifier) {
        CaptureLifestyleHeader(onClose = onClose)
        LifestyleHeroCard(emoji = "💜", title = "Wellbeing", body = "Check-in on a life area.", accent = accent, iconBg = accent)
        LifestyleSectionCard {
            Text("How it feels", color = LsText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Spacer(Modifier.height(8.dp))
            LifestyleTextArea(feels, { feels = it }, "How this part of life feels...", accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("WHICH LIFE AREA?", accent)
            Spacer(Modifier.height(10.dp))
            EmojiChipGrid(areaChips, lifeArea, { lifeArea = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("HOW DOES THIS AREA FEEL RIGHT NOW?", accent)
            Spacer(Modifier.height(10.dp))
            SegmentedScale(feelingLabels, feeling, { feeling = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("WHAT IS SHAPING THIS?", accent)
            Spacer(Modifier.height(10.dp))
            EmojiPillChips(shapingChips, shaping, { label ->
                shaping = if (label in shaping) shaping - label else shaping + label
            }, accent)
        }
        LifestyleSectionCard {
            Text("NOTES", color = LsText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Spacer(Modifier.height(8.dp))
            LifestyleTextArea(note, { note = it }, "Add a note - optional", accent)
        }
        LifestyleSaveError(error)
        LifestyleSaveButton(
            label = if (submitting) "Saving…" else "Save Wellbeing Check-in",
            enabled = !submitting && lifeArea.isNotBlank(),
            submitting = submitting,
            accent = accent,
            solid = true,
        ) {
            submitting = true
            error = null
            val trimmedFeels = feels.trim()
            val trimmedNote = note.trim()
            val title = trimmedFeels.ifBlank { lifeArea }.take(120)
            val description = buildList {
                add(lifeArea)
                add(feelingLabels.getOrNull(feeling).orEmpty())
                addAll(shaping.toList())
                if (trimmedFeels.isNotEmpty() && trimmedFeels != title) add(trimmedFeels)
                if (trimmedNote.isNotEmpty()) add(trimmedNote)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            scope.launch {
                repository.createLifestyleActivity(
                    momentId = momentId,
                    lifestyleContext = LifestyleQuickAddKind.WELLBEING.apiContext(),
                    title = title,
                    description = description,
                    wellbeingRating = wellbeingRatingFrom(feelingLabels.getOrNull(feeling).orEmpty(), LifestyleQuickAddKind.WELLBEING),
                ).fold(
                    onSuccess = { submitting = false; onSaved() },
                    onFailure = { e -> submitting = false; error = e.message ?: "Could not save" },
                )
            }
        }
    }
}

@Composable
private fun LifestyleDiscoverySheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val accent = LsMagenta
    val typeChips = remember {
        listOf(
            EmojiChip("📝", "Article"), EmojiChip("🎬", "Video"), EmojiChip("🎙️", "Podcast"),
            EmojiChip("📚", "Book"), EmojiChip("💻", "Course"), EmojiChip("🔧", "Tool"),
            EmojiChip("👤", "Person"), EmojiChip("📍", "Place"),
        )
    }
    val interestLabels = remember { listOf("Mildly", "Interesting", "Very", "Mind-blowing") }
    val exploreOptions = remember { listOf("Maybe", "Likely", "Definitely") }

    var what by remember { mutableStateOf("") }
    var discoveryType by remember { mutableStateOf("Podcast") }
    var interest by remember { mutableIntStateOf(2) }
    var explore by remember { mutableStateOf("Likely") }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LifestyleSheetScaffold(modifier = modifier) {
        DiscoveryHeader(onClose = onClose)
        LifestyleSectionCard {
            LifestyleSectionLabel("What did you discover?", accent)
            Spacer(Modifier.height(10.dp))
            LifestyleTextField(what, { what = it }, "A new podcast, technique, idea...", accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("Discovery type", accent)
            Spacer(Modifier.height(10.dp))
            EmojiChipGrid(typeChips, discoveryType, { discoveryType = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("How interesting was it?", accent)
            Spacer(Modifier.height(10.dp))
            InterestMeter(interestLabels, interest, { interest = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("Will you explore further?", accent)
            Spacer(Modifier.height(10.dp))
            SegmentedPills(exploreOptions, explore, { explore = it }, accent)
        }
        LifestyleSectionCard {
            Text("+ Add note - optional", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Spacer(Modifier.height(8.dp))
            LifestyleTextArea(note, { note = it }, "Add a quick note...", accent)
        }
        LifestyleSaveError(error)
        LifestyleSaveButton(
            label = if (submitting) "Saving…" else "Save Discovery",
            enabled = !submitting && (what.isNotBlank() || discoveryType.isNotBlank()),
            submitting = submitting,
            accent = accent,
            solid = true,
        ) {
            submitting = true
            error = null
            val trimmed = note.trim()
            val title = what.trim().ifBlank { discoveryType }.take(120)
            val description = buildList {
                add(discoveryType)
                add(interestLabels.getOrNull(interest).orEmpty())
                add(explore)
                if (trimmed.isNotEmpty()) add(trimmed)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            scope.launch {
                repository.createLifestyleActivity(
                    momentId = momentId,
                    lifestyleContext = LifestyleQuickAddKind.DISCOVERY.apiContext(),
                    title = title,
                    description = description,
                    wellbeingRating = wellbeingRatingFrom(interestLabels.getOrNull(interest).orEmpty(), LifestyleQuickAddKind.DISCOVERY),
                ).fold(
                    onSuccess = { submitting = false; onSaved() },
                    onFailure = { e -> submitting = false; error = e.message ?: "Could not save" },
                )
            }
        }
    }
}

@Composable
private fun LifestyleCreateSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val accent = LsRose
    val typeChips = remember {
        listOf(
            EmojiChip("✍️", "Writing"), EmojiChip("🎨", "Art"), EmojiChip("🎵", "Music"),
            EmojiChip("💻", "Design"), EmojiChip("📸", "Photography"), EmojiChip("🧩", "Problem Solving"),
            EmojiChip("📋", "Planning"), EmojiChip("📦", "Content"), EmojiChip("🔩", "Other"),
        )
    }
    val timeOptions = remember { listOf("Under 30 min", "30-60 min", "1-2 hours", "2+ hours") }
    val flowOptions = remember { listOf("No", "Partially", "Yes") }

    var what by remember { mutableStateOf("") }
    var creationType by remember { mutableStateOf("Art") }
    var stars by remember { mutableIntStateOf(4) }
    var timeInvested by remember { mutableStateOf("1-2 hours") }
    var flowState by remember { mutableStateOf("Yes") }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LifestyleSheetScaffold(modifier = modifier) {
        CaptureLifestyleHeader(onClose = onClose, titleColor = accent)
        LifestyleHeroCard(emoji = "🎨", title = "Create", body = "Record something you made.", accent = accent, titleColor = accent)
        LifestyleSectionCard {
            LifestyleSectionLabel("WHAT DID YOU CREATE?", accent)
            Spacer(Modifier.height(8.dp))
            LifestyleTextField(what, { what = it }, "Landing page, sketch, recipe...", accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("CREATION TYPE", accent)
            Spacer(Modifier.height(10.dp))
            EmojiPillChips(typeChips, setOf(creationType), { creationType = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("HOW SATISFIED ARE YOU?", accent)
            Spacer(Modifier.height(10.dp))
            StarRatingRow(stars, { stars = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("TIME INVESTED", accent, muted = true)
            Spacer(Modifier.height(10.dp))
            PillChips(timeOptions, timeInvested, { timeInvested = it }, accent)
        }
        LifestyleSectionCard {
            LifestyleSectionLabel("DID YOU REACH FLOW STATE?", accent, muted = true)
            Spacer(Modifier.height(10.dp))
            SegmentedPills(flowOptions, flowState, { flowState = it }, accent, rounded = 12.dp)
        }
        if (showNote) {
            LifestyleSectionCard {
                LifestyleTextArea(note, { note = it }, "Add note - optional", accent)
            }
        } else {
            Text(
                "+ Add note - optional",
                modifier = Modifier
                    .clickable { showNote = true }
                    .padding(vertical = 4.dp),
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        LifestyleSaveError(error)
        LifestyleSaveButton(
            label = if (submitting) "Saving…" else "Save Creation",
            enabled = !submitting && (what.isNotBlank() || creationType.isNotBlank()),
            submitting = submitting,
            accent = accent,
            solid = true,
        ) {
            submitting = true
            error = null
            val trimmed = note.trim()
            val title = what.trim().ifBlank { creationType }.take(120)
            val description = buildList {
                add(creationType)
                add("$stars/5")
                add(timeInvested)
                add("Flow: $flowState")
                if (trimmed.isNotEmpty()) add(trimmed)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            scope.launch {
                repository.createLifestyleActivity(
                    momentId = momentId,
                    lifestyleContext = LifestyleQuickAddKind.EXPRESSION.apiContext(),
                    title = title,
                    description = description,
                    wellbeingRating = stars * 2.0,
                ).fold(
                    onSuccess = { submitting = false; onSaved() },
                    onFailure = { e -> submitting = false; error = e.message ?: "Could not save" },
                )
            }
        }
    }
}

@Composable
private fun LifestyleAdjustSheet(
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
    modifier: Modifier = Modifier,
) {
    val accent = LsIndigo
    val areaChips = remember {
        listOf(
            EmojiChip("😴", "More Rest"), EmojiChip("✈️", "More Travel"), EmojiChip("🎨", "More Creativity"),
            EmojiChip("👥", "More Social Time"), EmojiChip("🏋️", "More Exercise"), EmojiChip("⏰", "More Personal Time"),
            EmojiChip("🌍", "More Exploration"), EmojiChip("⚖️", "More Balance"),
        )
    }
    val importanceOptions = remember { listOf("Low", "Medium", "High") }
    val confidenceOptions = remember { listOf("Not Sure", "Somewhat Sure", "Very Sure") }

    var whatToChange by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("More Creativity") }
    var importance by remember { mutableStateOf("High") }
    var confidence by remember { mutableStateOf("Very Sure") }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LifestyleSheetScaffold(modifier = modifier, bg = Color(0xFF0B0A12)) {
        CaptureLifestyleHeader(onClose = onClose, titleColor = accent, subtitle = "Record what shapes how you live.")
        LifestyleHeroCard(emoji = "⚙️", title = "Adjust", body = "Change your lifestyle priorities and habits.", accent = accent, iconBg = accent)
        LifestyleSectionCard(surface = Color(0xFF1E1C26), border = Color(0xFF2E2A3A)) {
            LifestyleSectionLabel("WHAT TO CHANGE?", accent)
            Spacer(Modifier.height(8.dp))
            LifestyleTextField(whatToChange, { whatToChange = it }, "Spend less eating out, cook more...", accent)
        }
        LifestyleSectionCard(surface = Color(0xFF1E1C26), border = Color(0xFF2E2A3A)) {
            Text(
                "WHAT AREA DO YOU WANT TO ADJUST?",
                color = LsText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Spacer(Modifier.height(12.dp))
            FullWidthAreaChips(areaChips, area, { area = it }, accent)
        }
        LifestyleSectionCard(surface = Color(0xFF1E1C26), border = Color(0xFF2E2A3A)) {
            LifestyleSectionLabel("HOW IMPORTANT IS THIS CHANGE?", accent)
            Spacer(Modifier.height(10.dp))
            SegmentedPills(importanceOptions, importance, { importance = it }, accent)
        }
        LifestyleSectionCard(surface = Color(0xFF1E1C26), border = Color(0xFF2E2A3A)) {
            LifestyleSectionLabel("HOW CONFIDENT ARE YOU?", accent)
            Spacer(Modifier.height(10.dp))
            SegmentedPills(confidenceOptions, confidence, { confidence = it }, accent)
        }
        if (showNote) {
            LifestyleSectionCard(surface = Color(0xFF1E1C26), border = Color(0xFF2E2A3A)) {
                LifestyleTextArea(note, { note = it }, "Add note - optional", accent)
            }
        } else {
            Text(
                "Add note - optional",
                modifier = Modifier.clickable { showNote = true },
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        LifestyleSaveError(error)
        LifestyleSaveButton(
            label = if (submitting) "Saving…" else "Update Lifestyle",
            enabled = !submitting && (whatToChange.isNotBlank() || area.isNotBlank()),
            submitting = submitting,
            accent = accent,
            solid = true,
        ) {
            submitting = true
            error = null
            val trimmed = note.trim()
            val title = whatToChange.trim().ifBlank { area }.take(120)
            val description = buildList {
                add(area)
                add(importance)
                add(confidence)
                if (trimmed.isNotEmpty()) add(trimmed)
            }.filter { it.isNotBlank() }.joinToString(" · ")
            scope.launch {
                repository.createLifestyleActivity(
                    momentId = momentId,
                    lifestyleContext = LifestyleQuickAddKind.ADJUST.apiContext(),
                    title = title,
                    description = description,
                    wellbeingRating = when (importance) {
                        "High" -> 9.0
                        "Medium" -> 7.0
                        else -> 5.0
                    },
                ).fold(
                    onSuccess = { submitting = false; onSaved() },
                    onFailure = { e -> submitting = false; error = e.message ?: "Could not save" },
                )
            }
        }
    }
}

private fun wellbeingRatingFrom(secondary: String, kind: LifestyleQuickAddKind): Double? {
    val rating = when (secondary.lowercase()) {
        "drained", "light", "glance", "low", "ordinary", "not worth it", "small", "mildly", "maybe" -> 4.0
        "neutral", "steady", "solid", "moderate", "okay", "medium", "interesting", "likely" -> 7.0
        "lifted", "deep", "good", "memorable", "worth it", "high", "very", "definitely" -> 9.0
        "excellent", "exceptional", "life enriching", "excellent value", "mind-blowing", "energized", "refreshed" -> 9.5
        else -> null
    }
    return rating ?: if (kind == LifestyleQuickAddKind.WELLBEING) 7.0 else null
}

@Composable
private fun LifestyleSheetScaffold(
    modifier: Modifier = Modifier,
    bg: Color = LsBg,
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
private fun CaptureLifestyleHeader(
    onClose: () -> Unit,
    title: String = "Capture Lifestyle",
    subtitle: String = "Record what shapes how you live.",
    titleColor: Color = LsText,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = LsSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        LifestyleCloseButton(onClose)
    }
}

@Composable
private fun DiscoveryHeader(onClose: () -> Unit) {
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
                    .background(LsSurface)
                    .border(1.dp, LsCardBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔍", fontSize = 22.sp)
            }
            Column {
                Text("Discovery", color = LsText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text("Capture a new curiosity.", color = LsSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
        LifestyleCloseButton(onClose)
    }
}

@Composable
private fun LifestyleCloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LsSurface)
            .border(1.dp, LsCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun LifestyleHeroCard(
    emoji: String,
    title: String,
    body: String,
    accent: Color,
    iconBg: Color = LsSurface,
    titleColor: Color = LsText,
) {
    LifestyleSectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg)
                    .border(1.dp, LsCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text(body, color = LsMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun LifestyleSectionCard(
    surface: Color = LsSurface,
    border: Color = LsCardBorder,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun LifestyleSectionLabel(text: String, accent: Color, muted: Boolean = false) {
    Text(
        text,
        color = if (muted) LsMuted else accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun LifestyleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LsElevated)
            .border(1.dp, LsCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = LsDim, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = LsText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun LifestyleTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LsElevated)
            .border(1.dp, LsCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = LsSecondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = LsText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiChipGrid(
    chips: List<EmojiChip>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    chips.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { chip ->
                val selectedNow = selected == chip.label
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selectedNow) accent else LsElevated)
                        .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(999.dp))
                        .clickable { onSelect(chip.label) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(chip.emoji, fontSize = 16.sp)
                    Text(
                        chip.label,
                        color = if (selectedNow) Color(0xFF14121B) else LsMuted,
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
private fun EmojiPillChips(
    chips: List<EmojiChip>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    accent: Color,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            val selectedNow = chip.label in selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedNow) accent else LsElevated)
                    .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onToggle(chip.label) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(chip.emoji, fontSize = 14.sp)
                Text(
                    chip.label,
                    color = if (selectedNow) Color.White else LsText,
                    fontSize = 12.sp,
                    fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selectedNow = selected == option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedNow) accent else LsElevated)
                    .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    option,
                    color = if (selectedNow) Color(0xFF14121B) else LsMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun FaceRatingRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (label, emoji) ->
            val selectedNow = selected == label
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selectedNow) accent else LsElevated)
                    .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(100.dp))
                    .clickable { onSelect(label) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(emoji, fontSize = 18.sp)
                Text(
                    label,
                    color = if (selectedNow) Color(0xFF14121B) else LsMuted,
                    fontSize = 11.sp,
                    fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun SegmentedScale(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    segments: Int = labels.size,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp)),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index == selectedIndex) accent else Color(0xFF2E293D))
                    .clickable { onSelect(index) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index.coerceAtMost(segments - 1)) },
                color = if (index == selectedIndex) accent else LsDim,
                fontSize = 12.sp,
                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun SegmentedPills(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
    rounded: androidx.compose.ui.unit.Dp = 999.dp,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selectedNow = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(rounded))
                    .background(if (selectedNow) accent else LsElevated)
                    .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(rounded))
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = if (selectedNow) Color.White else LsSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun InterestMeter(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
) {
    val sizes = listOf(28.dp, 34.dp, 40.dp, 46.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.indices.forEach { index ->
            val selectedNow = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(sizes[index])
                    .clip(RoundedCornerShape(sizes[index] / 2))
                    .background(if (selectedNow) accent else Color.Transparent)
                    .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(sizes[index] / 2))
                    .clickable { onSelect(index) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                color = if (index == selectedIndex) accent else LsSecondary,
                fontSize = 12.sp,
                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun StarRatingRow(stars: Int, onSelect: (Int) -> Unit, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { index ->
                Text(
                    if (index <= stars) "★" else "☆",
                    modifier = Modifier.clickable { onSelect(index) },
                    color = accent,
                    fontSize = 18.sp,
                )
            }
        }
        Text(
            "$stars/5 · ${when (stars) { 5 -> "Perfect"; 4 -> "Exceptional"; 3 -> "Good"; 2 -> "Fair"; else -> "Low" }}",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun FullWidthAreaChips(
    chips: List<EmojiChip>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    chips.forEach { chip ->
        val selectedNow = selected == chip.label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selectedNow) accent else LsElevated)
                .border(1.dp, if (selectedNow) accent else LsCardBorder, RoundedCornerShape(999.dp))
                .clickable { onSelect(chip.label) }
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(chip.emoji, fontSize = 18.sp)
            Text(
                chip.label,
                color = if (selectedNow) Color.White else Color(0xFFBEB9D2),
                fontSize = 13.sp,
                fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun LifestyleSaveError(error: String?) {
    error?.let {
        Text(it, color = LsError, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun LifestyleSaveButton(
    label: String,
    enabled: Boolean,
    submitting: Boolean,
    accent: Color,
    solid: Boolean,
    onClick: () -> Unit,
) {
    val bgModifier = if (solid) {
        Modifier.background(accent, RoundedCornerShape(16.dp))
    } else {
        Modifier.background(LsSaveGradient, RoundedCornerShape(16.dp))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .then(bgModifier)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (solid && accent == LsPink) Color(0xFF14121B) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}
