package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma 575:8838 — Group / Memory empty */
@Composable
fun GroupMemoryEmptyContent(
    onCreateMoment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // 804x1100 @2x → 402:550 (polaroids + copy baked into Figma export)
        GeAppear {
            GeFigmaHeroExport(
                resId = R.drawable.group_memory_hero,
                aspectRatio = 402f / 550f,
                onCta = onCreateMoment,
                ctaLabel = "Create Your First Memory",
            )
        }

        GeAppear(delayMillis = 120) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GeChapterLabel("Chapter 02 / Preservation")
                Text("What We Learn", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)

                LearnCard(
                    iconRes = R.drawable.group_memory_icon_calendar,
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFFFD8A8), Color(0xFFFFB598))),
                    cardTint = Color(0xFFFFB598).copy(alpha = 0.2f),
                    title = "Shared rituals",
                    body = "The moments and recurring plans your group returns to.",
                )
                LearnCard(
                    iconRes = R.drawable.group_memory_icon_award,
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFF2CA50), Color(0xFFF2CA50))),
                    cardTint = Color(0xFFF2CA50).copy(alpha = 0.2f),
                    title = "Milestones",
                    body = "The landmark achievements that define your journey.",
                )
                LearnCard(
                    iconRes = R.drawable.group_memory_icon_users,
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFFFB598), Color(0xFFFF7A3D))),
                    cardTint = Color(0xFFFF7A3D).copy(alpha = 0.2f),
                    title = "People and roles",
                    body = "Reveal how everyone shows up and supports each other.",
                )
                LearnCard(
                    iconRes = R.drawable.group_memory_icon_map_pin,
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFFFB598), Color(0xFFFFD8A8))),
                    cardTint = Color(0xFFFFD8A8).copy(alpha = 0.2f),
                    title = "Places and patterns",
                    body = "Map where your group's strongest memories are made.",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Your memory begins with the first shared moment.",
                        color = GeText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Create something together and Momentra will preserve what matters.",
                        color = GeSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnCard(
    iconRes: Int,
    iconBrush: Brush,
    cardTint: Color,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, GeBorder, RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(cardTint, GeCard)))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBrush),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(iconRes), null, Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = GeText, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
            Text(body, color = GeSecondary, fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}
