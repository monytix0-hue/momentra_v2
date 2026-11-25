package com.example.momentra.ui.shell.business.shared

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.api.TradingStoryCardDto
import com.example.momentra.data.api.TradingStoryPageDto
import com.example.momentra.data.api.TradingStoryPreviewBody
import com.example.momentra.data.api.TradingStoryPreviewDto
import com.example.momentra.ui.shell.group.shared.StoryStateBody
import com.example.momentra.ui.shell.group.shared.StoryStateNight
import com.example.momentra.ui.shell.group.shared.StoryStateSpecs
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val StoryInk = Color(0xFFF8F9FD)
private val StoryMuted = Color(0xFFF8F9FD).copy(alpha = 0.70f)
private val StoryGlass = Color.White.copy(alpha = 0.08f)

private enum class StoryPhase {
    Config, Generating, Viewer, Saved, Failed, Unauthorized, Conflict, Loading,
}

/**
 * Trading / Distribution Story v3 — Figma TRD-STORY-CONFIG / GENERATING / P1–P8 / SAVED / SHARE / states.
 */
@Composable
fun TradingStoryV3Flow(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    noun: String,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val accent = theme.accent
    val name = model.title?.takeIf { it.isNotBlank() } ?: noun.replaceFirstChar { it.uppercase() }
    val hasActivity = model.activity.isNotEmpty()

    var phase by remember { mutableStateOf(StoryPhase.Config) }
    var detailed by remember { mutableStateOf(false) }
    var includeGst by remember { mutableStateOf(false) }
    var gstAvailable by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf("THIS_MONTH") }
    var generateProgress by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<TradingStoryPreviewDto?>(null) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var savedStoryId by remember { mutableStateOf<String?>(null) }
    var savedVersion by remember { mutableIntStateOf(1) }
    var savedAt by remember { mutableStateOf<String?>(null) }
    var cancelGenerate by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    val periodLabel = storyPeriodDisplay(period, model.startDate)

    LaunchedEffect(momentId) {
        runCatching {
            ApiClient.apiService.getTradingStoryOptions(momentId).data
        }.onSuccess { options ->
            if (options == null) return@onSuccess
            gstAvailable = options.gstAvailable
            if (options.permissions?.canGenerate == false) {
                phase = StoryPhase.Unauthorized
            }
        }
    }

    fun shareText(): String {
        val hero = preview?.pages?.firstOrNull()?.hero
        val amount = hero?.value?.let { compactInr(it) } ?: "Business Story"
        return "$name · $periodLabel · 8 slides\n$amount — ${hero?.label ?: "Story"}\nRecorded activity only."
    }

    fun openSystemShare() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText())
        }
        context.startActivity(Intent.createChooser(intent, "Share story"))
    }

    suspend fun generate() {
        cancelGenerate = false
        error = null
        errorCode = null
        phase = StoryPhase.Generating
        generateProgress = 1
        val range = storyPeriodRange(period, model.startDate, model.endDate)
        runCatching {
            val start = ApiClient.apiService.startTradingStoryPreview(
                momentId = momentId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = TradingStoryPreviewBody(
                    startDate = range.first,
                    endDate = range.second,
                    detail = if (detailed) "DETAILED" else "SUMMARY",
                    includeGst = includeGst && gstAvailable,
                ),
            ).data
            val jobId = start.jobId
            if (!jobId.isNullOrBlank()) {
                for (step in 1..8) {
                    if (cancelGenerate) return
                    generateProgress = step
                    delay(120)
                    val job = runCatching { ApiClient.apiService.getTradingStoryJob(jobId).data }.getOrNull()
                    when (job?.status?.uppercase()) {
                        "FAILED" -> error("Generation failed")
                        "CONFLICT" -> {
                            phase = StoryPhase.Conflict
                            return
                        }
                    }
                    job?.progress?.let { generateProgress = (it / 12 + 1).coerceIn(1, 8) }
                }
            } else {
                delay(400)
                generateProgress = 8
            }
            val previewId = start.previewId ?: error("No preview id")
            if (cancelGenerate) return
            val loaded = ApiClient.apiService.getTradingStoryPreview(previewId).data
            if (loaded.status.equals("CONFLICT", true)) {
                phase = StoryPhase.Conflict
                return
            }
            preview = loaded
            pageIndex = 0
            phase = StoryPhase.Viewer
        }.onFailure { e ->
            error = e.message ?: "Could not generate story"
            errorCode = "GENERATION_FAILED"
            val msg = error.orEmpty().lowercase()
            phase = if (msg.contains("401") || msg.contains("403") || msg.contains("unauthor")) {
                StoryPhase.Unauthorized
            } else {
                StoryPhase.Failed
            }
        }
    }

    suspend fun save(thenShare: Boolean) {
        val previewId = preview?.previewId ?: return
        runCatching {
            ApiClient.apiService.saveTradingStoryPreview(
                previewId = previewId,
                idempotencyKey = UUID.randomUUID().toString(),
            ).data
        }.onSuccess { result ->
            savedStoryId = result?.storyId
            savedVersion = result?.version ?: 1
            savedAt = result?.savedAt ?: result?.snapshotWatermark
            phase = StoryPhase.Saved
            if (thenShare) {
                showShare = true
                openSystemShare()
            }
        }.onFailure {
            error = it.message
            errorCode = "SAVE_FAILED"
            phase = StoryPhase.Failed
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(StoryStateNight),
    ) {
        when (phase) {
            StoryPhase.Config -> ConfigPhase(
                name = name,
                accent = accent,
                period = period,
                onPeriod = { period = it },
                detailed = detailed,
                onDetailed = { detailed = it },
                gstAvailable = gstAvailable,
                includeGst = includeGst,
                onIncludeGst = { includeGst = it },
                hasActivity = hasActivity,
                periodLabel = periodLabel,
                hero = formatOperatingAmount(model.currency, model.finance?.sales, model.financeVisible),
                error = error,
                onGenerate = { scope.launch { generate() } },
                onDismiss = onDismiss,
            )
            StoryPhase.Generating -> GeneratingPhase(
                name = name,
                accent = accent,
                progress = generateProgress,
                onCancel = {
                    cancelGenerate = true
                    phase = StoryPhase.Config
                },
            )
            StoryPhase.Viewer -> ViewerPhase(
                preview = preview,
                name = name,
                periodLabel = periodLabel,
                accent = accent,
                pageIndex = pageIndex,
                onPageIndex = { pageIndex = it },
                saved = savedStoryId != null,
                onSave = { scope.launch { save(thenShare = false) } },
                onShare = { scope.launch { save(thenShare = true) } },
                onDismiss = onDismiss,
            )
            StoryPhase.Saved -> SavedPhase(
                name = name,
                periodLabel = periodLabel,
                accent = accent,
                hero = formatOperatingAmount(model.currency, model.finance?.sales, model.financeVisible),
                version = savedVersion,
                savedAt = savedAt,
                onShare = {
                    showShare = true
                    openSystemShare()
                },
                onBack = onDismiss,
            )
            StoryPhase.Failed -> StoryStateBody(
                spec = StoryStateSpecs.generatingFailed(error),
                accent = accent,
                onPrimary = { scope.launch { generate() } },
                onSecondary = { phase = StoryPhase.Config },
            )
            StoryPhase.Unauthorized -> StoryStateBody(
                spec = StoryStateSpecs.unauthorized(),
                accent = accent,
                onPrimary = onDismiss,
            )
            StoryPhase.Conflict -> StoryStateBody(
                spec = StoryStateSpecs.conflict(),
                accent = accent,
                onPrimary = { scope.launch { generate() } },
                onSecondary = { phase = StoryPhase.Config },
            )
            StoryPhase.Loading -> StoryStateBody(
                spec = StoryStateSpecs.loading(),
                accent = accent,
            )
        }

        if (showShare && phase == StoryPhase.Saved) {
            ShareSheetOverlay(
                name = name,
                periodLabel = periodLabel,
                accent = accent,
                onShare = { openSystemShare() },
                onDone = { showShare = false },
            )
        }
    }
}

