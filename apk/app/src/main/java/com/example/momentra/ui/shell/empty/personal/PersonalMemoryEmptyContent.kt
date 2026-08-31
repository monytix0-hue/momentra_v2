package com.example.momentra.ui.shell.empty.personal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `353:5878` body — Personal Memory empty. */
@Composable
fun PersonalMemoryEmptyContent(
    onCreateMoment: () -> Unit,
    history: List<MomentSummary> = emptyList(),
    modifier: Modifier = Modifier,
) {
    PeAppear {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PeBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Your Memory Is Forming",
                        color = PeText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "As you log moments, Momentra builds a deep intelligence layer - discovering who you are, how you grow, and what drives you.",
                        color = PeSecondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MemoryPreviewStack()
            }

            HowMemoryWorksSection()

            ModulesAwaitingDataSection()

            WhatYoullUnlockSection()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PeGradientPrimaryButton(
                    label = "Initialize Your Memory",
                    onClick = onCreateMoment,
                )
                Text(
                    "Every moment makes your memory smarter",
                    color = PeSubtle,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PeQuoteCard(text = "\"After 2 weeks, Memory showed me patterns I'd been blind to for years.\" - Alex, 28")

            PersonalHistoryBlock(title = "Past moments", history = history)
        }
    }
}

@Composable
private fun MemoryPreviewStack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0F0D15), Color(0xFF191622)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(PePurple.copy(alpha = 0.12f)),
        )
        MemoryGhostCard(
            glyph = "♡",
            tag = "Relationships",
            footer = "Patterns",
            border = PePurple.copy(alpha = 0.4f),
            brush = Brush.horizontalGradient(
                listOf(PePurple.copy(alpha = 0.08f), PePink.copy(alpha = 0.07f)),
            ),
            modifier = Modifier
                .offset(x = (-48).dp, y = (-36).dp)
                .rotate(-6f)
                .width(170.dp)
                .height(110.dp),
        )
        MemoryGhostCard(
            glyph = "☆",
            tag = "Emotions",
            footer = "Growth",
            border = PePink.copy(alpha = 0.25f),
            brush = Brush.horizontalGradient(
                listOf(PePink.copy(alpha = 0.06f), PePurple.copy(alpha = 0.06f)),
            ),
            modifier = Modifier
                .offset(x = 4.dp, y = 8.dp)
                .rotate(4f)
                .width(190.dp)
                .height(120.dp),
        )
        MemoryGhostCard(
            glyph = "◇",
            tag = "Experiences",
            footer = "Story",
            border = PePurple.copy(alpha = 0.25f),
            brush = Brush.horizontalGradient(
                listOf(PePurple.copy(alpha = 0.05f), PePink.copy(alpha = 0.05f)),
            ),
            modifier = Modifier
                .offset(x = 56.dp, y = 40.dp)
                .rotate(-8f)
                .width(160.dp)
                .height(100.dp),
        )
    }
}

@Composable
private fun MemoryGhostCard(
    glyph: String,
    tag: String,
    footer: String,
    border: Color,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    val faint = Color.White.copy(alpha = 0.1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = faint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                tag,
                color = faint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Text(footer, color = faint, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun HowMemoryWorksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeIconCircle(glyph = "◎", accent = PePurple, deep = Color(0xFF4F46E5), size = 36.dp)
            Text(
                "How Memory Intelligence Works",
                color = PeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MemoryStepCard("◉", "Capture", "Log moments daily", PePurple, Color(0xFF4F46E5), Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_personal_empty_arrow),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            MemoryStepCard("◈", "Analyze", "AI finds your patterns", PeGreen, Color(0xFF0F766E), Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_personal_empty_arrow),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            MemoryStepCard("✦", "Remember", "Deep insights emerge", PeAmber, Color(0xFFEA580C), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MemoryStepCard(
    glyph: String,
    title: String,
    body: String,
    accent: Color,
    deep: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeIconCircle(glyph = glyph, accent = accent, deep = deep, size = 32.dp)
        Text(title, color = PeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(body, color = PeSecondary, fontSize = 10.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun ModulesAwaitingDataSection() {
    data class Module(val glyph: String, val title: String, val subtitle: String, val accent: Color, val deep: Color)

    val modules = listOf(
        Module("◈", "Patterns", "Analyzing recurring behaviors", PePurple, Color(0xFF4F46E5)),
        Module("◎", "Recovery Anchors", "Identifying reset patterns", PeGreen, Color(0xFF0F766E)),
        Module("/", "Progress Signals", "Mapping momentum", PeBlue, Color(0xFF2563EB)),
        Module("✦", "Experience Highlights", "Extracting emotional resonance", PeAmber, Color(0xFFEA580C)),
        Module("♡", "Relationship Learning", "Synthesizing connection lessons", PePink, Color(0xFFBE185D)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeIconCircle(glyph = "▣", accent = PePurple, deep = Color(0xFF4F46E5), size = 36.dp)
            Text(
                "Modules Awaiting Data",
                color = PeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        modules.forEach { module ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PeIconCircle(glyph = module.glyph, accent = module.accent, deep = module.deep, size = 28.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(module.title, color = PeText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(module.subtitle, color = PeSubtle, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
                Text(
                    "PENDING",
                    color = PeSubtle,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun WhatYoullUnlockSection() {
    data class Unlock(val glyph: String, val title: String, val body: String, val accent: Color, val deep: Color)

    val cards = listOf(
        Unlock("◎", "Identity Snapshot", "A real-time portrait of who you are", PePurple, Color(0xFF4F46E5)),
        Unlock("◈", "Pattern Detection", "Recurring behaviors and cycles revealed", PeGreen, Color(0xFF0F766E)),
        Unlock("↗", "Growth Trajectory", "Where you're heading and what's changing", PeAmber, Color(0xFFEA580C)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "What You'll Unlock",
            color = PeText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        cards.forEach { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(card.accent),
                )
                PeIconCircle(glyph = card.glyph, accent = card.accent, deep = card.deep, size = 40.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(card.title, color = PeText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(card.body, color = PeSecondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_personal_empty_lock),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Unlocks after 7 moments",
                        color = PeSubtle,
                        fontSize = 9.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}
