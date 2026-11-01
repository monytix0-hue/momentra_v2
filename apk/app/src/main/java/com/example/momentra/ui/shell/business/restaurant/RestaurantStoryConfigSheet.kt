package com.example.momentra.ui.shell.business.restaurant

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.RestaurantEmber
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Create Business Story — Figma `RST-STORY-CONFIG` `2671:32352`.
 *
 * `POST /v1/moments/:momentId/stories` takes no request body and regenerates the
 * moment's recorded window, so the period / detail / GST controls cannot be honoured.
 * They stay visible (the frame shows them) and read as unavailable with the reason
 * printed, instead of pretending the request carries options.
 */
@Composable
fun RestaurantStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val restaurantName = model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"
    val month = RestaurantFormat.month(model.startDate)
    val netSales = RestaurantFormat.compact(model.currency, model.finance?.sales, model.financeVisible)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RestaurantEmber.chip)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✕",
                    color = RestaurantEmber.textBody,
                    fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }

        StoryPreviewThumb(netSales = netSales, month = month)

        Text(
            "Your ${month.substringBefore(' ')} story is ready to compose",
            style = RestaurantEmber.rowCaption().copy(fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(RestaurantEmber.sheet)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(RestaurantEmber.ember.copy(alpha = 0.55f)),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Create Business Story",
                    style = RestaurantEmber.screenTitle().copy(fontSize = 20.sp),
                )
                Text(
                    "$restaurantName · 8 slides",
                    style = RestaurantEmber.rowCaption(),
                )
            }

            // Period chips: the story covers the moment's recorded window only.
            StoryPeriodChips(selected = month)

            // Detail level: one composition exists today.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RestaurantEmber.field)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StorySegment("Summary", selected = true, Modifier.weight(1f))
                StorySegment("Detailed", selected = false, Modifier.weight(1f))
            }

            if (model.gstTracking == "YES") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Include GST snapshot",
                            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint)
                                .copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                        )
                        Text(
                            "Recorded GST is not part of this story build",
                            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
                        )
                    }
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier
                                .padding(start = 3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(RestaurantEmber.textFaint),
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RestaurantEmber.rCardTight)
                    .background(RestaurantEmber.ember)
                    .shadow(
                        14.dp,
                        RestaurantEmber.rCardTight,
                        clip = false,
                        ambientColor = RestaurantEmber.ember.copy(alpha = 0.45f),
                        spotColor = RestaurantEmber.ember.copy(alpha = 0.45f),
                    )
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
                        painterResource(RestaurantTileIcons.plus),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    if (generating) "Composing…" else "Generate story",
                    color = Color.White,
                    fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            error?.let {
                Text(it, style = RestaurantEmber.rowCaption(RestaurantEmber.negative))
            }

            EmberWhyNote(
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
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(width = 140.dp, height = 300.dp)
                .shadow(
                    24.dp,
                    shape,
                    clip = false,
                    ambientColor = RestaurantEmber.ember.copy(alpha = 0.35f),
                    spotColor = RestaurantEmber.ember.copy(alpha = 0.35f),
                )
                .clip(shape)
                .background(Brush.verticalGradient(listOf(RestaurantEmber.wine, RestaurantEmber.screenBottom)))
                .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.35f), shape)
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
                            .background(if (index == 0) RestaurantEmber.ember else Color.White.copy(alpha = 0.22f)),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "THE BUSINESS MOMENT",
                    color = RestaurantEmber.gold,
                    fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 6.sp,
                )
                Text(
                    netSales,
                    color = RestaurantEmber.textPrimary,
                    fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "net sales · $month",
                    color = RestaurantEmber.textBody,
                    fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
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
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
        )
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        color = if (selected) Color.White else RestaurantEmber.textFaint,
        fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) RestaurantEmber.ember else RestaurantEmber.chip)
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StorySegment(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        label,
        color = if (selected) RestaurantEmber.textPrimary else RestaurantEmber.textFaint,
        fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 14.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RestaurantEmber.chip else Color.Transparent)
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
    )
}
