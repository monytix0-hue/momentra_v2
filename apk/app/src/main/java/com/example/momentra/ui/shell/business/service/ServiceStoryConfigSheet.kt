package com.example.momentra.ui.shell.business.service

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Create Business Story — Figma `RTL-STORY-CONFIG · IG · Create story` `2670:33783`.
 *
 * The story endpoint regenerates the moment's recorded window and takes no body, so the
 * period, detail and GST controls cannot be honoured. They stay visible (the frame shows
 * them) but read as unavailable with the reason, instead of silently doing nothing.
 */
@Composable
fun ServiceStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val storeName = model.title?.takeIf { it.isNotBlank() } ?: "Consumer Service"
    val month = ServiceFormat.month(model.startDate)
    val netSales = ServiceFormat.compact(model.currency, model.finance?.sales, model.financeVisible)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ServicesTeal.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ServicesTeal.chip)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = ServicesTeal.textBody, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        StoryPreviewThumb(netSales = netSales, month = month)

        Text(
            "Your ${month.substringBefore(' ')} story is ready to compose",
            color = ServicesTeal.textBody,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(ServicesTeal.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(ServicesTeal.tealMuted),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Create Business Story",
                    color = ServicesTeal.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text(
                    "$storeName · 8 slides",
                    color = ServicesTeal.textBody,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                )
            }

            // Period chips: the story covers the moment's recorded window only.
            StoryPeriodChips(selected = month)

            // Detail level: one composition exists today.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ServicesTeal.field)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StorySegment("Summary", selected = true, Modifier.weight(1f))
                StorySegment("Detailed", selected = false, Modifier.weight(1f))
            }

            if (model.gstTracking == "YES") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Include GST snapshot",
                            color = ServicesTeal.textFaint,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                        Text(
                            "Recorded GST is not part of this story build",
                            color = ServicesTeal.textFaint,
                            fontFamily = PlusJakartaSans,
                            fontSize = 10.sp,
                        )
                    }
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(Modifier.padding(start = 3.dp).size(18.dp).clip(CircleShape).background(ServicesTeal.textFaint))
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(ServicesTeal.rCardTight)
                    .background(ServicesTeal.teal)
                    .shadow(14.dp, ServicesTeal.rCardTight, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.45f), spotColor = ServicesTeal.teal.copy(alpha = 0.45f))
                    .clickable(enabled = !generating) {
                        scope.launch {
                            generating = true
                            error = null
                            runCatching {
                                ApiClient.apiService.createMomentStory(
                                    momentId = momentId,
                                    idempotencyKey = UUID.randomUUID().toString(),
                                )
                            }.onSuccess {
                                OperatingChromeState.previewStoryId = momentId
                                onDismiss()
                            }.onFailure { error = it.message ?: "Could not generate story" }
                            generating = false
                        }
                    }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (generating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                } else {
                    Icon(
                        painterResource(R.drawable.qa_plus),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    if (generating) "Composing…" else "Generate story",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            if (error != null) {
                Text(
                    error ?: "",
                    color = ServicesTeal.negative,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                )
            }

            WhyNote(
                "Every slide is built from recorded activity. Nothing is estimated, and an empty section says so.",
                tinted = true,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** 140 × 300 story thumb: 8-segment progress, cover eyebrow, hero metric, caption. */
@Composable
private fun StoryPreviewThumb(netSales: String, month: String) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(width = 140.dp, height = 300.dp)
                .shadow(24.dp, shape, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.35f), spotColor = ServicesTeal.teal.copy(alpha = 0.35f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1A1F), Color(0xFF0B0A1F))))
                .border(1.dp, ServicesTeal.teal.copy(alpha = 0.35f), shape)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(8) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (index == 0) ServicesTeal.teal else Color.White.copy(alpha = 0.22f)),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "THE BUSINESS MOMENT",
                    color = ServicesTeal.teal,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 6.sp,
                )
                Text(
                    netSales,
                    color = ServicesTeal.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "net sales · $month",
                    color = ServicesTeal.textBody,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 7.sp,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .size(width = 46.dp, height = 6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun StoryPeriodChips(selected: String) {
    val periods = listOf("This week", "This month", "Last month", "Quarter", "Year", "Custom")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            periods.take(3).forEach { PeriodChip(it, it == selected, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            periods.drop(3).forEach { PeriodChip(it, it == selected, Modifier.weight(1f)) }
        }
        Text(
            "Period follows the moment's recorded window — regenerate covers that range only.",
            color = ServicesTeal.textFaint,
            fontFamily = PlusJakartaSans,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        color = if (selected) Color.White else ServicesTeal.textFaint,
        fontFamily = PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) ServicesTeal.teal else ServicesTeal.chip)
            .padding(vertical = 8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun StorySegment(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        color = if (selected) ServicesTeal.textPrimary else ServicesTeal.textFaint,
        fontFamily = PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 14.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ServicesTeal.chip else Color.Transparent)
            .padding(vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
