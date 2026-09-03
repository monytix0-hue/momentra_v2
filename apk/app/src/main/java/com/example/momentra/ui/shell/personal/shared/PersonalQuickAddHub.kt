package com.example.momentra.ui.shell.personal.shared

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.shell.MomentThemes
import com.example.momentra.ui.shell.personal.future.create.FutureQuickAddKind

private val HubBg = Color(0xFF14121B)
private val HubSurface = Color(0xFF201E28)
private val HubText = Color(0xFFE5E0EE)
private val HubSecondary = Color(0xFFC9C4D8)
private val HubBorder = Color(0xFF938EA1)
private val HubPurple = Color(0xFF6C4EF2)
private val HubRelPink = Color(0xFFE12A9E)
private val HubRelTeal = Color(0xFF14B8A6)

/**
 * Figma `1006:7553` personal-quick-add-grid.
 * Relationships family: Figma `1006:8274` Action Center / Relationships.
 * Bottom-nav + opens this hub. Tile set follows active moment family.
 */
@Composable
fun PersonalQuickAddHub(
    hasActiveMoment: Boolean,
    onClose: () -> Unit,
    onIncome: () -> Unit,
    onRecovery: () -> Unit = {},
    onMood: () -> Unit = {},
    onAttention: () -> Unit = {},
    onAdjust: () -> Unit = {},
    onTransfer: () -> Unit = {},
    onSavings: () -> Unit = {},
    onFutureQuickAdd: (FutureQuickAddKind) -> Unit = {},
    onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit = {},
    onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Unit = {},
    momentTypeCode: String? = null,
    /**
     * Bootstrap capability codes (V019). Empty = show family-default tiles;
     * [PersonalActionRegistry] is the single mapper when caps are present.
     */
    capabilities: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val family = personalPulseFamilyFor(momentTypeCode)
    val isRelationships = family == PersonalPulseFamily.RELATIONSHIPS
    val isLifestyle = family == PersonalPulseFamily.LIFESTYLE
    val isLifeOps = family == PersonalPulseFamily.LIFE_OPERATIONS
    val momentTheme = MomentThemes.resolve(AppContext.PERSONAL, momentTypeCode)
    var search by remember { mutableStateOf("") }
    val actions = hubActionsFor(
        family = family,
        hasActiveMoment = hasActiveMoment,
        capabilities = capabilities,
        onIncome = onIncome,
        onRecovery = onRecovery,
        onMood = onMood,
        onAttention = onAttention,
        onAdjust = onAdjust,
        onTransfer = onTransfer,
        onSavings = onSavings,
        onFutureQuickAdd = onFutureQuickAdd,
        onLifestyleQuickAdd = onLifestyleQuickAdd,
        onRelationshipsQuickAdd = onRelationshipsQuickAdd,
    ).filter {
        val q = search.trim()
        q.isEmpty() || it.label.contains(q, ignoreCase = true)
    }
    val heroTitle = when (family) {
        PersonalPulseFamily.RELATIONSHIPS -> "Nurture your bonds"
        PersonalPulseFamily.LIFESTYLE -> "Curate your lifestyle"
        else -> "Design your focus"
    }
    val blurb = when (family) {
        PersonalPulseFamily.FUTURE_BUILDING ->
            "Quickly record milestones, opportunities, pivots, and progress."
        PersonalPulseFamily.LIFESTYLE ->
            "Track experiences, wellbeing, discoveries, expressions and adjustments."
        PersonalPulseFamily.RELATIONSHIPS ->
            "Track connections, support, shared experiences, investments and adjustments."
        else ->
            "Quickly record expenses, recovery states, mood, attention targets, and reflections."
    }
    val heroBrush = when (family) {
        PersonalPulseFamily.RELATIONSHIPS -> Brush.linearGradient(
            listOf(Color(0xFF14B8A6).copy(alpha = 0.2f), Color(0xFF10B981).copy(alpha = 0.122f)),
        )
        PersonalPulseFamily.LIFESTYLE -> Brush.linearGradient(
            listOf(Color(0xFFEC4899).copy(alpha = 0.122f), Color(0xFFA78BFA).copy(alpha = 0.122f)),
        )
        else -> Brush.linearGradient(
            listOf(momentTheme.primary.copy(alpha = 0.24f), momentTheme.secondary.copy(alpha = 0.12f)),
        )
    }
    val heroRes = if (isRelationships) R.drawable.qa_hero_relationships else R.drawable.qa_hero
    val searchPlaceholder = when (family) {
        PersonalPulseFamily.FUTURE_BUILDING -> "Search future actions…"
        PersonalPulseFamily.LIFESTYLE -> "Search lifestyle actions..."
        PersonalPulseFamily.RELATIONSHIPS -> "Search relationships..."
        else -> "Search personal actions..."
    }
    val chipAccent = when (family) {
        PersonalPulseFamily.RELATIONSHIPS -> HubRelPink
        PersonalPulseFamily.LIFESTYLE -> HubPurple
        else -> momentTheme.primary
    }
    val useWideTiles = isRelationships || isLifestyle || isLifeOps
    val cardHeight = if (useWideTiles) 104.dp else 88.dp
    val cardRadius = if (useWideTiles) 16.dp else 14.dp
    val gridGap = if (useWideTiles) 12.dp else 10.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HubBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Quick Add",
                    color = HubText,
                    fontSize = if (useWideTiles) 22.sp else 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Simplify your day and align focus",
                    color = HubSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HubSurface)
                    .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_qa_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            when (family) {
                PersonalPulseFamily.FUTURE_BUILDING -> {
                    HubChip("Future Building", selected = true, accent = chipAccent)
                    HubChip("Growth Mindset", selected = false, accent = chipAccent)
                }
                PersonalPulseFamily.LIFESTYLE -> {
                    HubChip("Lifestyle", selected = true, accent = chipAccent)
                    HubChip("Wellness", selected = false, accent = chipAccent)
                }
                PersonalPulseFamily.RELATIONSHIPS -> {
                    HubChip(
                        "Relationships",
                        selected = true,
                        accent = chipAccent,
                        selectedBorder = HubRelTeal,
                    )
                    HubChip("Connections", selected = false, accent = chipAccent, filledUnselected = true)
                }
                else -> {
                    HubChip("Personal Space", selected = true, accent = chipAccent)
                    HubChip("Introspective", selected = false, accent = chipAccent)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(heroBrush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    heroTitle,
                    color = HubText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    blurb,
                    color = HubSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Image(
                    painter = painterResource(heroRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isRelationships) 14.dp else 12.dp))
                .background(HubSurface)
                .padding(horizontal = if (isRelationships) 14.dp else 12.dp, vertical = if (isRelationships) 11.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_qa_search),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = HubText,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(chipAccent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text(
                            searchPlaceholder,
                            color = HubSecondary,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    inner()
                },
            )
        }

        // Figma `1122:7929` — 3 + 3 + 2 wide rows; Lifestyle/Relationships: 3 + 2
        val actionRows: List<List<HubAction>> = when {
            isLifeOps && search.isBlank() -> listOf(
                actions.take(3),
                actions.drop(3).take(3),
                actions.drop(6).take(2),
            )
            useWideTiles && search.isBlank() -> listOf(
                actions.take(3),
                actions.drop(3),
            )
            else -> actions.chunked(3)
        }
        actionRows.forEach { row ->
            val fillEmptySlots = !(useWideTiles && row.size == 2 && search.isBlank())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gridGap),
            ) {
                row.forEach { action ->
                    ActionCard(
                        label = action.label,
                        iconRes = action.iconRes,
                        brush = action.brush,
                        enabled = action.enabled,
                        onClick = action.onClick,
                        height = cardHeight,
                        cornerRadius = cardRadius,
                        labelSizeSp = if (useWideTiles) 14 else 12,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(qaTileTagForLabel(action.label)),
                    )
                }
                if (fillEmptySlots) {
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (!hasActiveMoment) {
            Text(
                "Create a Personal Moment from the top-bar + to unlock Quick Add actions.",
                color = HubSecondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun qaTileTagForLabel(label: String): String = when (label) {
    "Income" -> MaestroIds.QA_TILE_INCOME
    "Recovery" -> MaestroIds.QA_TILE_RECOVERY
    "Mood" -> MaestroIds.QA_TILE_MOOD
    "Attention" -> MaestroIds.QA_TILE_ATTENTION
    "Adjust" -> MaestroIds.QA_TILE_ADJUST
    "Milestone" -> MaestroIds.QA_TILE_MILESTONE
    "Opportunity" -> MaestroIds.QA_TILE_OPPORTUNITY
    "Pivot" -> MaestroIds.QA_TILE_PIVOT
    "Progress" -> MaestroIds.QA_TILE_PROGRESS
    "Learning" -> MaestroIds.QA_TILE_LEARNING
    "Experience" -> MaestroIds.QA_TILE_EXPERIENCE
    "Wellbeing" -> MaestroIds.QA_TILE_WELLBEING
    "Discovery" -> MaestroIds.QA_TILE_DISCOVERY
    "Create", "Expression" -> MaestroIds.QA_TILE_EXPRESSION
    "Connection" -> MaestroIds.QA_TILE_CONNECTION
    "Support" -> MaestroIds.QA_TILE_SUPPORT
    "Shared Exp" -> MaestroIds.QA_TILE_SHARED_EXP
    "Investment" -> MaestroIds.QA_TILE_INVESTMENT
    "Transfer" -> MaestroIds.QA_TILE_TRANSFER
    "Savings" -> MaestroIds.QA_TILE_SAVINGS
    else -> "qa.tile.${label.lowercase().replace(' ', '_')}"
}

private data class HubAction(
    val label: String,
    val iconRes: Int,
    val brush: Brush,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

private fun hubActionsFor(
    family: PersonalPulseFamily,
    hasActiveMoment: Boolean,
    capabilities: List<String>,
    onIncome: () -> Unit,
    onRecovery: () -> Unit,
    onMood: () -> Unit,
    onAttention: () -> Unit,
    onAdjust: () -> Unit,
    onTransfer: () -> Unit,
    onSavings: () -> Unit,
    onFutureQuickAdd: (FutureQuickAddKind) -> Unit,
    onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit,
    onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Unit,
): List<HubAction> {
    val expenseEnabled = hasActiveMoment &&
        PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.EXPENSE)
    val moneyQuickAddEnabled = expenseEnabled ||
        (hasActiveMoment &&
            PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.MOVEMENT))
    val lifeOpsEnabled = hasActiveMoment &&
        PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.LIFE_OPS)
    val futureEnabled = hasActiveMoment &&
        PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.FUTURE)
    val lifestyleEnabled = hasActiveMoment &&
        PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.LIFESTYLE)
    val relationshipsEnabled = hasActiveMoment &&
        PersonalActionRegistry.isDestinationEnabled(capabilities, PersonalActionRegistry.Destination.RELATIONSHIPS)

    val income = HubAction(
        label = "Income",
        iconRes = R.drawable.ic_qa_trending,
        brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))),
        enabled = moneyQuickAddEnabled,
        onClick = onIncome,
    )
    return when (family) {
        PersonalPulseFamily.FUTURE_BUILDING -> listOf(
            HubAction("Milestone", R.drawable.ic_qa_target, Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6C4EF2))), futureEnabled) {
                onFutureQuickAdd(FutureQuickAddKind.MILESTONE)
            },
            HubAction("Opportunity", R.drawable.ic_qa_activity, Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))), futureEnabled) {
                onFutureQuickAdd(FutureQuickAddKind.OPPORTUNITY)
            },
            HubAction("Pivot", R.drawable.ic_qa_refresh, Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2))), futureEnabled) {
                onFutureQuickAdd(FutureQuickAddKind.PIVOT)
            },
            HubAction("Progress", R.drawable.ic_qa_trending, Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))), futureEnabled) {
                onFutureQuickAdd(FutureQuickAddKind.PROGRESS)
            },
            HubAction("Learning", R.drawable.ic_qa_book, Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA))), futureEnabled) {
                onFutureQuickAdd(FutureQuickAddKind.LEARNING)
            },
        )
        // Figma `1006:8074` — Experience | Wellbeing | Discovery / Create | Adjust (no Expense)
        PersonalPulseFamily.LIFESTYLE -> listOf(
            HubAction("Experience", R.drawable.ic_qa_wallet, Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFBE185D))), lifestyleEnabled) {
                onLifestyleQuickAdd(LifestyleQuickAddKind.EXPERIENCE)
            },
            HubAction("Wellbeing", R.drawable.ic_qa_activity, Brush.horizontalGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))), lifestyleEnabled) {
                onLifestyleQuickAdd(LifestyleQuickAddKind.WELLBEING)
            },
            HubAction("Discovery", R.drawable.ic_qa_smile, Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFC026D3))), lifestyleEnabled) {
                onLifestyleQuickAdd(LifestyleQuickAddKind.DISCOVERY)
            },
            HubAction("Create", R.drawable.ic_qa_target, Brush.horizontalGradient(listOf(Color(0xFFFB7185), Color(0xFFF43F5E))), lifestyleEnabled) {
                onLifestyleQuickAdd(LifestyleQuickAddKind.EXPRESSION)
            },
            HubAction("Adjust", R.drawable.ic_qa_refresh, Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA))), lifestyleEnabled) {
                onLifestyleQuickAdd(LifestyleQuickAddKind.ADJUST)
            },
        )
        // Figma `1006:8274` — Connection | Support | Shared Exp / Investment | Adjust (no Expense)
        PersonalPulseFamily.RELATIONSHIPS -> listOf(
            HubAction("Connection", R.drawable.ic_qa_users, Brush.horizontalGradient(listOf(Color(0xFFE12A9E), Color(0xFFBE1882))), relationshipsEnabled) {
                onRelationshipsQuickAdd(RelationshipsQuickAddKind.CONNECTION)
            },
            HubAction("Support", R.drawable.ic_qa_heart, Brush.horizontalGradient(listOf(Color(0xFFC8238C), Color(0xFFA51473))), relationshipsEnabled) {
                onRelationshipsQuickAdd(RelationshipsQuickAddKind.SUPPORT)
            },
            HubAction("Shared Exp", R.drawable.ic_qa_camera, Brush.horizontalGradient(listOf(Color(0xFFEB3CAA), Color(0xFFC82891))), relationshipsEnabled) {
                onRelationshipsQuickAdd(RelationshipsQuickAddKind.SHARED)
            },
            HubAction("Investment", R.drawable.ic_qa_trending, Brush.horizontalGradient(listOf(Color(0xFFF578C8), Color(0xFFE12A9E))), relationshipsEnabled) {
                onRelationshipsQuickAdd(RelationshipsQuickAddKind.INVESTMENT)
            },
            HubAction("Adjust", R.drawable.ic_qa_sliders, Brush.horizontalGradient(listOf(Color(0xFFF064B9), Color(0xFFD23296))), relationshipsEnabled) {
                onRelationshipsQuickAdd(RelationshipsQuickAddKind.ADJUST)
            },
        )
        else -> listOf(
            income,
            HubAction("Recovery", R.drawable.ic_qa_activity, Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))), lifeOpsEnabled, onRecovery),
            HubAction("Mood", R.drawable.ic_qa_smile, Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2))), lifeOpsEnabled, onMood),
            HubAction("Attention", R.drawable.ic_qa_target, Brush.horizontalGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))), lifeOpsEnabled, onAttention),
            HubAction("Transfer", R.drawable.ic_qa_refresh, Brush.horizontalGradient(listOf(Color(0xFF1E40AF), Color(0xFF0B2A8A))), moneyQuickAddEnabled, onTransfer),
            HubAction("Savings", R.drawable.ic_qa_trending, Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))), moneyQuickAddEnabled, onSavings),
            HubAction("Adjust", R.drawable.ic_qa_sliders, Brush.horizontalGradient(listOf(Color(0xFFD946EF), Color(0xFF86198F))), lifeOpsEnabled, onAdjust),
            HubAction("Reflect", R.drawable.ic_qa_book, Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA))), false) {},
        )
    }
}

@Composable
private fun HubChip(
    label: String,
    selected: Boolean,
    accent: Color = HubPurple,
    selectedBorder: Color? = null,
    filledUnselected: Boolean = false,
) {
    val bg = when {
        selected -> accent
        filledUnselected -> HubSurface
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> (selectedBorder ?: accent).copy(alpha = if (selectedBorder != null) 1f else 0.3f)
        else -> HubBorder
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else HubSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun ActionCard(
    label: String,
    iconRes: Int,
    brush: Brush,
    enabled: Boolean,
    onClick: () -> Unit,
    height: Dp = 88.dp,
    cornerRadius: Dp = 14.dp,
    labelSizeSp: Int = 12,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier
                .size(28.dp)
                .alpha(if (enabled) 1f else 0.55f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
            fontSize = labelSizeSp.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}
