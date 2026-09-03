package com.example.momentra.ui.shell.group.wedding.create

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma Wedding exact tokens — 575:14939 / 575:14768 / 575:15203 / 584:16938. */
object WeddingActiveTheme {
    val Bg = Color(0xFF131313)
    val Accent = Color(0xFFEC4899)
    val AccentLight = Color(0xFFF472B6)
    val AccentSoft = Color(0x33EC4899)
    val AccentSolid = Color(0xFFED4A99)
    val Text = Color(0xFFE5E2E1)
    val Secondary = Color(0xFFDFC0B4)
    val Muted = Color(0xFFA8A19E)
    val Card = Color(0xFF201F1F)
    val Border = Color(0x1AFFFFFF)
    /** Figma dark ink on light pink hero — not peach brown. */
    val DarkText = Color(0xFF14121B)
    val PeachChip = Color(0xFFFFB598)
    val TealChip = Color(0xFF14B8A6)
    val PurpleChip = Color(0xFFA855F7)
    val Beige = Color(0xFFE8D5C4)
    val SectionRadius = 20.dp
    val SectionPad = 20.dp
    val HeroRadius = 24.dp
    /** Light pink hero — Figma Pulse. */
    val HeroGradient = Brush.horizontalGradient(listOf(Color(0xFFFBCFE8), Color(0xFFF472B6)))
    val MagentaGradient = Brush.horizontalGradient(listOf(Accent, AccentLight))
    val InsightsGradient = Brush.horizontalGradient(listOf(AccentSolid, Color(0xFFDB2777)))
    val StatGradientA = Brush.horizontalGradient(listOf(Color(0xFF8C1F59), Color(0xFF591438)))
    val StatGradientB = Brush.horizontalGradient(listOf(Color(0xFF992673), Color(0xFF661A4D)))
    val StatGradientC = Brush.horizontalGradient(listOf(Color(0xFFA62E66), Color(0xFF6B1A40)))
    val StatGradientD = Brush.horizontalGradient(listOf(Color(0xFF802680), Color(0xFF521452)))
    val HubHeroGradient = Brush.horizontalGradient(listOf(Color(0xFFFBCFE8), Color(0xFFF472B6)))
}

@Composable
fun WeddingFadeIn(content: @Composable () -> Unit) {
    content()
}

@Composable
fun WeddingSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WeddingActiveTheme.SectionRadius))
            .background(WeddingActiveTheme.Card)
            .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(WeddingActiveTheme.SectionRadius))
            .padding(WeddingActiveTheme.SectionPad),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = WeddingActiveTheme.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            trailing?.invoke()
        }
        content()
    }
}

@Composable
fun WeddingViewAllLink(enabled: Boolean = false, onClick: () -> Unit = {}) {
    Text(
        "View all",
        color = WeddingActiveTheme.Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun WeddingPinkCta(
    title: String,
    subtitle: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlinedButton: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WeddingActiveTheme.MagentaGradient)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (outlinedButton) {
                        Modifier
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    } else {
                        Modifier.background(Color.White)
                    },
                )
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+ $buttonLabel",
                color = if (outlinedButton) Color.White else WeddingActiveTheme.Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun WeddingStatCard(
    label: String,
    value: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    icon: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Text(it, fontSize = 12.sp) }
            Text(label, color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
        Text(value, color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun WeddingEmojiChip(
    label: String,
    emoji: String,
    enabled: Boolean = false,
    showDot: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.95f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WeddingActiveTheme.AccentSoft)
                    .border(1.dp, WeddingActiveTheme.Accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            if (showDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(WeddingActiveTheme.Accent),
                )
            }
        }
        Text(label, color = WeddingActiveTheme.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun WeddingAttentionRow(
    emoji: String,
    title: String,
    detail: String,
    cta: String,
    onCta: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WeddingActiveTheme.Bg)
            .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(14.dp))
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(64.dp)
                .background(WeddingActiveTheme.Accent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WeddingActiveTheme.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = WeddingActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(detail, color = WeddingActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            Text(
                cta,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(WeddingActiveTheme.Accent)
                    .clickable(onClick = onCta)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun WeddingPartyProgressRow(
    name: String,
    role: String,
    percent: Int,
    featured: Boolean = false,
    avatarColor: Color = WeddingActiveTheme.Accent,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$name ($role)${if (featured) " ★" else ""}",
                    color = WeddingActiveTheme.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(if (featured) "Most active" else "Active", color = WeddingActiveTheme.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(WeddingActiveTheme.AccentSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(WeddingActiveTheme.Accent),
                )
            }
        }
        Text("$percent%", color = WeddingActiveTheme.AccentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun WeddingCategoryBar(label: String, amountLabel: String, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WeddingActiveTheme.Accent),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = WeddingActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text(amountLabel, color = WeddingActiveTheme.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(WeddingActiveTheme.AccentSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(WeddingActiveTheme.Accent),
                )
            }
        }
    }
}

@Composable
fun WeddingSegmentedProgress(filled: Int, total: Int = 8, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < filled) WeddingActiveTheme.Accent else WeddingActiveTheme.AccentSoft),
            )
        }
    }
}

@Composable
fun WeddingActivityRow(emoji: String, title: String, whenLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WeddingActiveTheme.Bg)
            .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(52.dp)
                .background(WeddingActiveTheme.Accent.copy(alpha = 0.7f)),
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = WeddingActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
                Text(whenLabel, color = WeddingActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

/** @deprecated removed from Figma surfaces — kept for any residual call sites. */
@Composable
fun WeddingDemoBadge(modifier: Modifier = Modifier) {
    // No-op visual: Figma has no Demo badges.
}

@Composable
fun WeddingEmptyBlock(message: String, detail: String, showApiGap: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(message, color = WeddingActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = WeddingActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun WeddingComingSoonBlock(message: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(message, color = WeddingActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = WeddingActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun WeddingChip(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) WeddingActiveTheme.AccentSoft else WeddingActiveTheme.Card)
            .border(
                1.dp,
                if (selected) WeddingActiveTheme.Accent else WeddingActiveTheme.Border,
                RoundedCornerShape(12.dp),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) WeddingActiveTheme.AccentLight else WeddingActiveTheme.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun WeddingIconChip(
    label: String,
    iconRes: Int,
    enabled: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    WeddingEmojiChip(label = label, emoji = "•", enabled = enabled, onClick = onClick, modifier = modifier)
}
