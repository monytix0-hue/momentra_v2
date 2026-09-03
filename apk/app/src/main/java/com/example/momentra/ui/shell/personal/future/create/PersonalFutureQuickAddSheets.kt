package com.example.momentra.ui.shell.personal.future.create

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.personal.future.create.apiKind

/**
 * Figma Future Building quick-add sheets:
 * Milestone `353:11724`, Opportunity `353:11768`, Pivot `353:11812`, plus Progress / Learning.
 */
enum class FutureQuickAddKind {
    MILESTONE,
    OPPORTUNITY,
    PIVOT,
    PROGRESS,
    LEARNING,
}

fun FutureQuickAddKind.apiKind(): String = when (this) {
    FutureQuickAddKind.MILESTONE -> "MILESTONE"
    FutureQuickAddKind.OPPORTUNITY -> "OPPORTUNITY"
    FutureQuickAddKind.PIVOT -> "PIVOT"
    FutureQuickAddKind.PROGRESS -> "PROGRESS"
    FutureQuickAddKind.LEARNING -> "LEARNING"
}

private val FbBg = Color(0xFF14121B)
private val FbSurface = Color(0xFF201E28)
private val FbElevated = Color(0xFF35333E)
private val FbText = Color(0xFFE5E0EE)
private val FbSecondary = Color(0xFFC9C4D8)
private val FbBorder = Color(0xFF938EA1)
private val FbCardBorder = Color.White.copy(alpha = 0.08f)
private val FbBrand = Color(0xFFC9BFFF)
private val FbOnBrand = Color(0xFF2F009C)
private val FbAccent = Color(0xFF7C5CFC)
private val FbPivot = Color(0xFF06B6D4)
private val FbProgress = Color(0xFF10B981)
private val FbLearning = Color(0xFF6366F1)
private val FbError = Color(0xFFF87171)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalFutureQuickAddSheet(
    kind: FutureQuickAddKind,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val copy = remember(kind) { kind.toCopy() }
    var primary by remember(kind) { mutableStateOf(copy.primaryOptions.first()) }
    var secondary by remember(kind) { mutableStateOf(copy.secondaryOptions.firstOrNull().orEmpty()) }
    var tertiary by remember(kind) { mutableStateOf(copy.tertiaryOptions.firstOrNull().orEmpty()) }
    var note by remember(kind) { mutableStateOf("") }
    var meter by remember(kind) { mutableIntStateOf(copy.meterDefault) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FbBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    copy.title,
                    color = FbText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    copy.subtitle,
                    color = FbSecondary,
                    fontSize = 12.sp,
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
                Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FbCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(copy.heroIconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(copy.heroGlyph, color = Color.White, fontSize = 18.sp, fontFamily = PlusJakartaSans)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            copy.heroTitle,
                            color = copy.heroTitleColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            copy.heroBody,
                            color = FbSecondary,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            FbCard {
                FbLabel(copy.noteLabel)
                Spacer(Modifier.height(8.dp))
                FbNote(value = note, onValueChange = { note = it }, placeholder = copy.notePlaceholder)
            }

            FbCard {
                FbLabel(copy.primaryLabel)
                Spacer(Modifier.height(10.dp))
                FbChips(
                    options = copy.primaryOptions,
                    selected = primary,
                    onSelect = { primary = it },
                    activeFill = copy.chipActive,
                    activeText = copy.chipActiveText,
                )
            }

            if (copy.secondaryOptions.isNotEmpty()) {
                FbCard {
                    FbLabel(copy.secondaryLabel)
                    Spacer(Modifier.height(10.dp))
                    FbChips(
                        options = copy.secondaryOptions,
                        selected = secondary,
                        onSelect = { secondary = it },
                        activeFill = copy.chipActive,
                        activeText = copy.chipActiveText,
                    )
                }
            }

            if (copy.tertiaryOptions.isNotEmpty()) {
                FbCard {
                    FbLabel(copy.tertiaryLabel)
                    Spacer(Modifier.height(10.dp))
                    FbChips(
                        options = copy.tertiaryOptions,
                        selected = tertiary,
                        onSelect = { tertiary = it },
                        activeFill = copy.chipActive,
                        activeText = copy.chipActiveText,
                    )
                }
            }

            if (copy.showMeter) {
                FbCard {
                    FbLabel(copy.meterLabel)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp)),
                    ) {
                        copy.meterOptions.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .background(if (meter == index) copy.chipActive else FbBg)
                                    .clickable { meter = index },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        copy.meterOptions.forEachIndexed { index, label ->
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                color = if (meter == index) copy.chipActive else FbSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (meter == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }

            error?.let {
                Text(it, color = FbError, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            val canSave = primary.isNotBlank() && !submitting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (canSave) 1f else 0.6f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(copy.ctaColor)
                    .clickable(enabled = canSave) {
                        submitting = true
                        error = null
                        val trimmed = note.trim()
                        val title = if (trimmed.isEmpty()) primary else trimmed.take(120)
                        val parts = buildList {
                            add(primary)
                            if (secondary.isNotBlank()) add(secondary)
                            if (tertiary.isNotBlank()) add(tertiary)
                            if (copy.showMeter && meter in copy.meterOptions.indices) {
                                add(copy.meterOptions[meter])
                            }
                            if (trimmed.isNotEmpty() && trimmed != title) add(trimmed)
                        }
                        val description = parts.joinToString(" · ")
                        val progressValue = kind.progressValue(meter, copy.meterOptions)
                        scope.launch {
                            val result = repository.createFutureItem(
                                momentId = momentId,
                                kind = kind.apiKind(),
                                title = title,
                                description = description,
                                progressValue = progressValue,
                            )
                            submitting = false
                            result.fold(
                                onSuccess = { onSaved() },
                                onFailure = { e -> error = e.message ?: "Could not save" },
                            )
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (submitting) "Saving…" else copy.cta,
                    color = copy.ctaText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

private data class FutureSheetCopy(
    val title: String,
    val subtitle: String,
    val heroTitle: String,
    val heroBody: String,
    val heroGlyph: String,
    val heroIconBg: Color,
    val heroTitleColor: Color,
    val noteLabel: String,
    val notePlaceholder: String,
    val primaryLabel: String,
    val primaryOptions: List<String>,
    val secondaryLabel: String = "",
    val secondaryOptions: List<String> = emptyList(),
    val tertiaryLabel: String = "",
    val tertiaryOptions: List<String> = emptyList(),
    val showMeter: Boolean = false,
    val meterLabel: String = "",
    val meterOptions: List<String> = emptyList(),
    val meterDefault: Int = 0,
    val chipActive: Color,
    val chipActiveText: Color,
    val cta: String,
    val ctaColor: Color,
    val ctaText: Color,
)

private fun FutureQuickAddKind.toCopy(): FutureSheetCopy = when (this) {
    FutureQuickAddKind.MILESTONE -> FutureSheetCopy(
        title = "Quick Add",
        subtitle = "Milestone",
        heroTitle = "Milestone",
        heroBody = "Celebrate an achievement that moved you forward.",
        heroGlyph = "★",
        heroIconBg = FbElevated,
        heroTitleColor = FbText,
        noteLabel = "WHAT DID YOU ACHIEVE?",
        notePlaceholder = "Career growth, Business growth, Personal reinvention...",
        primaryLabel = "WHAT KIND OF MILESTONE?",
        primaryOptions = listOf(
            "Achievement", "Recognition", "Completion", "Launch",
            "Certification", "Promotion", "Breakthrough", "Revenue Event",
        ),
        secondaryLabel = "HOW BIG DOES THIS FEEL?",
        secondaryOptions = listOf("Personal Win", "Shared Win", "Life Moment"),
        tertiaryLabel = "MEASURABLE OUTCOME",
        tertiaryOptions = listOf(
            "Income Increase", "Saving Increase", "Revenue Increase",
            "Cost Reduction", "No Financial Impact",
        ),
        chipActive = FbBrand,
        chipActiveText = FbOnBrand,
        cta = "Save Milestone",
        ctaColor = FbBrand,
        ctaText = FbOnBrand,
    )
    FutureQuickAddKind.OPPORTUNITY -> FutureSheetCopy(
        title = "Build Momentum",
        subtitle = "Record something that moved you forward.",
        heroTitle = "Opportunity",
        heroBody = "Spot and capture opportunities as they appear.",
        heroGlyph = "◎",
        heroIconBg = FbAccent,
        heroTitleColor = FbAccent,
        noteLabel = "WHAT DID YOU SPOT?",
        notePlaceholder = "Describe what happened",
        primaryLabel = "OPPORTUNITY TYPE",
        primaryOptions = listOf(
            "New Connection", "New Skill", "New Resource", "New Idea",
            "New Funding", "New Role", "New Client", "Partnership",
        ),
        secondaryLabel = "STATUS",
        secondaryOptions = listOf("Exploring", "Considering", "Acting", "Captured"),
        showMeter = true,
        meterLabel = "POTENTIAL IMPACT",
        meterOptions = listOf("Low", "Moderate", "High", "Game Changer"),
        meterDefault = 2,
        chipActive = FbAccent,
        chipActiveText = Color.White,
        cta = "Save Opportunity",
        ctaColor = FbAccent,
        ctaText = Color.White,
    )
    FutureQuickAddKind.PIVOT -> FutureSheetCopy(
        title = "Build Momentum",
        subtitle = "Record something that moved you forward.",
        heroTitle = "Pivot",
        heroBody = "Record a change in direction or strategy.",
        heroGlyph = "↻",
        heroIconBg = FbElevated,
        heroTitleColor = FbText,
        noteLabel = "WHAT DIRECTION CHANGED?",
        notePlaceholder = "I moved from Developer role to Engineering Management.",
        primaryLabel = "WHAT CHANGED?",
        primaryOptions = listOf(
            "New Priority", "New Goal", "Reduce Scope",
            "Increase Focus", "Change Timeline", "Change Direction",
        ),
        secondaryLabel = "WHY DID YOU CHANGE DIRECTION?",
        secondaryOptions = listOf(
            "New Information", "Opportunity", "Constraint", "Personal Decision", "Market Change",
        ),
        showMeter = true,
        meterLabel = "HOW CONFIDENT ARE YOU?",
        meterOptions = listOf("40%", "65%", "85%"),
        meterDefault = 2,
        chipActive = FbPivot,
        chipActiveText = Color.White,
        cta = "Save Pivot",
        ctaColor = FbPivot,
        ctaText = Color.White,
    )
    FutureQuickAddKind.PROGRESS -> FutureSheetCopy(
        title = "Build Momentum",
        subtitle = "Record something that moved you forward.",
        heroTitle = "Progress",
        heroBody = "Mark forward motion on a goal or habit.",
        heroGlyph = "↗",
        heroIconBg = Color(0xFF047857),
        heroTitleColor = FbProgress,
        noteLabel = "WHAT MOVED FORWARD?",
        notePlaceholder = "Shipped a feature, closed a deal, finished a module…",
        primaryLabel = "PROGRESS TYPE",
        primaryOptions = listOf(
            "Skill", "Project", "Habit", "Revenue", "Fitness", "Learning Goal",
        ),
        secondaryLabel = "STATUS",
        secondaryOptions = listOf("Started", "Midway", "Nearly Done", "Complete"),
        showMeter = true,
        meterLabel = "HOW FAR?",
        meterOptions = listOf("25%", "50%", "75%", "100%"),
        meterDefault = 1,
        chipActive = FbProgress,
        chipActiveText = Color.White,
        cta = "Save Progress",
        ctaColor = FbProgress,
        ctaText = Color.White,
    )
    FutureQuickAddKind.LEARNING -> FutureSheetCopy(
        title = "Build Momentum",
        subtitle = "Record something that moved you forward.",
        heroTitle = "Learning",
        heroBody = "Capture a growth signal while it's fresh.",
        heroGlyph = "✎",
        heroIconBg = Color(0xFF4338CA),
        heroTitleColor = FbLearning,
        noteLabel = "WHAT DID YOU LEARN?",
        notePlaceholder = "A concept, skill, or insight you want to keep…",
        primaryLabel = "LEARNING TYPE",
        primaryOptions = listOf(
            "Course", "Book", "Mentorship", "Practice", "Reflection", "Experiment",
        ),
        secondaryLabel = "DEPTH",
        secondaryOptions = listOf("Glance", "Solid", "Deep"),
        tertiaryLabel = "APPLY TO",
        tertiaryOptions = listOf("Career", "Skills", "Network", "Capital", "Mindset"),
        chipActive = FbLearning,
        chipActiveText = Color.White,
        cta = "Save Learning",
        ctaColor = FbLearning,
        ctaText = Color.White,
    )
}

private fun FutureQuickAddKind.progressValue(meter: Int, meterOptions: List<String>): Double? {
    if (meter !in meterOptions.indices) return null
    val raw = meterOptions[meter].replace("%", "").trim()
    raw.toDoubleOrNull()?.let { return it }
    return when (raw.lowercase()) {
        "low" -> 25.0
        "moderate" -> 50.0
        "high" -> 75.0
        "game changer" -> 95.0
        else -> null
    }
}

@Composable
private fun FbCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FbSurface)
            .border(1.dp, FbCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun FbLabel(text: String) {
    Text(
        text,
        color = FbBrand,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun FbNote(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FbElevated)
            .border(1.dp, FbBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = FbSecondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = FbText, fontSize = 13.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(FbBrand),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FbChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    activeFill: Color,
    activeText: Color,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selectedNow = selected == option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedNow) activeFill else FbElevated)
                    .border(
                        1.dp,
                        if (selectedNow) activeFill else FbBorder,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    option,
                    color = if (selectedNow) activeText else FbText,
                    fontSize = 12.sp,
                    fontWeight = if (selectedNow) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}
