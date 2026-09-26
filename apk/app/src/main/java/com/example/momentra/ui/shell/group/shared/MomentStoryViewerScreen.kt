package com.example.momentra.ui.shell.group.shared

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.MomentStoryDto
import com.example.momentra.data.api.MomentStoryMetricKeyDto
import com.example.momentra.data.api.MomentStoryPhotoDto
import com.example.momentra.data.api.MomentStoryMoneyCategoryDto
import com.example.momentra.data.api.MomentStoryMoneyExpenseDto
import com.example.momentra.data.api.MomentStorySnapshotDto
import com.example.momentra.ui.shell.components.momentraMaxWidth
import com.example.momentra.ui.shell.components.rememberMomentraWindowSize
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Moment Story viewer — dark booklet matching the branded memory PDF.
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
    val window = rememberMomentraWindowSize()
    val chromeDark = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StoryNight),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .momentraMaxWidth(window.contentMaxWidth)
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (chromeDark) {
                                Modifier
                            } else {
                                Modifier
                                    .background(MomentraBrandColors.StoryDarkClose)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.momentra_official_logo),
                        contentDescription = "Momentra",
                        modifier = Modifier.height(40.dp).width(108.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    snap?.display?.displayLabel ?: "Moment Story",
                    color = if (chromeDark) MomentraBrandColors.Indigo100 else MomentraBrandColors.StoryMuted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
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
                StoryShareBar(
                    narrow = window.isNarrowWidth,
                    sharing = sharing,
                    pageLabel = "${pagerState.currentPage + 1} / ${chapters.size}",
                    pageColor = if (chromeDark) MomentraBrandColors.Indigo100 else MomentraBrandColors.StoryMuted,
                    onShare = {
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
                    onSharePdf = {
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
                )
            }
        }
    }
    }
}

@Composable
private fun StoryShareBar(
    narrow: Boolean,
    sharing: Boolean,
    pageLabel: String,
    pageColor: Color,
    onShare: () -> Unit,
    onSharePdf: () -> Unit,
) {
    val shareButton: @Composable (Modifier) -> Unit = { modifier ->
        Button(
            onClick = onShare,
            modifier = modifier,
            enabled = !sharing,
            colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Ember500),
        ) { Text("Share") }
    }
    val pdfButton: @Composable (Modifier) -> Unit = { modifier ->
        Button(
            onClick = onSharePdf,
            modifier = modifier,
            enabled = !sharing,
            colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Indigo700),
        ) { Text("Share PDF") }
    }
    if (narrow) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shareButton(Modifier.fillMaxWidth())
            pdfButton(Modifier.fillMaxWidth())
            Text(pageLabel, color = pageColor, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shareButton(Modifier.weight(1f))
            pdfButton(Modifier.weight(1f))
            Text(pageLabel, color = pageColor)
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
    val pages = mutableListOf("cover", "moment")
    if (!moneyChapterEmpty(story)) pages += listOf("money", "expenses")
    pages += "close"
    return pages
}

private fun storyCurrency(snap: MomentStorySnapshotDto?): String =
    snap?.identity?.currencyCode?.takeIf { it.length == 3 } ?: "INR"

private fun storyPhotosOldest(photos: List<MomentStoryPhotoDto>?): List<MomentStoryPhotoDto> =
    photos.orEmpty()
        .filter { !it.url.isNullOrBlank() }
        .sortedWith(compareBy(nullsLast()) { it.at })

private fun storySummaryLine(snap: MomentStorySnapshotDto?, includePayments: Boolean): String {
    val people = snap?.people.orEmpty().size
    val memories = snap?.photos.orEmpty().count { !it.url.isNullOrBlank() }
    val currency = storyCurrency(snap)
    val parts = mutableListOf(
        if (people == 1) "1 person" else "$people people",
        if (memories == 1) "1 memory" else "$memories memories",
        formatStoryMoney(snap?.money?.spent ?: 0.0, currency),
    )
    if (includePayments) {
        val payments = snap?.money?.expenses.orEmpty().size
        parts += if (payments == 1) "1 payment" else "$payments payments"
    }
    return parts.joinToString(" · ")
}

