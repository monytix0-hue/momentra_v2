package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma 575:8553 — Group / Moments empty */
@Composable
fun GroupMomentsEmptyContent(
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
                resId = R.drawable.group_moments_hero,
                aspectRatio = 402f / 520f,
                onCta = onCreateMoment,
                ctaLabel = "Start Your First Moment",
            )
        }

        GeAppear(delayMillis = 90) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GeChapterLabel("Chapter 02 / Type Selection")
                Text("Moment Types", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                GeMomentTypeGrid()
            }
        }

        GeAppear(delayMillis = 180) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GeChapterLabel("Chapter 03 / How It Works")
                Text("Your journey, mapped.", color = GeText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(
                    "Follow the path from idea to memory - with a little magic along the way.",
                    color = GeSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                GeTimelineStep(1, "Create a Moment", "Set your objective with friends or family.", leftAligned = true)
                GeTimelineStep(2, "Invite Your People", "Share the link to bring everyone on board.", leftAligned = false)
                GeTimelineStep(3, "Plan & Contribute", "Pool money, schedule tasks, and lock dates.", leftAligned = true)
                GeTimelineStep(4, "Stay In Sync", "Evolve plans fluidly as life happens.", leftAligned = false)
                GeTimelineStep(5, "Keep Memories", "Every shared journey archives naturally.", leftAligned = true)
                Text(
                    "Every shared story starts with a single moment.",
                    color = GeSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun GeTimelineStep(
    number: Int,
    title: String,
    body: String,
    leftAligned: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (leftAligned) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leftAligned) {
            GeStepBadge(number)
            Spacer(Modifier.width(16.dp))
            GeStepCard(title, body, Modifier.weight(1f))
        } else {
            GeStepCard(title, body, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            GeStepBadge(number)
        }
    }
}

@Composable
private fun GeStepBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GeOrange),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = GeCtaText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun GeStepCard(title: String, body: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GeBorder, RoundedCornerShape(16.dp))
            .background(GeCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title.uppercase(), color = GeText, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
        Text(body, color = GeSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
