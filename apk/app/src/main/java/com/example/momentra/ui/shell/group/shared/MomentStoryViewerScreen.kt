package com.example.momentra.ui.shell.group.shared

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.MomentStoryDto
import com.example.momentra.data.api.MomentStoryMetricKeyDto
import com.example.momentra.data.api.MomentStoryMoneyCategoryDto
import com.example.momentra.data.api.MomentStoryMoneyExpenseDto
import com.example.momentra.data.api.MomentStorySnapshotDto
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Moment Story viewer — cream editorial + purple cover (Figma 5-chapter redesign).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentStoryViewerScreen(
    momentId: String,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var phase by remember { mutableStateOf("loading") }
    var message by remember { mutableStateOf<String?>(null) }
    var story by remember { mutableStateOf<MomentStoryDto?>(null) }
    var sharing by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            phase = "loading"
            message = null
            try {
                val status = ApiClient.apiService.getMomentStoryStatus(momentId).data
                when (status.status) {
                    "GENERATING" -> {
                        phase = "generating"
                        return@launch
                    }
                    "FAILED" -> {
                        phase = "failed"
                        message = status.errorMessage ?: "Story generation failed."
                        return@launch
                    }
                    "NOT_STARTED" -> {
                        phase = "not_started"
                        message = "No Story yet — complete the moment to unlock it."
                        return@launch
                    }
                }
                story = ApiClient.apiService.getMomentStory(momentId).data
                phase = "ready"
            } catch (e: Exception) {
                phase = "error"
                message = e.message ?: "Could not load Story"
            }
        }
    }

    LaunchedEffect(momentId) { reload() }

    val chapters = resolveStoryChapters(story)
    val pagerState = rememberPagerState(pageCount = { chapters.size.coerceAtLeast(1) })
    val snap = story?.snapshot
    val page = chapters.getOrNull(pagerState.currentPage) ?: "cover"
    val chromeDark = page == "cover" || page == "close"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(
                when (page) {
                    "cover" -> MomentraBrandColors.Indigo700
                    "close" -> MomentraBrandColors.StoryDarkClose
                    else -> MomentraBrandColors.StoryCream
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    painter = painterResource(R.drawable.momentra_official_logo),
                    contentDescription = "Momentra",
                    modifier = Modifier.height(28.dp).width(28.dp),
                    contentScale = ContentScale.Fit,
                )
                Column {
                    Text(
                        "momentra",
                        color = if (chromeDark) MomentraBrandColors.TextOnDark else MomentraBrandColors.StoryTerracotta,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        fontSize = 18.sp,
                    )
                    Text(
                        snap?.display?.displayLabel ?: "Moment Story",
                        color = if (chromeDark) MomentraBrandColors.Indigo100 else MomentraBrandColors.StoryMuted,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            TextButton(onClick = onClose) {
                Text(
                    "Close",
                    color = if (chromeDark) MomentraBrandColors.Ember500 else MomentraBrandColors.StoryAccentPurple,
                )
            }
        }

        when (phase) {
            "loading", "generating" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MomentraBrandColors.Ember500)
                    if (phase == "generating") {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Generating your Moment Story…",
                            color = if (chromeDark) MomentraBrandColors.TextOnDark else MomentraBrandColors.StoryInk,
                        )
                    }
                }
            }
            "failed", "error" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        message ?: "Could not load Story",
                        color = if (chromeDark) MomentraBrandColors.TextOnDark else MomentraBrandColors.StoryInk,
                        modifier = Modifier.padding(24.dp),
                    )
                    Button(
                        onClick = { reload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Ember500),
                    ) { Text("Retry") }
                }
            }
            "not_started" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    message ?: "No Story yet — complete the moment to unlock it.",
                    color = if (chromeDark) MomentraBrandColors.TextOnDark else MomentraBrandColors.StoryInk,
                    modifier = Modifier.padding(24.dp),
                )
            }
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { idx ->
                    val chapter = chapters.getOrNull(idx) ?: "cover"
                    StoryChapterPage(chapter = chapter, story = story)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            if (sharing) return@Button
                            scope.launch {
                                sharing = true
                                try {
                                    val pack = ApiClient.apiService.getMomentStorySharePack(momentId).data
                                    val link = pack.webUrl?.takeIf { it.startsWith("https://") }
                                    if (link.isNullOrBlank()) {
                                        Toast.makeText(context, "Story link is not ready.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val blurb = pack.blurb?.trim().orEmpty()
                                    val title = snap?.identity?.title ?: "Moment Story"
                                    val text = buildString {
                                        append(if (blurb.isNotEmpty()) blurb else title)
                                        append("\n")
                                        append(link)
                                    }
                                    shareMomentStory(context, text)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Could not share story", Toast.LENGTH_SHORT).show()
                                } finally {
                                    sharing = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !sharing,
                        colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Ember500),
                    ) { Text("Share") }
                    Button(
                        onClick = {
                            if (sharing) return@Button
                            scope.launch {
                                sharing = true
                                try {
                                    val bytes = withContext(Dispatchers.IO) {
                                        ApiClient.apiService.getMomentStoryPdf(momentId).bytes()
                                    }
                                    shareMomentStoryPdf(context, bytes)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Could not share PDF", Toast.LENGTH_SHORT).show()
                                } finally {
                                    sharing = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !sharing,
                        colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Indigo700),
                    ) { Text("Share PDF") }
                    Text(
                        "${pagerState.currentPage + 1} / ${chapters.size}",
                        color = if (chromeDark) MomentraBrandColors.Indigo100 else MomentraBrandColors.StoryMuted,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

private fun moneyChapterEmpty(story: MomentStoryDto?): Boolean {
    val money = story?.snapshot?.money ?: return true
    return (money.contributed ?: 0.0) == 0.0 &&
        (money.spent ?: 0.0) == 0.0 &&
        (money.remaining ?: 0.0) == 0.0 &&
        money.expenses.isNullOrEmpty()
}

private fun resolveStoryChapters(story: MomentStoryDto?): List<String> {
    val raw = story?.chapters ?: story?.snapshot?.chapters
    val chapters = if (raw != null && raw.any { it == "moment" || it == "together" }) {
        raw
    } else {
        listOf("cover", "moment", "together", "money", "close")
    }
    return if (moneyChapterEmpty(story)) chapters.filter { it != "money" } else chapters
}

private fun storyCurrency(snap: MomentStorySnapshotDto?): String =
    snap?.identity?.currencyCode?.takeIf { it.length == 3 } ?: "INR"

private fun celebrationFamily(family: String?): Boolean =
    family == "HOUSE_PARTY" || family == "WEDDING" || family == "SHARED_EXPERIENCE"

@Composable
private fun StoryChapterPage(chapter: String, story: MomentStoryDto?) {
    val snap = story?.snapshot
    when (chapter) {
        "cover" -> CoverChapter(snap)
        "moment", "memories" -> MomentChapter(snap)
        "together", "alive" -> TogetherChapter(snap)
        "money" -> MoneyChapter(snap)
        else -> CloseChapter(snap)
    }
}

@Composable
private fun CoverChapter(snap: MomentStorySnapshotDto?) {
    val title = snap?.identity?.title ?: "Moment"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MomentraBrandColors.Indigo500.copy(alpha = 0.35f))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            (snap?.display?.coverEyebrow ?: "Moment Story").uppercase(),
            color = MomentraBrandColors.Ember500,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = MomentraBrandColors.TextOnDark,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val place = snap?.places?.firstOrNull()?.label
            val range = dateRangeLabel(snap?.identity?.startAt, snap?.identity?.endAt)
            val people = metricInt(snap?.metrics, "people")
            val days = metricInt(snap?.metrics, "days")
            if (!place.isNullOrBlank()) CoverPill(place, MomentraBrandColors.Teal700)
            if (!range.isNullOrBlank()) CoverPill(range, MomentraBrandColors.Indigo300)
            if (people != null || days != null) {
                CoverPill(
                    listOfNotNull(
                        people?.let { "$it people" },
                        days?.let { "$it days" },
                    ).joinToString(" · "),
                    MomentraBrandColors.Indigo500,
                )
            }
        }
        val hero = snap?.photos?.firstOrNull()?.url
        if (!hero.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            RemoteStoryImage(url = hero, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            snap?.narrative?.opening.orEmpty(),
            color = MomentraBrandColors.Indigo100,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(20.dp))
        MetricGrid(
            metrics = snap?.metrics,
            metricKeys = snap?.display?.metricKeys,
            onDark = true,
            currencyCode = storyCurrency(snap),
        )
    }
}

@Composable
private fun CoverPill(text: String, bg: Color) {
    Text(
        text,
        color = MomentraBrandColors.TextOnDark,
        fontSize = 11.sp,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun MomentChapter(snap: MomentStorySnapshotDto?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MomentraBrandColors.StoryCreamBright)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "A MOMENT TO REMEMBER",
            color = MomentraBrandColors.StoryTerracotta,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            snap?.identity?.title ?: "Moment",
            color = MomentraBrandColors.StoryInk,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            snap?.narrative?.opening.orEmpty(),
            color = MomentraBrandColors.StoryInk,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "The Shape of This Moment",
            color = MomentraBrandColors.StoryInk,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
        )
        Text(
            "Facts that hold the whole story.",
            color = MomentraBrandColors.StoryMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(12.dp))
        FactRows(snap)
        Spacer(Modifier.height(20.dp))
        Text(
            "How the Moment Came Together",
            color = MomentraBrandColors.StoryInk,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(10.dp))
        HorizontalTimeline(snap?.timeline.orEmpty().take(6))
        val mosaic = snap?.photos.orEmpty().filter { !it.url.isNullOrBlank() }
        if (mosaic.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "PHOTO STORY",
                color = MomentraBrandColors.StoryTerracotta,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Text(
                "Celebrations, held close.",
                color = MomentraBrandColors.StoryInk,
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.height(10.dp))
            PhotoMosaic(urls = mosaic.mapNotNull { it.url }.take(4))
        }
    }
}

@Composable
private fun TogetherChapter(snap: MomentStorySnapshotDto?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MomentraBrandColors.StoryCreamBright)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(MomentraBrandColors.StoryAccentPurple, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                "HOW IT CAME TOGETHER",
                color = MomentraBrandColors.StoryAccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "How the Moment came together",
            color = MomentraBrandColors.StoryInk,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "From the first invite to the last shared update.",
            color = MomentraBrandColors.StoryMuted,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(16.dp))
        val peopleCount = metricInt(snap?.metrics, "people") ?: snap?.people.orEmpty().size
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MomentraBrandColors.StoryDarkClose, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column {
                Text(
                    "$peopleCount PEOPLE · NO RANKINGS",
                    color = MomentraBrandColors.Indigo100,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "No rankings. Just many ways of showing up.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        VerticalTimeline(snap?.timeline.orEmpty())
        Spacer(Modifier.height(16.dp))
        Text(
            "INVISIBLE PREP",
            color = MomentraBrandColors.StoryAccentPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val plansN = metricInt(snap?.metrics, "plans")
            PrepCard(
                value = plansN?.takeIf { it > 0 }?.toString() ?: "—",
                label = "Plans",
                modifier = Modifier.weight(1f),
            )
            PrepCard(
                value = snap?.metrics?.get("contributed")?.toString()
                    ?: snap?.metrics?.get("raised")?.toString()
                    ?: "—",
                label = "Contributed",
                modifier = Modifier.weight(1f),
            )
            val decisionsN = metricInt(snap?.metrics, "decisions")
            PrepCard(
                value = decisionsN?.takeIf { it > 0 }?.toString() ?: "—",
                label = "Decisions",
                modifier = Modifier.weight(1f),
            )
        }
        val decisions = snap?.decisions.orEmpty()
        if (decisions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Shared decisions", color = MomentraBrandColors.StoryTerracotta, fontWeight = FontWeight.SemiBold)
            decisions.take(4).forEach { d ->
                Spacer(Modifier.height(6.dp))
                Text(
                    d.title ?: "Decision",
                    color = MomentraBrandColors.StoryInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MomentraBrandColors.StoryCream, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun PrepCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MomentraBrandColors.StoryDarkClose, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PlusJakartaSans)
        Text(label, color = MomentraBrandColors.Indigo100, fontSize = 11.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MoneyChapter(snap: MomentStorySnapshotDto?) {
    val money = snap?.money
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MomentraBrandColors.StoryCream)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(MomentraBrandColors.StoryAccentPurple, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                "THE MONEY BEHIND THE MOMENT",
                color = MomentraBrandColors.StoryAccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The Money Behind the Moment",
            color = MomentraBrandColors.StoryInk,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
        )
        Text(
            "How contributions became days together.",
            color = MomentraBrandColors.StoryMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(14.dp))
        val spent = money?.spent ?: 0.0
        val target = money?.target
        val pct = if (target != null && target > 0) ((spent / target) * 1000).roundToInt() / 10.0 else null
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BUDGET AT A GLANCE", color = MomentraBrandColors.StoryAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (pct != null) {
                Text("${pct}% budget used", color = MomentraBrandColors.StoryInk, fontSize = 14.sp, fontFamily = FontFamily.Serif)
            }
        }
        Spacer(Modifier.height(10.dp))
        val currency = storyCurrency(snap)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MoneyFlowTile("Contributed", money?.contributed ?: 0.0, currency, Modifier.weight(1f))
            MoneyFlowTile("Spent", spent, currency, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MoneyFlowTile("Remaining", money?.remaining ?: 0.0, currency, Modifier.weight(1f))
            MoneyFlowTile("Unsettled", money?.unsettled ?: 0.0, currency, Modifier.weight(1f))
        }
        if ((money?.unsettled ?: 0.0) <= 0.0 && spent > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "✓  All recorded balances settled.",
                color = MomentraBrandColors.Teal700,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        val categories = money?.categories.orEmpty().filter { (it.amount ?: 0.0) > 0.0 }
        if (categories.isNotEmpty() && spent > 0) {
            Spacer(Modifier.height(18.dp))
            Text("WHERE THE MONEY WENT", color = MomentraBrandColors.StoryAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            CategoryDonut(categories, spent, currency)
        }
        val expenses = money?.expenses.orEmpty()
        if (expenses.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("EXPENSES BEHIND THE MOMENT", color = MomentraBrandColors.StoryAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            groupStoryExpenses(expenses).forEach { day ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day.label, color = MomentraBrandColors.StoryInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(formatStoryMoney(day.total, currency), color = MomentraBrandColors.StoryMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MomentraBrandColors.StoryDarkClose, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    day.items.forEach { e ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(cleanExpenseTitle(e.description), color = Color.White, fontSize = 12.sp)
                                Text(
                                    listOfNotNull(e.category, e.payer).joinToString(" · "),
                                    color = MomentraBrandColors.Indigo100,
                                    fontSize = 10.sp,
                                )
                            }
                            Text(formatStoryMoney(e.amount ?: 0.0, currency), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        val moneyInsight = snap?.narrative?.insights.orEmpty().firstOrNull {
            it.contains("accounted", ignoreCase = true) || it.contains("spend", ignoreCase = true)
        }
        if (moneyInsight != null) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MomentraBrandColors.StoryPeach, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("HUMAN INSIGHT", color = MomentraBrandColors.StoryTerracotta, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(moneyInsight, color = MomentraBrandColors.StoryMuted, fontSize = 13.sp)
            }
        }
        if (money == null || (spent <= 0 && (money.contributed ?: 0.0) <= 0)) {
            Spacer(Modifier.height(12.dp))
            Text("No expenses recorded.", color = MomentraBrandColors.StoryMuted)
        }
    }
}

private val storySliceColors = listOf(
    Color(0xFF4B3EA8),
    Color(0xFFE8621A),
    Color(0xFF0F7A6A),
    Color(0xFFC4893A),
    Color(0xFF6C4EF2),
    Color(0xFFB45F3D),
    Color(0xFF3D6B9A),
    Color(0xFF8A6A4A),
)

@Composable
private fun CategoryDonut(
    categories: List<MomentStoryMoneyCategoryDto>,
    spent: Double,
    currencyCode: String,
) {
    val amounts = categories.map { it.amount ?: 0.0 }
    val percents = categoryPercents(amounts, spent)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(132.dp)) {
            var start = -90f
            categories.forEachIndexed { index, cat ->
                val sweep = (((cat.amount ?: 0.0) / spent) * 360f).toFloat()
                if (sweep <= 0f) return@forEachIndexed
                drawArc(
                    color = storySliceColors[index % storySliceColors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                )
                start += sweep
            }
            drawCircle(color = MomentraBrandColors.StoryCream, radius = size.minDimension * 0.29f)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            categories.forEachIndexed { index, cat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(storySliceColors[index % storySliceColors.size], CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat.name ?: "Other", color = MomentraBrandColors.StoryInk, fontSize = 13.sp)
                        Text(
                            formatStoryMoney(cat.amount ?: 0.0, currencyCode),
                            color = MomentraBrandColors.StoryMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        "${percents.getOrElse(index) { 0 }}%",
                        color = MomentraBrandColors.StoryInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

private data class StoryExpenseDay(
    val label: String,
    val total: Double,
    val items: List<MomentStoryMoneyExpenseDto>,
)

private fun groupStoryExpenses(expenses: List<MomentStoryMoneyExpenseDto>): List<StoryExpenseDay> {
    val zone = java.time.ZoneId.systemDefault()
    val heading = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM", java.util.Locale.US)
    val buckets = linkedMapOf<java.time.LocalDate, MutableList<MomentStoryMoneyExpenseDto>>()
    val undated = mutableListOf<MomentStoryMoneyExpenseDto>()
    expenses.forEach { expense ->
        val day = expense.at?.let { iso ->
            runCatching { java.time.Instant.parse(iso).atZone(zone).toLocalDate() }.getOrNull()
        }
        if (day == null) undated.add(expense) else buckets.getOrPut(day) { mutableListOf() }.add(expense)
    }
    val days = buckets.keys.sorted().map { day ->
        val items = buckets.getValue(day)
        StoryExpenseDay(day.format(heading), items.sumOf { it.amount ?: 0.0 }, items)
    }
    if (undated.isEmpty()) return days
    return days + StoryExpenseDay("Undated", undated.sumOf { it.amount ?: 0.0 }, undated)
}

private fun categoryPercents(amounts: List<Double>, spent: Double): List<Int> {
    if (spent <= 0.0 || amounts.isEmpty()) return amounts.map { 0 }
    val raw = amounts.map { ((it / spent) * 100).roundToInt() }.toMutableList()
    val drift = 100 - raw.sum()
    if (drift != 0) {
        val largest = amounts.indices.maxBy { amounts[it] }
        raw[largest] = (raw[largest] + drift).coerceAtLeast(0)
    }
    return raw
}

@Composable
private fun MoneyFlowTile(label: String, value: Double, currencyCode: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MomentraBrandColors.StoryCreamBright, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(formatStoryMoney(value, currencyCode), color = MomentraBrandColors.StoryInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = MomentraBrandColors.StoryMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CloseChapter(snap: MomentStorySnapshotDto?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MomentraBrandColors.StoryDarkClose)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.momentra_official_logo),
            contentDescription = "Momentra",
            modifier = Modifier.height(36.dp).width(36.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "TOGETHER · FORWARD",
            color = MomentraBrandColors.Ember500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(16.dp))
        val celebration = celebrationFamily(snap?.identity?.familyProfile)
        Text(
            if (celebration) "The celebration ended.\nThe Moment stayed." else (snap?.display?.closeLine ?: "Life happens in moments."),
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            lineHeight = 34.sp,
        )
        if (celebration) {
            Spacer(Modifier.height(12.dp))
            Text(
                snap?.display?.closeLine ?: "Life happens in moments.",
                color = MomentraBrandColors.Indigo100,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            snap?.identity?.title ?: "",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        val plans = metricInt(snap?.metrics, "plans")
        val decisions = metricInt(snap?.metrics, "decisions")
        Spacer(Modifier.height(20.dp))
        if (plans != null && plans > 0) {
            Text("✓  $plans plans on the checklist", color = MomentraBrandColors.Indigo100, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
        }
        if (decisions != null && decisions > 0) {
            Text("✓  $decisions decisions closed", color = MomentraBrandColors.Indigo100, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
        }
        if ((snap?.money?.unsettled ?: 0.0) <= 0.0 && (snap?.money?.spent ?: 0.0) > 0) {
            Text("✓  All balances settled", color = MomentraBrandColors.Indigo100, fontSize = 14.sp)
        }
        snap?.narrative?.insights.orEmpty().take(2).forEach {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MomentraBrandColors.Indigo100, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PhotoMosaic(urls: List<String>) {
    if (urls.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RemoteStoryImage(
            url = urls[0],
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
        )
        if (urls.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                urls.drop(1).take(2).forEach { url ->
                    RemoteStoryImage(
                        url = url,
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
                if (urls.size == 2) Spacer(Modifier.weight(1f))
            }
        }
        urls.drop(3).take(1).forEach { url ->
            RemoteStoryImage(
                url = url,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)),
            )
        }
    }
}

@Composable
private fun FactRows(snap: MomentStorySnapshotDto?) {
    val metrics = snap?.metrics ?: return
    val defs = snap.display?.metricKeys?.mapNotNull { mk ->
        val key = mk.key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        key to (mk.label ?: key)
    } ?: listOf(
        "people" to "People",
        "days" to "Days",
        "plans" to "Plans",
        "spent" to "Spent",
        "decisions" to "Decisions",
        "photos" to "Photos",
    )
    val countKeys = setOf("people", "days", "hours", "months", "plans", "decisions", "photos", "bills")
    defs.take(6).forEach { (key, label) ->
        val raw = metrics[key] ?: return@forEach
        val n = metricNumeric(raw)
        if (key in countKeys && n != null && n == 0.0) return@forEach
        val value = formatMetricValue(
            raw,
            key,
            countKeys,
            setOf("spent", "raised", "target", "remaining", "contributed"),
            storyCurrency(snap),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "$value $label",
                color = MomentraBrandColors.StoryTerracotta,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.width(130.dp),
            )
            Text(
                factBlurb(key, value, label),
                color = MomentraBrandColors.StoryInk,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun factBlurb(key: String, value: String, label: String): String = when (key) {
    "people" -> "$value people shared this Moment."
    "days", "hours", "months" -> "Together across $value ${label.lowercase()}."
    "plans" -> "Plans moved from checklist to reality."
    "decisions" -> "Decisions were made together."
    "photos" -> "$value photos and memories were captured."
    "spent", "raised", "contributed" -> "The money story landed with the group."
    else -> "$label: $value"
}

@Composable
private fun HorizontalTimeline(items: List<com.example.momentra.data.api.MomentStoryTimelineItemDto>) {
    if (items.isEmpty()) {
        Text("The moment unfolded together.", color = MomentraBrandColors.StoryMuted)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .background(MomentraBrandColors.Amber500, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        formatStoryDateShort(item.at) ?: "",
                        color = MomentraBrandColors.StoryTerracotta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(item.label ?: "Milestone", color = MomentraBrandColors.StoryInk, fontSize = 13.sp)
                    if (!item.detail.isNullOrBlank()) {
                        Text(item.detail, color = MomentraBrandColors.StoryMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalTimeline(items: List<com.example.momentra.data.api.MomentStoryTimelineItemDto>) {
    HorizontalTimeline(items)
}

@Composable
private fun MetricGrid(
    metrics: Map<String, Any?>?,
    metricKeys: List<MomentStoryMetricKeyDto>?,
    onDark: Boolean,
    currencyCode: String,
) {
    if (metrics.isNullOrEmpty()) return
    val defs = if (!metricKeys.isNullOrEmpty()) {
        metricKeys.mapNotNull { mk ->
            val key = mk.key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            key to (mk.label?.takeIf { it.isNotBlank() } ?: key.replaceFirstChar { it.uppercase() })
        }
    } else {
        listOf(
            "people" to "People",
            "days" to "Days",
            "plans" to "Plans",
            "decisions" to "Decisions",
            "photos" to "Photos",
            "spent" to "Spent",
        )
    }
    val countKeys = setOf("people", "days", "hours", "months", "plans", "decisions", "photos", "bills")
    val moneyKeys = setOf("spent", "raised", "target", "remaining", "contributed")
    val shown = defs.mapNotNull { (key, label) ->
        val raw = metrics[key] ?: return@mapNotNull null
        val numeric = metricNumeric(raw)
        if (key in countKeys && numeric != null && numeric == 0.0) return@mapNotNull null
        Triple(key, label, formatMetricValue(raw, key, countKeys, moneyKeys, currencyCode))
    }.take(6)
    if (shown.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (_, label, display) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (onDark) MomentraBrandColors.Indigo500.copy(alpha = 0.55f)
                                else MomentraBrandColors.StoryCream,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Text(
                            display,
                            color = if (onDark) MomentraBrandColors.TextOnDark else MomentraBrandColors.StoryInk,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            label,
                            color = if (onDark) MomentraBrandColors.Indigo100 else MomentraBrandColors.StoryMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RemoteStoryImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MomentraBrandColors.Indigo100.copy(alpha = 0.35f)))
    }
}

private fun metricInt(metrics: Map<String, Any?>?, key: String): Int? {
    val n = metricNumeric(metrics?.get(key)) ?: return null
    return n.roundToInt()
}

private fun metricNumeric(raw: Any?): Double? = when (raw) {
    is Number -> raw.toDouble()
    is String -> raw.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
    else -> null
}

private fun formatMetricValue(
    raw: Any?,
    key: String,
    countKeys: Set<String>,
    moneyKeys: Set<String>,
    currencyCode: String,
): String {
    if (raw is String && key in moneyKeys) return raw
    val n = metricNumeric(raw)
    return when {
        key in countKeys && n != null -> n.toInt().toString()
        key in moneyKeys && n != null -> formatStoryMoney(n, currencyCode)
        raw is String -> raw
        n != null && n == n.toLong().toDouble() -> n.toLong().toString()
        else -> raw.toString()
    }
}

private fun formatStoryMoney(value: Double, currencyCode: String): String {
    val code = currencyCode.takeIf { it.length == 3 } ?: "INR"
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(code)
        format.maximumFractionDigits = 0
        format.format(value)
    } catch (_: Exception) {
        "$code ${value.roundToInt()}"
    }
}

/** Strip trailing " | Category" so titles don't duplicate the category subline. */
private fun cleanExpenseTitle(description: String?): String {
    val raw = description?.trim().orEmpty()
    if (raw.isEmpty()) return "Expense"
    val sep = " | "
    val idx = raw.lastIndexOf(sep)
    if (idx < 0) return raw
    val note = raw.substring(0, idx).trim()
    return note.ifEmpty { "Expense" }
}

private fun dateRangeLabel(start: String?, end: String?): String? {
    val s = formatStoryDate(start)
    val e = formatStoryDate(end)
    return when {
        s != null && e != null && s != e -> "$s · $e"
        s != null -> s
        e != null -> e
        else -> null
    }
}

private fun formatStoryDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = java.time.Instant.parse(iso)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    } catch (_: Exception) {
        iso.take(10)
    }
}

private fun formatStoryDateShort(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = java.time.Instant.parse(iso)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)).uppercase()
    } catch (_: Exception) {
        iso.take(10)
    }
}

private fun shareMomentStoryPdf(context: Context, bytes: ByteArray) {
    val file = File(context.cacheDir, "moment-story.pdf")
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Share Moment Story PDF").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context.findActivity() == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        (context.findActivity() ?: context).startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Sharing unavailable", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMomentStory(context: Context, text: String) {
    val payload = text.trim().ifBlank { "Moment Story" }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
        putExtra(Intent.EXTRA_SUBJECT, "Moment Story")
    }
    val chooser = Intent.createChooser(send, "Share Moment Story").apply {
        if (context.findActivity() == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        (context.findActivity() ?: context).startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Sharing unavailable", Toast.LENGTH_SHORT).show()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