@Composable
private fun ConfigPhase(
    name: String,
    accent: Color,
    period: String,
    onPeriod: (String) -> Unit,
    detailed: Boolean,
    onDetailed: (Boolean) -> Unit,
    gstAvailable: Boolean,
    includeGst: Boolean,
    onIncludeGst: (Boolean) -> Unit,
    hasActivity: Boolean,
    periodLabel: String,
    hero: String,
    error: String?,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CloseChip(onDismiss)
        }
        StoryThumb(hero = hero, month = periodLabel, accent = accent)
        Text(
            "Your $periodLabel story is ready to compose",
            color = StoryMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(StoryGlass)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Create Business Story", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("$name · 8 slides", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
            }
            PeriodChips(period, onPeriod, accent)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(4.dp),
            ) {
                Seg("Summary", !detailed, accent, Modifier.weight(1f)) { onDetailed(false) }
                Seg("Detailed", detailed, accent, Modifier.weight(1f)) { onDetailed(true) }
            }
            if (gstAvailable) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include GST snapshot", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Shown because GST is enabled · recorded only", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 11.sp)
                    }
                    Switch(
                        checked = includeGst,
                        onCheckedChange = onIncludeGst,
                        colors = SwitchDefaults.colors(checkedTrackColor = accent),
                    )
                }
            }
            if (!hasActivity) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠ No verified activity in this period", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "We found no reconciled trades for $periodLabel. We won’t show zeros for data we don’t have.",
                        color = StoryMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    listOf("Trades", "Revenue", "Payouts").forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row, color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                            Text("Not recorded", color = Color(0xFFF5A623), fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (hasActivity) accent else Color.White.copy(alpha = 0.08f))
                    .clickable(enabled = hasActivity, onClick = onGenerate)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (hasActivity) "Generate story" else "Generate · needs activity",
                    color = if (hasActivity) Color(0xFF090D16) else StoryMuted,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (error != null) {
                Text(error, color = Color(0xFFFF6B6B), fontFamily = PlusJakartaSans, fontSize = 11.sp)
            }
            Text(
                "Every page is built from recorded activity (M01–M18). Nothing is estimated.",
                color = StoryMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun GeneratingPhase(name: String, accent: Color, progress: Int, onCancel: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).scale(scale).border(2.dp, accent.copy(alpha = 0.25f), CircleShape))
            Box(Modifier.size(180.dp).border(2.dp, accent.copy(alpha = 0.35f), CircleShape))
            Box(
                Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1).uppercase(),
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Composing your story", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("Replaying stock & orders…", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        ProgressSegments(current = progress, total = 8, accent = accent, modifier = Modifier.width(260.dp))
        Text("$progress of 8 slides", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Spacer(Modifier.height(48.dp))
        Text("Only recorded activity is used — nothing estimated.", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text(
            "Cancel",
            color = StoryMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp).clickable(onClick = onCancel),
        )
    }
}