private fun expenseOutsideNote(snap: MomentStorySnapshotDto?): String? {
    val dated = snap?.money?.expenses.orEmpty().mapNotNull { it.at }.filter { it.isNotBlank() }
    if (dated.isEmpty()) return null
    val start = snap?.identity?.startAt
    val end = snap?.identity?.endAt ?: snap?.identity?.completedAt
    if (start.isNullOrBlank() && end.isNullOrBlank()) return null
    val earliest = dated.minBy { storyDayKey(it) }
    val latest = dated.maxBy { storyDayKey(it) }
    val before = !start.isNullOrBlank() && storyDayKey(earliest) < storyDayKey(start)
    val after = !end.isNullOrBlank() && storyDayKey(latest) > storyDayKey(end)
    if (!before && !after) return null
    val span = activitySpanLabel(earliest, latest) ?: return null
    val activated = formatStoryDayMonth(start)
    val completed = formatStoryDayMonth(end)
    return when {
        activated != null && completed != null ->
            "Recorded expense activity spans $span, while the Moment itself was activated on $activated and completed on $completed."
        activated != null ->
            "Recorded expense activity spans $span, while the Moment itself was activated on $activated."
        completed != null ->
            "Recorded expense activity spans $span, while the Moment itself was completed on $completed."
        else -> null
    }
}

private fun celebrationFamily(family: String?): Boolean =
    family == "HOUSE_PARTY" || family == "WEDDING" || family == "SHARED_EXPERIENCE"

private val StoryNight = Color(0xFF120F20)
private val StoryGold = Color(0xFFF5A623)
private val StoryLight = Color(0xFFF4EFFF)
private val StoryMutedViolet = Color(0xFF6B5EA0)
private val StoryLavender = Color(0xFFC3B5FD)
private val StoryCard = Color(0xFF1A1430)
private val StoryGreen = Color(0xFF0EC97F)

