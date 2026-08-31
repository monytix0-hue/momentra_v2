package com.example.momentra.ui.shell

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.shell.empty.personal.PeAppear
import com.example.momentra.ui.shell.empty.personal.PeBg
import com.example.momentra.ui.shell.empty.personal.PeAmber
import com.example.momentra.ui.shell.empty.personal.PeGradientPrimaryButton
import com.example.momentra.ui.shell.empty.personal.PeGreen
import com.example.momentra.ui.shell.empty.personal.PeIconCircle
import com.example.momentra.ui.shell.empty.personal.PePink
import com.example.momentra.ui.shell.empty.personal.PePurple
import com.example.momentra.ui.shell.empty.personal.PeSecondary
import com.example.momentra.ui.shell.empty.personal.PeSubtle
import com.example.momentra.ui.shell.empty.personal.PeText
import com.example.momentra.ui.shell.empty.personal.PersonalHistoryBlock
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `353:5783` body — Personal Life empty. */
@Composable
fun PersonalLifeEmptyContent(
    onStartCta: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "See Your Whole Life in One Place",
                    color = PeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Momentra connects your daily moments across all life areas to reveal the bigger picture - your energy, growth, balance, and trajectory.",
                    color = PeSecondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HowLifeWorksSection()

            LifePillarsSection()

            WhatYoullDiscoverSection()

            SocialProofCard()

            PeGradientPrimaryButton(
                label = "✨ Start Building Your Life Graph",
                onClick = onStartCta,
            )
            Text(
                "Takes less than a minute to begin",
                color = PeSubtle,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            PersonalHistoryBlock(title = "Past moments", history = history)
        }
    }
}

@Composable
private fun HowLifeWorksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PePurple),
                contentAlignment = Alignment.Center,
            ) {
                Text("✨", fontSize = 16.sp)
            }
            Text(
                "How Life Intelligence Works",
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
            LifeStepCard(
                emoji = "📝",
                title = "Log",
                body = "Capture moments across all 4 life areas",
                accent = PePurple,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(R.drawable.ic_personal_empty_arrow),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            LifeStepCard(
                emoji = "🔗",
                title = "Connect",
                body = "Momentra finds patterns between your areas",
                accent = PeGreen,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(R.drawable.ic_personal_empty_arrow),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            LifeStepCard(
                emoji = "💡",
                title = "Reveal",
                body = "See your complete life intelligence graph",
                accent = PeAmber,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LifeStepCard(
    emoji: String,
    title: String,
    body: String,
    accent: Color,
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Text(title, color = PeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(body, color = PeSecondary, fontSize = 10.sp, fontFamily = PlusJakartaSans, lineHeight = 13.sp)
    }
}

@Composable
private fun LifePillarsSection() {
    data class Pillar(val glyph: String, val title: String, val subtitle: String, val accent: Color, val deep: Color)

    val pillars = listOf(
        Pillar("▣", "Life Operations", "Money, routines, commitments", PePurple, Color(0xFF4F46E5)),
        Pillar("↗", "Future Building", "Goals, growth, milestones", PeGreen, Color(0xFF0F766E)),
        Pillar("◇", "Lifestyle", "Experiences, wellbeing, creativity", PeAmber, Color(0xFFEA580C)),
        Pillar("♡", "Relationships", "Connections, care, shared moments", PePink, Color(0xFFBE185D)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3A3842))
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
                "Your 4 Life Pillars",
                color = PeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        pillars.forEach { pillar ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PeIconCircle(glyph = pillar.glyph, accent = pillar.accent, deep = pillar.deep, size = 32.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(pillar.title, color = PeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(pillar.subtitle, color = PeSubtle, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, pillar.accent, RoundedCornerShape(2.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "0 of 4 areas active",
                color = Color(0xFFC9BFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, PePurple, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun WhatYoullDiscoverSection() {
    data class Unlock(
        val glyph: String,
        val title: String,
        val body: String,
        val accent: Color,
        val deep: Color,
    )

    val cards = listOf(
        Unlock("◎", "Life Health Score", "A real-time composite of your wellbeing across all areas", PePurple, Color(0xFF4F46E5)),
        Unlock("◈", "Cross-Area Patterns", "How your finances affect your relationships, how goals impact energy", PeGreen, Color(0xFF0F766E)),
        Unlock("↗", "Trajectory Forecast", "Where your life is heading based on current momentum", PeAmber, Color(0xFFEA580C)),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3A3842))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeIconCircle(glyph = "✦", accent = PePink, deep = Color(0xFFBE185D), size = 36.dp)
            Text(
                "What You'll Discover",
                color = PeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        cards.forEach { card ->
            LockedFeatureRow(
                glyph = card.glyph,
                title = card.title,
                body = card.body,
                accent = card.accent,
                deep = card.deep,
                lockLabel = "Unlocks after 7 moments",
            )
        }
    }
}

@Composable
private fun LockedFeatureRow(
    glyph: String,
    title: String,
    body: String,
    accent: Color,
    deep: Color,
    lockLabel: String,
) {
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
                .background(accent),
        )
        PeIconCircle(glyph = glyph, accent = accent, deep = deep, size = 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = PeText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(body, color = PeSecondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_personal_empty_lock),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(lockLabel, color = PeSubtle, fontSize = 9.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun SocialProofCard() {
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
            "\"After 2 weeks, I finally understood why I felt drained every Thursday.\" - Sandeep",
            color = PeText,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = PlusJakartaSans,
            lineHeight = 20.sp,
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
