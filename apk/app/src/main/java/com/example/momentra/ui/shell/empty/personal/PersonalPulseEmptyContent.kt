package com.example.momentra.ui.shell.empty.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `353:320` body — Personal Pulse empty.
 * S2-B: Educational preview art only — not live inventory, balances, or Pulse metrics.
 * Real emptiness comes from bootstrap Moment inventory via S1 shell Offline/Error/Empty states.
 */
@Composable
fun PersonalPulseEmptyContent(
    onCreateMoment: () -> Unit,
    history: List<MomentSummary> = emptyList(),
    modifier: Modifier = Modifier,
) {
    PeAppear {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1726), Color(0xFF0F0D15))))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Your Personal Operating System",
                    color = PeText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Life moves through commitments, money, energy, experiences, and relationships.",
                    color = Color(0xFFB8B0C8),
                    fontSize = 15.sp,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            StartYourJourneyCard(onClick = onCreateMoment)

            FuturePulsePreviewCard(onCreateMoment = onCreateMoment)

            OperationalSignalsSection()

            WhatMomentraLearnsSection()

            PeQuoteCard(text = "\"Start your journey to build a system that grows with you.\"")

            PersonalHistoryBlock(title = "Recent moments", history = history)
        }
    }
}

@Composable
private fun StartYourJourneyCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                ),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PePurple),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                "Start Your Journey",
                color = PeText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Activate your personal operating system.",
                color = PeSecondary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeIconCircle(
                glyph = "↗",
                accent = PePurple,
                deep = Color(0xFF4F46E5),
                size = 48.dp,
            )
            Text(
                "›",
                color = PeMuted,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FuturePulsePreviewCard(onCreateMoment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                ),
            )
            .border(1.dp, PePurple, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0F0D15), Color(0xFF1A1726)))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(PePurple.copy(alpha = 0.08f)),
            )
            PulseMiniCard(
                title = "Activity",
                chip = "Live",
                chipColor = PeGreen,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 8.dp, y = (-28).dp)
                    .rotate(-6f)
                    .width(168.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PeGreen),
                    )
                }
                Text(
                    "68% analyzed",
                    color = Color(0xFFE5E7EB).copy(alpha = 0.75f),
                    fontSize = 11.sp,
                )
            }
            PulseMiniCard(
                title = "Patterns",
                chip = "Trend",
                chipColor = PePurple,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .rotate(6f)
                    .width(168.dp),
            ) {
                SparklineRows()
            }
            PulseMiniCard(
                title = "Signals",
                chip = "New",
                chipColor = PeAmber,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-18).dp)
                    .width(168.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PeAmber.copy(alpha = 0.85f)),
                        )
                    }
                }
            }
        }
        Text(
            "Your future pulse will form here.",
            color = PeMuted,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
        PeGradientPrimaryButton(
            label = "Create My First Moment",
            onClick = onCreateMoment,
            height = 60.dp,
        )
    }
}

@Composable
private fun PulseMiniCard(
    title: String,
    chip: String,
    chipColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, chipColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = Color(0xFFE5E7EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                chip,
                color = chipColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipColor.copy(alpha = 0.1f))
                    .border(1.dp, chipColor.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        content()
    }
}

@Composable
private fun SparklineRows() {
    val heights = listOf(10, 18, 12, 22, 14, 20, 16)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PePurple.copy(alpha = 0.75f)),
            )
        }
    }
}

@Composable
private fun OperationalSignalsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                ),
            )
            .border(1.dp, PePurple, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PersonalSectionLabel("OPERATIONAL SIGNALS")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SignalCard("◉", "Moments", PePurple, Color(0xFF4F46E5), Modifier.weight(1f))
                SignalCard("/", "Runtimes", PeBlue, Color(0xFF2563EB), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SignalCard("◇", "Patterns", PeAmber, Color(0xFFEA580C), Modifier.weight(1f))
                SignalCard("✦", "Guidance", PeGreen, Color(0xFF0F766E), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignalCard(
    glyph: String,
    label: String,
    accent: Color,
    deep: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeIconCircle(glyph = glyph, accent = accent, deep = deep, size = 32.dp)
        Text(
            label,
            color = PeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("-", color = PeSubtle, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.5f)),
            )
        }
    }
}

@Composable
private fun WhatMomentraLearnsSection() {
    val steps = listOf(
        Triple("◎", "1", "Stability") to (PePurple to Color(0xFF4F46E5)),
        Triple("/", "2", "Recovery") to (PeGreen to Color(0xFF0F766E)),
        Triple("/", "3", "Progress") to (PeBlue to Color(0xFF2563EB)),
        Triple("↗", "4", "Momentum") to (PeAmber to Color(0xFFEA580C)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                ),
            )
            .border(1.dp, PePurple, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeIconCircle(glyph = "◎", accent = PePurple, deep = Color(0xFF4F46E5), size = 36.dp)
            Text(
                "WHAT MOMENTRA LEARNS",
                color = PeSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, (copy, colors) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(1.dp)
                            .background(PePurple.copy(alpha = 0.3f)),
                    )
                }
                LearnStepCard(
                    glyph = copy.first,
                    number = copy.second,
                    label = copy.third,
                    accent = colors.first,
                    deep = colors.second,
                )
            }
        }
        Text(
            "Start your journey to build a system that grows with you.",
            color = PeMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LearnStepCard(
    glyph: String,
    number: String,
    label: String,
    accent: Color,
    deep: Color,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeIconCircle(glyph = glyph, accent = accent, deep = deep, size = 32.dp)
        Text(number, color = PeText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(label, color = PeText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}