@Composable
private fun ViewerPhase(
    preview: TradingStoryPreviewDto?,
    name: String,
    periodLabel: String,
    accent: Color,
    pageIndex: Int,
    onPageIndex: (Int) -> Unit,
    saved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pages = preview?.pages.orEmpty()
    val page = pages.getOrNull(pageIndex)
    val total = pages.size.coerceAtLeast(1)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ProgressSegments(current = pageIndex + 1, total = total, accent = accent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(preview?.businessName ?: name, color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "$periodLabel · ${page?.title ?: "Story"} · ${pageIndex + 1}/$total",
                        color = StoryMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                }
                CloseChip(onDismiss)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                PageBody(page = page, accent = accent)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text((page?.title ?: "STORY").uppercase(), color = StoryMuted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text("Recorded activity only", color = StoryMuted.copy(alpha = 0.8f), fontFamily = PlusJakartaSans, fontSize = 11.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (saved) "Saved" else "Save story",
                    color = Color(0xFF090D16),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent)
                        .clickable(enabled = !saved, onClick = onSave)
                        .padding(vertical = 14.dp),
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StoryGlass)
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↗", color = StoryInk, fontSize = 18.sp)
                }
            }
        }
        Row(Modifier.fillMaxSize().padding(top = 120.dp, bottom = 120.dp)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .pointerInput(pageIndex) {
                        detectTapGestures {
                            if (pageIndex > 0) onPageIndex(pageIndex - 1)
                        }
                    },
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(0.7f)
                    .pointerInput(pageIndex, pages.size) {
                        detectTapGestures {
                            if (pageIndex < pages.lastIndex) onPageIndex(pageIndex + 1)
                        }
                    },
            )
        }
    }
}

