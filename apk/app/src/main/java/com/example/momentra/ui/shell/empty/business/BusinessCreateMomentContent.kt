package com.example.momentra.ui.shell.empty.business

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessIcons

private val Page = Color(0xFF111113)
private val ChoiceCard = Color(0xFF1B1B1E)
private val ChoiceStroke = Color(0x1AFFFFFF)
private val Emerald = Color(0xFF10B981)
private val EmeraldWash = Color(0xFF10B981).copy(alpha = 0.07f)
private val HeroFill = Color(0xFF18181B)
private val HeroStroke = Color(0xFF053B29)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFFA7A7AE)
private val CtaInk = Color(0xFF0E0E0E)

/** One guided business choice: card identity plus the preview it unlocks. */
private data class BizChoice(
    val kind: BusinessSetupKind,
    val caption: String,
    @DrawableRes val icon: Int,
    val promise: String,
    val description: String,
    val capabilities: List<Pair<Int, String>>,
) {
    val eyebrow: String
        get() = when (kind) {
            BusinessSetupKind.MARKETING_CAMPAIGNS -> "YOUR MARKETING MOMENT"
            BusinessSetupKind.TRADING_DISTRIBUTION -> "YOUR TRADING MOMENT"
            BusinessSetupKind.RETAIL_CONSUMER -> "YOUR RETAIL MOMENT"
            BusinessSetupKind.MANUFACTURING_PRODUCTION -> "YOUR MANUFACTURING MOMENT"
        }
}

private fun iconRes(key: String): Int = BusinessIcons.res(key) ?: R.drawable.ic_biz_layers

private val BizChoices: List<BizChoice> = listOf(
    BizChoice(
        kind = BusinessSetupKind.MARKETING_CAMPAIGNS,
        caption = "Ads, promotions & leads",
        icon = iconRes("megaphone"),
        promise = "Built for the way you promote.",
        description = "Plan campaigns, track leads, and keep every approval in one place.",
        capabilities = listOf(
            iconRes("wallet") to "Campaign budgets",
            iconRes("target") to "Lead tracking",
            iconRes("users") to "Client retainers",
            iconRes("badge-check") to "Creative approvals",
        ),
    ),
    BizChoice(
        kind = BusinessSetupKind.TRADING_DISTRIBUTION,
        caption = "Bulk buying & selling",
        icon = iconRes("truck"),
        promise = "Built for the way you trade.",
        description = "Buy in bulk, sell across counters, and keep stock, dispatch and collections in step.",
        capabilities = listOf(
            iconRes("package") to "Purchase & stock",
            iconRes("truck") to "Sales & dispatch",
            iconRes("wallet") to "Collections & ledger",
            iconRes("route") to "Shipments & tracking",
        ),
    ),
    BizChoice(
        kind = BusinessSetupKind.RETAIL_CONSUMER,
        caption = "Shop & counter sales",
        icon = iconRes("store"),
        promise = "Built for the way you sell.",
        description = "Run counter sales, keep stock honest, and close the day with cash that matches.",
        capabilities = listOf(
            iconRes("store") to "POS / counter sales",
            iconRes("package") to "Inventory & stock",
            iconRes("users") to "Customer tabs",
            iconRes("receipt") to "Daily cash close",
        ),
    ),
    BizChoice(
        kind = BusinessSetupKind.MANUFACTURING_PRODUCTION,
        caption = "Batch & job work",
        icon = iconRes("factory"),
        promise = "Built for the way you make.",
        description = "Make products in batches from raw materials or manage made-to-order job work.",
        capabilities = listOf(
            iconRes("clipboard-list") to "Quotes & job orders",
            iconRes("factory") to "Production & WIP",
            iconRes("layers") to "BOM & materials",
            iconRes("receipt-text") to "Invoices against orders",
        ),
    ),
)

/**
 * Figma 2943:9075 — Create a moment · Guided business selection.
 *
 * The top bar "+" opens this screen directly for Business: pick a kind, read what the moment
 * will include, then Continue into the four-step setup wizard for that kind.
 */
@Composable
fun BusinessCreateMomentContent(
    onBack: () -> Unit,
    onSelectSetup: (BusinessSetupKind) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(BusinessSetupKind.MANUFACTURING_PRODUCTION) }
    val choice = BizChoices.first { it.kind == selected }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Page),
    ) {
        Header(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Question(
                selected = selected,
                onSelect = { selected = it },
            )
            BusinessPreview(choice)
        }

        ContinueAction(
            caption = "You can change this later in Manage moment.",
            onClick = { onSelectSetup(choice.kind) },
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MomentraWordmark(
            showTagline = false,
            titleSizeSp = 21f,
            alignStart = true,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = onBack)
                .semantics {
                    contentDescription = "Close creation"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val stroke = 1.5.dp.toPx()
                drawLine(
                    color = TextPrimary,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = TextPrimary,
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun Question(
    selected: BusinessSetupKind,
    onSelect: (BusinessSetupKind) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "What kind of\nbusiness do you run?",
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            )
            Text(
                text = "Choose a type. We’ll tailor your moment.",
                color = TextSecondary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
        // 2x2 grid, 10dp between columns and rows, matching the Figma category pairs.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BizChoices.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { choice ->
                        BusinessChoice(
                            choice = choice,
                            isSelected = choice.kind == selected,
                            onSelect = { onSelect(choice.kind) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessChoice(
    choice: BizChoice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(EmeraldWash)
                        .border(1.5.dp, Emerald, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                        .background(ChoiceCard)
                        .border(1.dp, ChoiceStroke, RoundedCornerShape(12.dp))
                },
            )
            .testTag(choice.kind.maestroTag)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics {
                role = Role.RadioButton
                this.selected = isSelected
                contentDescription = "${choice.kind.title}. ${choice.caption}"
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(choice.icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (isSelected) Emerald else TextSecondary),
                modifier = Modifier.size(19.dp),
            )
            SelectionIndicator(isSelected = isSelected)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = choice.kind.title,
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
            )
            Text(
                text = choice.caption,
                color = TextSecondary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

/** Unselected is a 17dp hairline ring; selected is an 18dp emerald disc with a dark check. */
@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    if (!isSelected) {
        Box(
            modifier = Modifier
                .size(17.dp)
                .border(1.5.dp, Color(0x59FFFFFF), CircleShape),
        )
        return
    }
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = Emerald)
        val unit = size.minDimension / 18f
        val check = Path().apply {
            moveTo(4f * unit, 9.5f * unit)
            lineTo(7.5f * unit, 12.5f * unit)
            lineTo(14f * unit, 5.5f * unit)
        }
        drawPath(
            path = check,
            color = CtaInk,
            style = Stroke(
                width = 1.8f * unit,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun BusinessPreview(choice: BizChoice) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HeroFill)
                .border(1.dp, HeroStroke, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = choice.eyebrow,
                    color = Emerald,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = choice.promise,
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                )
                Text(
                    text = choice.description,
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1AFFFFFF)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choice.capabilities.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        pair.forEach { (icon, label) ->
                            Capability(icon = icon, label = label, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Capability(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Emerald),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ContinueAction(caption: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Emerald)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = "Continue"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Continue",
                color = CtaInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
        }
        Text(
            text = caption,
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
