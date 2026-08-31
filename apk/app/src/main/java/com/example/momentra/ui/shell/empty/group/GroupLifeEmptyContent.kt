package com.example.momentra.ui.shell.empty.group

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma 575:8660 â€” Group / Life empty */
@Composable
fun GroupLifeEmptyContent(
    onCreateMoment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeBg)
            .verticalScroll(rememberScrollState()),
    ) {
        GeAppear {
            GeFigmaHeroExport(
                resId = R.drawable.group_life_hero,
                aspectRatio = 402f / 351f,
                onCta = onCreateMoment,
                ctaLabel = "Create First Group Moment",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GeChapterLabel("Chapter 02 / Lifeline Dimensions")
            Text("Activate Your Dimensions", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DimensionCard(
                    iconRes = R.drawable.group_life_sparkles,
                    accent = GeOrange,
                    title = "Experience",
                    modifier = Modifier.weight(1f),
                    onSetUp = onCreateMoment,
                )
                DimensionCard(
                    iconRes = R.drawable.group_life_icon_wallet,
                    accent = Color(0xFF60A5FA),
                    title = "Purchase",
                    modifier = Modifier.weight(1f),
                    onSetUp = onCreateMoment,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DimensionCard(
                    iconRes = R.drawable.group_life_icon_calendar,
                    accent = Color(0xFF10B981),
                    title = "Living",
                    modifier = Modifier.weight(1f),
                    onSetUp = onCreateMoment,
                )
                DimensionCard(
                    iconRes = R.drawable.group_life_icon_award,
                    accent = Color(0xFFEC4899),
                    title = "Goal",
                    modifier = Modifier.weight(1f),
                    onSetUp = onCreateMoment,
                )
            }
            DimensionCard(
                iconRes = R.drawable.group_life_icon_users,
                accent = Color(0xFF8B5CF6),
                title = "Community",
                modifier = Modifier.fillMaxWidth(),
                onSetUp = onCreateMoment,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GeChapterLabel("Chapter 03 / System Secrets")
            Text("Intelligence Unlocks", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            UnlockCard(
                iconRes = R.drawable.group_life_icon_users,
                accent = GeOrange,
                title = "Participation patterns",
                body = "Who powers the pulse and drives consistent action.",
            )
            UnlockCard(
                iconRes = R.drawable.group_life_icon_wallet,
                accent = Color(0xFF14B8A6),
                title = "Contribution balance",
                body = "Keep spending, funding, and splitting completely fair.",
            )
            UnlockCard(
                iconRes = R.drawable.group_life_icon_calendar,
                accent = Color(0xFFF2CA50),
                title = "Coordination health",
                body = "Real-time updates and seamless timeline synchronization.",
            )
            UnlockCard(
                iconRes = R.drawable.group_life_icon_award,
                accent = Color(0xFF8B5CF6),
                title = "Shared achievement",
                body = "Milestones unlocked together as a united group.",
            )
            UnlockCard(
                iconRes = R.drawable.group_life_icon_sparkles,
                accent = Color(0xFF60A5FA),
                title = "Memory evolution",
                body = "Watch how your shared traditions and rituals grow.",
            )
            Text(
                "Insights unlock naturally as your group creates and completes moments together.",
                color = GeSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GeChapterLabel("Chapter 04 / Philosophy")
            Text("Why These Matter", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            WhyRow("01", "Experience", "Builds connection through shared time.")
            WhyRow("02", "Purchase", "Makes collective decisions and money visible.")
            WhyRow("03", "Living", "Turns everyday coordination into clarity.")
            WhyRow("04", "Goal", "Keeps ambition measurable and shared.")
            WhyRow("05", "Community", "Shows how your group contributes beyond itself.")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GeCtaBrush)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Explore Group Types"
                    }
                    .clickable(onClick = onCreateMoment),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Explore Group Types",
                        color = GeCtaText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Image(
                        painter = painterResource(R.drawable.group_life_arrow_right),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionCard(
    iconRes: Int,
    accent: Color,
    title: String,
    modifier: Modifier,
    onSetUp: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, GeBorder, RoundedCornerShape(18.dp))
            .background(GeCard)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(iconRes), null, Modifier.size(22.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = GeText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Inactive", color = GeSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.2f))
                .clickable(onClick = onSetUp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(painterResource(R.drawable.group_life_sparkles), null, Modifier.size(14.dp))
            Text("Set Up", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun UnlockCard(iconRes: Int, accent: Color, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .background(GeCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(iconRes), null, Modifier.size(22.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.group_life_icon_lock), null, Modifier.size(18.dp))
            }
        }
        Text(title, color = GeText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(body, color = GeSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        GeShimmerBar(accent = accent)
    }
}

@Composable
private fun WhyRow(badge: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, GeOrangeSoft.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .background(GeCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GeOrange.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = GeOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = GeText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, color = GeSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