@Composable
private fun PageBody(page: TradingStoryPageDto?, accent: Color) {
    val pageId = page?.pageId.orEmpty().uppercase()
    Text(
        (page?.title ?: "Story").uppercase(),
        color = accent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(10.dp))
    when (pageId) {
        "P2" -> {
            Text(page?.title.orEmpty(), color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            page?.narrative.orEmpty().forEach {
                Text(it, color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StoryGlass)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                page?.cards.orEmpty().forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            card.label.orEmpty(),
                            color = StoryInk,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(card.value.orEmpty(), color = StoryInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                    }
                }
            }
        }
        "P5" -> {
            HeroBlock(page)
            page?.narrative.orEmpty().forEach {
                Text(it, color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            }
            val cards = page?.cards.orEmpty()
            val mid = (cards.size / 2).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCol("INBOUND", cards.take(mid), Modifier.weight(1f))
                GlassCol("OUTBOUND", cards.drop(mid), Modifier.weight(1f))
            }
        }
        else -> {
            Text(page?.title.orEmpty(), color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            HeroBlock(page)
            page?.narrative.orEmpty().forEach {
                Text(it, color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                page?.cards.orEmpty().take(4).forEach { card ->
                    Text(
                        "${card.label.orEmpty()} · ${card.value.orEmpty()}",
                        color = StoryInk,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(StoryGlass)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
            if (page?.state == "empty" || page?.state == "partial") {
                Text(
                    if (page.state == "empty") "Nothing recorded yet" else "Partial · some facts missing",
                    color = Color(0xFFF5A623),
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroBlock(page: TradingStoryPageDto?) {
    val value = page?.hero?.value
    if (value != null) {
        Text(
            if (page?.hero?.unit.orEmpty().contains("count", true)) value.toInt().toString() else compactInr(value),
            color = StoryInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Text("Not recorded", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp)
    }
    page?.hero?.label?.takeIf { it.isNotBlank() }?.let {
        Text(it, color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 15.sp)
    }
}

@Composable
private fun GlassCol(title: String, cards: List<TradingStoryCardDto>, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StoryGlass)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = StoryMuted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        cards.forEach { card ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.label.orEmpty(), color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                Text(card.value ?: "—", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SavedPhase(
    name: String,
    periodLabel: String,
    accent: Color,
    hero: String,
    version: Int,
    savedAt: String?,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        StoryThumb(hero = hero, month = periodLabel, accent = accent)
        Text("✓", color = accent, fontSize = 40.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Story saved to Memory", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            "Version $version · ${savedAt ?: "just now"} · immutable snapshot",
            color = StoryMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "New or backdated entries create Version 2 with a changed-facts badge.",
            color = StoryMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Share story",
            color = Color(0xFF090D16),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent)
                .clickable(onClick = onShare)
                .padding(vertical = 16.dp),
        )
        Text(
            "Back to Memory",
            color = StoryInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StoryGlass)
                .clickable(onClick = onBack)
                .padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun ShareSheetOverlay(
    name: String,
    periodLabel: String,
    accent: Color,
    onShare: () -> Unit,
    onDone: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDone),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF12151F))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(100.dp)).background(Color.White.copy(alpha = 0.2f)))
            }
            Text("Share your Business Moment", color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("$name · $periodLabel · 8 slides", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            val dests = listOf("Your story", "WhatsApp", "Instagram", "Copy link", "Export PDF", "Save image", "Email", "More")
            dests.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .clickable(onClick = onShare),
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StoryGlass),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("•", color = accent, fontSize = 18.sp)
                            }
                            Text(label, color = StoryInk, fontFamily = PlusJakartaSans, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
            Text(
                "Done",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable(onClick = onDone)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
fun TradingStoryMemoryEmptyState(
    businessLabel: String,
    accent: Color,
    onGenerate: () -> Unit,
) {
    StoryStateBody(
        spec = StoryStateSpecs.memoryEmpty(businessLabel),
        accent = accent,
        onPrimary = onGenerate,
    )
}

@Composable
private fun ProgressSegments(current: Int, total: Int, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(if (index == current - 1) 4.dp else 3.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (index < current) accent else Color.White.copy(alpha = 0.22f)),
            )
        }
    }
}

@Composable
private fun StoryThumb(hero: String, month: String, accent: Color) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .size(148.dp, 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), StoryStateNight)))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ProgressSegments(current = 1, total = 8, accent = accent)
            Column {
                Text("THE BUSINESS MOMENT", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 8.sp)
                Text(hero, color = StoryInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("net sales · $month", color = StoryMuted, fontFamily = PlusJakartaSans, fontSize = 10.sp)
            }
            Box(Modifier.size(56.dp, 6.dp).clip(RoundedCornerShape(100.dp)).background(Color.White.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun PeriodChips(period: String, onPeriod: (String) -> Unit, accent: Color) {
    val chips = listOf(
        "This week" to "THIS_WEEK",
        "This month" to "THIS_MONTH",
        "Last month" to "LAST_MONTH",
        "Quarter" to "QUARTER",
        "Year" to "YEAR",
        "Custom" to "CUSTOM",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, code) ->
                    val on = period == code
                    Text(
                        label,
                        color = if (on) Color(0xFF090D16) else StoryInk,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) accent else StoryGlass)
                            .clickable { onPeriod(code) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Seg(label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color(0xFF090D16) else StoryMuted,
        fontFamily = PlusJakartaSans,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun CloseChip(onDismiss: () -> Unit) {
    Text(
        "✕",
        color = StoryInk,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(StoryGlass)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun compactInr(value: Double): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 100_000 -> "₹${(value / 100_000).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it) }}L"
        abs >= 1_000 -> "₹${(value / 1_000).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it) }}K"
        else -> "₹${value.toInt()}"
    }
}

private fun storyPeriodDisplay(preset: String, start: String?): String {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return when (preset) {
        "THIS_WEEK" -> "This week"
        "LAST_MONTH" -> LocalDate.now().minusMonths(1).format(fmt)
        "QUARTER" -> "This quarter"
        "YEAR" -> "This year"
        "CUSTOM" -> "Custom period"
        else -> {
            val date = start?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
            date.format(fmt)
        }
    }
}

private fun storyPeriodRange(preset: String, start: String?, end: String?): Pair<String, String> {
    val today = LocalDate.now()
    return when (preset) {
        "THIS_WEEK" -> today.minusDays(6).toString() to today.toString()
        "LAST_MONTH" -> {
            val first = today.withDayOfMonth(1).minusMonths(1)
            first.toString() to first.withDayOfMonth(first.lengthOfMonth()).toString()
        }
        "QUARTER" -> {
            val qStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
            LocalDate.of(today.year, qStartMonth, 1).toString() to today.toString()
        }
        "YEAR" -> LocalDate.of(today.year, 1, 1).toString() to today.toString()
        "CUSTOM" -> {
            val from = start?.take(10).orEmpty()
            val to = end?.take(10).orEmpty()
            if (from.isNotBlank() && to.isNotBlank()) from to to
            else today.withDayOfMonth(1).toString() to today.toString()
        }
        else -> today.withDayOfMonth(1).toString() to today.toString()
    }
}
