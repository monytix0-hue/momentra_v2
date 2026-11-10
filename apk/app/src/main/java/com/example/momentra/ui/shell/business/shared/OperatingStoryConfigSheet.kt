package com.example.momentra.ui.shell.business.shared

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Create Business Story for Trading and Distribution — `TRD-MEMORY-v12` /
 * `DST-MEMORY-v12`. Same contract as the retail sheet: the endpoint regenerates the
 * moment's recorded window and takes no body, so the period/detail controls stay visible
 * but read as unavailable rather than silently doing nothing.
 */
@Composable
internal fun OperatingStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    noun: String,
    chapters: List<String>,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val name = model.title?.takeIf { it.isNotBlank() } ?: noun.replaceFirstChar { it.uppercase() }
    val month = monthLabel(model.startDate)
    val hero = formatOperatingAmount(model.currency, model.finance?.sales, model.financeVisible)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "✕",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.inset)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
            )
        }
        StoryThumb(hero = hero, month = month, theme = theme)
        Text(
            "Your ${month.substringBefore(' ')} story is ready to compose",
            color = theme.body,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(theme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(theme.border),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Create Business Story", color = theme.ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("$name · ${chapters.size} pages", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
            }
            Text(
                chapters.joinToString(" · ") { it.replaceFirstChar { c -> c.uppercase() } },
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoryChip("Summary", true, theme, Modifier.weight(1f))
                StoryChip("Detailed", false, theme, Modifier.weight(1f))
            }
            Text(
                "Period follows the moment's recorded window — regenerating covers that range only.",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
            )
            val hasActivity = model.activity.isNotEmpty()
            if (!hasActivity) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.inset)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠", color = theme.warning, fontFamily = PlusJakartaSans, fontSize = 18.sp)
                    Text(
                        "No verified activity in this period",
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "We found no reconciled records for $month. We won’t show zeros for data we don’t have.",
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    listOf("Activity" to "Not recorded", "Money" to "Not recorded", "Network" to "Not recorded").forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                            Text(value, color = theme.warning, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (hasActivity) theme.accent else theme.inset)
                    .clickable(enabled = !generating && hasActivity) {
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
                    CircularProgressIndicator(color = theme.canvas, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("+", color = if (hasActivity) theme.canvas else theme.body, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        generating -> "Composing…"
                        !hasActivity -> "Generate · needs activity"
                        else -> "Generate story"
                    },
                    color = if (hasActivity) theme.canvas else theme.body,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (error != null) {
                Text(error ?: "", color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 11.sp)
            }
            OperatingWhyFootnote(
                "Every page is built from recorded activity. Nothing is estimated, and an empty page says so.",
                theme,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StoryThumb(hero: String, month: String, theme: OperatingSheetTheme) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .size(width = 140.dp, height = 300.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(theme.surface, theme.canvas)))
                .border(1.dp, theme.accent.copy(alpha = 0.35f), shape)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(8) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (index == 0) theme.accent else Color.White.copy(alpha = 0.22f)),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "THE BUSINESS MOMENT",
                    color = theme.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 6.sp,
                )
                Text(
                    hero,
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "recorded sales · $month",
                    color = theme.body,
                    fontFamily = PlusJakartaSans,
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
private fun StoryChip(label: String, selected: Boolean, theme: OperatingSheetTheme, modifier: Modifier) {
    Text(
        label,
        color = if (selected) theme.canvas else theme.body,
        fontFamily = PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) theme.accent else theme.inset)
            .padding(vertical = 10.dp),
    )
}

private fun monthLabel(start: String?): String {
    val date = start?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
}
