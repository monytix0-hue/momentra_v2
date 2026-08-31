package com.example.momentra.ui.shell.empty.personal

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `353:394` body — Personal Moments empty. */
@Composable
fun PersonalMomentsEmptyContent(
    onCreateMoment: () -> Unit,
    history: List<MomentSummary> = emptyList(),
    modifier: Modifier = Modifier,
) {
    PeAppear {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0F0D15), Color(0xFF191622))))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Your Story Starts Here",
                    color = PeText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Every moment you capture becomes part of your personal narrative.",
                    color = PeMuted,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            JourneyTimeline(onCreateMoment = onCreateMoment)

            TestimonialCard()

            WhatAwaitsYouSection()

            MilestonesSection()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PeGradientPrimaryButton(
                    label = "✨ Create Your First Moment",
                    onClick = onCreateMoment,
                )
                Text(
                    "It only takes 30 seconds",
                    color = PeSubtle,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }

            PersonalHistoryBlock(title = "Past moments", history = history)
        }
    }
}

@Composable
private fun JourneyTimeline(onCreateMoment: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(344.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 32.dp, top = 36.dp)
                .width(4.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(PePurple, PeGreen))),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TimelineNode(
                glyph = "◎",
                accent = Color(0xFF4F46E5),
                deep = Color(0xFF2E26A8),
                title = "Where you've been",
                subtitle = "Patterns will emerge from your history",
                highlighted = false,
            )
            TimelineNode(
                glyph = "⚡",
                accent = Color(0xFF4F46E5),
                deep = Color(0xFF2E26A8),
                title = "Where you are now",
                subtitle = "Start capturing this moment",
                highlighted = true,
                circleSize = 44.dp,
                onCapture = onCreateMoment,
            )
            TimelineNode(
                glyph = "☆",
                accent = Color(0xFF0F766E),
                deep = Color(0xFF0A5A4C),
                title = "Where you're going",
                subtitle = "Your future self will thank you",
                highlighted = false,
            )
        }
    }
}

@Composable
private fun TimelineNode(
    glyph: String,
    accent: Color,
    deep: Color,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    circleSize: Dp = 40.dp,
    onCapture: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeIconCircle(glyph = glyph, accent = accent, deep = deep, size = circleSize)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .then(
                    if (highlighted) {
                        Modifier.border(2.dp, PePurple, RoundedCornerShape(16.dp))
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    },
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (highlighted) 12.dp else 8.dp),
        ) {
            Text(
                title,
                color = PeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                subtitle,
                color = PeMuted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            if (onCapture != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PePurple)
                        .clickable(onClick = onCapture)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Capture Now →",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
private fun TestimonialCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, PePurple, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PePurple),
            contentAlignment = Alignment.Center,
        ) {
            Text("\"", color = Color.White, fontSize = 18.sp)
        }
        Text(
            "After 30 days, I noticed patterns I never saw before. Momentra helped me understand my energy cycles and relationship dynamics.",
            color = PeText,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = PlusJakartaSans,
            lineHeight = 20.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "- Santosh, using Momentra for 3 months",
                color = PeMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) {
                    Image(
                        painter = painterResource(R.drawable.ic_personal_empty_star),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatAwaitsYouSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "What awaits you",
                color = PeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text("✦", color = Color(0xFF4F46E5), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewUnlockCard(
                emoji = "📊",
                title = "Your Pulse",
                subtitle = "Real-time insights",
                accent = PePurple,
                modifier = Modifier.weight(1f),
            )
            PreviewUnlockCard(
                emoji = "🧠",
                title = "Your Memory",
                subtitle = "Deep patterns",
                accent = PeGreen,
                modifier = Modifier.weight(1f),
            )
            PreviewUnlockCard(
                emoji = "🌊",
                title = "Your Life",
                subtitle = "Full picture",
                accent = PeBlue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewUnlockCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 14.sp)
            }
            Text(
                title,
                color = PeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
            )
            Text(
                subtitle,
                color = PeMuted,
                fontSize = 10.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_personal_empty_lock),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    "Unlock with first moment",
                    color = PeSubtle,
                    fontSize = 8.sp,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MilestonesSection() {
    data class Milestone(val glyph: String, val label: String, val accent: Color, val deep: Color, val locked: Boolean)

    val items = listOf(
        Milestone("◉", "First Moment", Color(0xFF4F46E5), Color(0xFF2E26A8), false),
        Milestone("▲", "7-Day Streak", Color(0xFFEA580C), Color(0xFFB45309), true),
        Milestone("◈", "Pattern Found", Color(0xFF0F766E), Color(0xFF0A5A4C), true),
        Milestone("◇", "30 Days", Color(0xFF2563EB), Color(0xFF1D4ED8), true),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Milestones to unlock",
                color = PeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text("✦", color = Color(0xFFEA580C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (item.locked) 0.5f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeIconCircle(
                        glyph = item.glyph,
                        accent = item.accent,
                        deep = item.deep,
                        size = 44.dp,
                    )
                    Text(
                        item.label,
                        color = if (item.locked) PeSubtle else PeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