@Composable
private fun StoryChapterPage(chapter: String, story: MomentStoryDto?) {
    val snap = story?.snapshot
    when (chapter) {
        "cover" -> CoverChapter(snap)
        "moment", "memories" -> MomentChapter(snap)
        "expenses", "together", "alive" -> ExpenseRecordChapter(snap)
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
            .background(StoryNight)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.7f))
        Image(
            painter = painterResource(R.drawable.momentra_official_logo),
            contentDescription = "Momentra",
            modifier = Modifier.height(88.dp).width(240.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "M E M O R Y",
            color = StoryGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.weight(0.55f))
        Text(
            title.uppercase(),
            color = StoryLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
        val opening = snap?.narrative?.opening?.trim().orEmpty()
        if (opening.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                opening,
                color = StoryLight,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        val range = dateRangeLabel(snap?.identity?.startAt, snap?.identity?.endAt)
        if (!range.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                range,
                color = StoryGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(0.35f))
        Text(
            storySummaryLine(snap, includePayments = false),
            color = StoryLavender,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
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
    val currency = storyCurrency(snap)
    val beats = buildList {
        if (!snap?.identity?.startAt.isNullOrBlank()) {
            add((formatStoryDateShort(snap?.identity?.startAt) ?: "") to "Moment began")
        }
        if (snap?.money?.target != null) {
            add(formatStoryMoney(snap.money.target, currency) to "Budget set")
        }
        val ended = snap?.identity?.completedAt ?: snap?.identity?.endAt
        if (!ended.isNullOrBlank()) {
            add((formatStoryDateShort(ended) ?: "") to "Moment completed")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StoryNight)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "01  /  THE MOMENT",
            color = StoryGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "What stayed",
            color = StoryLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        val opening = snap?.narrative?.opening?.trim().orEmpty()
        if (opening.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(opening, color = StoryMutedViolet, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        val mosaic = storyPhotosOldest(snap?.photos)
        if (mosaic.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            StoryPhotoCollage(mosaic)
        }
        if (beats.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                beats.forEach { (stamp, label) ->
                    Column(Modifier.weight(1f)) {
                        Text(stamp, color = StoryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(label, color = StoryLight, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ExpenseRecordChapter(snap: MomentStorySnapshotDto?) {
    val money = snap?.money
    val currency = storyCurrency(snap)
    val expenses = money?.expenses.orEmpty().sortedWith(compareBy(nullsLast()) { it.at })
    val count = expenses.size
    val mid = (count + 1) / 2
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StoryNight)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "03  /  EXPENSE RECORD",
            color = StoryGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (count == 1) "All 1 payment" else "All $count payments",
            color = StoryLight,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "The complete record, side by side.",
            color = StoryMutedViolet,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(14.dp))
        if (expenses.isEmpty()) {
            Text("No payments recorded.", color = StoryMutedViolet, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    expenses.take(mid).forEachIndexed { index, expense ->
                        ExpenseLine(index + 1, expense, currency)
                    }
                }
                Column(Modifier.weight(1f)) {
                    expenses.drop(mid).forEachIndexed { index, expense ->
                        ExpenseLine(mid + index + 1, expense, currency)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MomentraBrandColors.Indigo700)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TOTAL RECORDED", color = StoryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                formatStoryMoney(money?.spent ?: 0.0, currency),
                color = StoryLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "MEMORY  ·  FINANCIAL DETAILS",
            color = StoryMutedViolet,
            fontSize = 10.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ExpenseLine(
    number: Int,
    expense: MomentStoryMoneyExpenseDto,
    currency: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(number), color = StoryMutedViolet, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            Spacer(Modifier.width(4.dp))
            Text(
                formatStoryDateShort(expense.at) ?: "UNDATED",
                color = StoryGold,
                fontSize = 10.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                cleanExpenseTitle(expense.description),
                color = StoryLight,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatStoryMoney(expense.amount ?: 0.0, currency),
                color = StoryLight,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            (expense.category ?: "").uppercase(),
            color = StoryMutedViolet,
            fontSize = 8.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun StoryBeat(stamp: String, label: String) {
    if (stamp.isBlank()) return
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stamp, color = MomentraBrandColors.StoryInk, fontSize = 16.sp, fontFamily = FontFamily.Serif)
        Text(label, color = MomentraBrandColors.StoryMuted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun StoryPhotoCollage(photos: List<MomentStoryPhotoDto>) {
    photos.chunked(6).forEach { page ->
        val slots = page
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.getOrNull(0)?.let { photo ->
                    CollageSlot(photo, Modifier.weight(1.25f).fillMaxHeight())
                }
                Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    slots.getOrNull(1)?.let { photo ->
                        CollageSlot(photo, Modifier.fillMaxWidth().weight(1f))
                    }
                    slots.getOrNull(2)?.let { photo ->
                        CollageSlot(photo, Modifier.fillMaxWidth().weight(1.15f))
                    }
                }
            }
            if (slots.size > 3) {
                Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    slots.drop(3).take(3).forEach { photo ->
                        CollageSlot(photo, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageSlot(photo: MomentStoryPhotoDto, modifier: Modifier) {
    val url = photo.url ?: return
    RemoteStoryImage(
        url = url,
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        upright = true,
        cover = true,
    )
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
            .background(StoryNight)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "02  /  WHAT IT TOOK",
            color = StoryGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "The financial story",
            color = StoryLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(12.dp))
        val spent = money?.spent ?: 0.0
        val target = money?.target
        val currency = storyCurrency(snap)
        val payments = money?.expenses.orEmpty().size
        Text(
            formatStoryMoney(spent, currency),
            color = StoryGold,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            if (payments == 1) "spent across 1 payment" else "spent across $payments payments",
            color = StoryMutedViolet,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(14.dp))
        val over = target != null && spent > target
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MoneyFlowTile("BUDGET", if (target == null) "—" else formatStoryMoney(target, currency), Modifier.weight(1f), MomentraBrandColors.Indigo700)
            MoneyFlowTile(
                if (over) "OVER BUDGET" else "REMAINING",
                formatStoryMoney(if (over) spent - (target ?: 0.0) else (money?.remaining ?: 0.0), currency),
                Modifier.weight(1f),
                if (over) MomentraBrandColors.Ember500 else MomentraBrandColors.Indigo700,
            )
            MoneyFlowTile("UNSETTLED", formatStoryMoney(money?.unsettled ?: 0.0, currency), Modifier.weight(1f), StoryGreen)
        }
        val categories = money?.categories.orEmpty().filter { (it.amount ?: 0.0) > 0.0 }
        if (categories.isNotEmpty() && spent > 0) {
            Spacer(Modifier.height(18.dp))
            CategoryDonut(categories, spent, currency)
        }
        val largest = money?.expenses.orEmpty().maxByOrNull { it.amount ?: 0.0 }
        if (largest != null && (largest.amount ?: 0.0) > 0.0) {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().background(StoryCard)) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(MomentraBrandColors.Ember500))
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        "ONE PAYMENT STOOD ABOVE THE REST",
                        color = StoryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                cleanExpenseTitle(largest.description),
                                color = StoryLight,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                listOfNotNull(titleCaseStory(largest.category), formatStoryDateCasual(largest.at) ?: "Undated").joinToString("  ·  "),
                                color = StoryMutedViolet,
                                fontSize = 12.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        Text(
                            formatStoryMoney(largest.amount ?: 0.0, currency),
                            color = StoryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
        val outside = expenseOutsideNote(snap)
        if (outside != null) {
            Spacer(Modifier.height(12.dp))
            Text(outside, color = StoryMutedViolet, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private val storySliceColors = listOf(
    Color(0xFF6C4EF2),
    Color(0xFFE8621A),
    Color(0xFFF5A623),
    Color(0xFF0EC97F),
    Color(0xFF4B3EA8),
    Color(0xFF3D6B9A),
    Color(0xFFB45F3D),
    Color(0xFF8A6A4A),
)

@Composable
private fun CategoryDonut(
    categories: List<MomentStoryMoneyCategoryDto>,
    spent: Double,
    currencyCode: String,
) {
    val amounts = categories.map { it.amount ?: 0.0 }
    val sliceTotal = amounts.sum().takeIf { it > 0.0 } ?: spent
    val percents = categoryPercents(amounts, sliceTotal)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                var start = -90f
                categories.forEachIndexed { index, cat ->
                    val sweep = (((cat.amount ?: 0.0) / sliceTotal) * 360f).toFloat()
                    if (sweep <= 0f) return@forEachIndexed
                    drawArc(
                        color = storySliceColors[index % storySliceColors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = true,
                    )
                    start += sweep
                }
                drawCircle(color = StoryNight, radius = size.minDimension * 0.29f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatStoryCompact(sliceTotal, currencyCode),
                    color = StoryLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
                Text("TOTAL", color = StoryMutedViolet, fontSize = 8.sp, fontFamily = PlusJakartaSans)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            categories.forEachIndexed { index, cat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(storySliceColors[index % storySliceColors.size], CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        cat.name ?: "Other",
                        color = StoryLight,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatStoryMoney(cat.amount ?: 0.0, currencyCode),
                        color = StoryMutedViolet,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${percents.getOrElse(index) { 0 }}%",
                        color = StoryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

private data class StoryExpenseDay(
    val dayNumber: String?,
    val month: String?,
    val weekday: String?,
    val total: Double,
    val items: List<MomentStoryMoneyExpenseDto>,
)

private fun groupStoryExpenses(expenses: List<MomentStoryMoneyExpenseDto>): List<StoryExpenseDay> {
    val zone = java.time.ZoneId.systemDefault()
    val monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.US)
    val weekdayFmt = java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale.US)
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
        StoryExpenseDay(
            day.dayOfMonth.toString(),
            day.format(monthFmt),
            day.format(weekdayFmt),
            items.sumOf { it.amount ?: 0.0 },
            items,
        )
    }
    if (undated.isEmpty()) return days
    return days + StoryExpenseDay(null, null, null, undated.sumOf { it.amount ?: 0.0 }, undated)
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
private fun MoneyFlowTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    stripe: Color,
) {
    Column(modifier.background(StoryCard)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(stripe))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(label, color = StoryMutedViolet, fontSize = 9.sp, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, color = StoryLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CloseChapter(snap: MomentStorySnapshotDto?) {
    val started = formatStoryStampYear(snap?.identity?.startAt)
    val ended = formatStoryStampYear(snap?.identity?.completedAt ?: snap?.identity?.endAt)
    val celebration = celebrationFamily(snap?.identity?.familyProfile)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StoryNight)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (started != null) {
                Text(started, color = StoryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text("This Moment began.", color = StoryLight, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(28.dp))
            }
            if (ended != null) {
                Text(ended, color = StoryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text("It ended.", color = StoryLight, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (celebration) {
                Text("The celebration ended.", color = StoryLight, fontSize = 16.sp, fontFamily = PlusJakartaSans, textAlign = TextAlign.Center)
                Text("The Moment stayed.", color = StoryLight, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, textAlign = TextAlign.Center)
            } else {
                Text(
                    snap?.display?.closeLine ?: "Life happens in moments.",
                    color = StoryLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (snap?.identity?.title ?: "").uppercase(),
                color = StoryGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                storySummaryLine(snap, includePayments = true),
                color = StoryMutedViolet,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.momentra_official_logo),
                contentDescription = "Momentra",
                modifier = Modifier.height(44.dp).width(140.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun PhotoMosaic(photos: List<MomentStoryPhotoDto>) {
    val visible = photos.filter { !it.url.isNullOrBlank() }
    if (visible.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CollagePhoto(visible[0], Modifier.fillMaxWidth().height(200.dp), 16.dp)
        val rest = visible.drop(1)
        var index = 0
        var wide = true
        while (index < rest.size) {
            if (wide || index == rest.lastIndex) {
                CollagePhoto(rest[index], Modifier.fillMaxWidth().height(150.dp), 14.dp)
                index += 1
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CollagePhoto(rest[index], Modifier.weight(1f).height(120.dp), 14.dp)
                    CollagePhoto(rest[index + 1], Modifier.weight(1f).height(120.dp), 14.dp)
                }
                index += 2
            }
            wide = !wide
        }
    }
}

@Composable
private fun CollagePhoto(photo: MomentStoryPhotoDto, imageModifier: Modifier, corner: Dp) {
    val url = photo.url ?: return
    Column {
        RemoteStoryImage(
            url = url,
            modifier = imageModifier.clip(RoundedCornerShape(corner)),
        )
        photo.title?.takeIf { it.isNotBlank() }?.let { label ->
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = MomentraBrandColors.StoryInk,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
private fun RemoteStoryImage(
    url: String,
    modifier: Modifier = Modifier,
    upright: Boolean = false,
    cover: Boolean = false,
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = URL(url).openStream().use { it.readBytes() }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                if (!upright) decoded else applyExifOrientation(decoded, bytes)
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        val frame = if (upright && !cover && bmp.height > 0) {
            modifier.aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
        } else {
            modifier
        }
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = if (cover || !upright) ContentScale.Crop else ContentScale.Fit,
            modifier = frame,
        )
    } else {
        Box(modifier.background(MomentraBrandColors.Indigo700))
    }
}

private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
    val rounded = value.roundToInt()
    val sign = if (rounded < 0) "-" else ""
    val body = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(kotlin.math.abs(rounded))
    if (code == "INR") return "${sign}Rs $body"
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(code)
        format.maximumFractionDigits = 0
        format.format(value)
    } catch (_: Exception) {
        "$sign$code $body"
    }
}

private fun formatStoryCompact(value: Double, currencyCode: String): String {
    val code = currencyCode.takeIf { it.length == 3 } ?: "INR"
    val rounded = value.roundToInt()
    val abs = kotlin.math.abs(rounded)
    val compact = when {
        abs >= 100_000 -> String.format(Locale.US, "%.1fL", rounded / 100_000.0)
        abs >= 1_000 -> String.format(Locale.US, "%.1fk", rounded / 1_000.0)
        else -> rounded.toString()
    }
    val sign = if (compact.startsWith("-")) "-" else ""
    val body = compact.removePrefix("-")
    return if (code == "INR") "${sign}Rs $body" else "$sign$code $body"
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

private val storyZone: java.time.ZoneId = java.time.ZoneId.of("Asia/Kolkata")

private fun storyLocalDate(iso: String?): java.time.LocalDate? {
    if (iso.isNullOrBlank()) return null
    return runCatching { java.time.Instant.parse(iso).atZone(storyZone).toLocalDate() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(storyZone).toLocalDate() }.getOrNull()
        ?: runCatching { java.time.LocalDate.parse(iso.take(10)) }.getOrNull()
}

private fun storyDayKey(iso: String): String = storyLocalDate(iso)?.toString().orEmpty()

private fun formatStoryDayMonth(iso: String?): String? {
    val date = storyLocalDate(iso) ?: return null
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))
}

private fun activitySpanLabel(earliest: String, latest: String): String? {
    val a = storyLocalDate(earliest) ?: return null
    val b = storyLocalDate(latest) ?: return null
    val month = java.time.format.DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    return when {
        a.year == b.year && a.month == b.month -> "${a.dayOfMonth}–${b.dayOfMonth} ${a.format(month)}"
        a.year == b.year -> "${a.dayOfMonth} ${a.format(month)} – ${b.dayOfMonth} ${b.format(month)}"
        else -> "${a.dayOfMonth} ${a.format(month)} ${a.year} – ${b.dayOfMonth} ${b.format(month)} ${b.year}"
    }
}

private fun dateRangeLabel(start: String?, end: String?): String? {
    val a = storyLocalDate(start)
    val b = storyLocalDate(end)
    if (a == null && b == null) return null
    val full = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    if (a != null && b != null && a.year == b.year && a.month == b.month) {
        return "${a.dayOfMonth}–${b.dayOfMonth} ${a.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))}"
    }
    return listOfNotNull(a?.format(full), b?.format(full)).joinToString(" – ")
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
    val date = storyLocalDate(iso) ?: return null
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)).uppercase()
}

private fun formatStoryDateCasual(iso: String?): String? {
    val date = storyLocalDate(iso) ?: return null
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}

private fun formatStoryStampYear(iso: String?): String? {
    val date = storyLocalDate(iso) ?: return null
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)).uppercase()
}

private fun titleCaseStory(value: String?): String {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return ""
    return raw.lowercase().replace(Regex("(^|[\\s/&-])(\\w)")) { match ->
        match.groupValues[1] + match.groupValues[2].uppercase()
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
