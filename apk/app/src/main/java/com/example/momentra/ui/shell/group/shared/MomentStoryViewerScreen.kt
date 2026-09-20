package com.example.momentra.ui.shell.group.shared

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.MomentStoryDto
import com.example.momentra.data.api.MomentStorySharePackDto
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/**
 * Interactive Moment Story viewer (Phase 1) — swipeable chapters with brand chrome + share pack.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentStoryViewerScreen(
    momentId: String,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var story by remember { mutableStateOf<MomentStoryDto?>(null) }
    var sharePack by remember { mutableStateOf<MomentStorySharePackDto?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val status = ApiClient.apiService.getMomentStoryStatus(momentId).data
                if (status.status == "GENERATING") {
                    error = "Generating your Moment Story…"
                    loading = false
                    return@launch
                }
                if (status.status == "FAILED") {
                    error = status.errorMessage ?: "Story generation failed."
                    loading = false
                    return@launch
                }
                if (status.status == "NOT_STARTED") {
                    error = "No Story yet — complete the moment to unlock it."
                    loading = false
                    return@launch
                }
                story = ApiClient.apiService.getMomentStory(momentId).data
                runCatching {
                    sharePack = ApiClient.apiService.getMomentStorySharePack(momentId).data
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not load Story"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(momentId) { reload() }

    val chapters = story?.chapters ?: story?.snapshot?.chapters ?: listOf("cover", "alive", "money", "close")
    val pagerState = rememberPagerState(pageCount = { chapters.size.coerceAtLeast(1) })
    val snap = story?.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MomentraBrandColors.Indigo700),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "momentra",
                    color = MomentraBrandColors.TextOnDark,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    fontSize = 18.sp,
                )
                Text(
                    snap?.display?.displayLabel ?: "Moment Story",
                    color = MomentraBrandColors.Indigo100,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            TextButton(onClick = onClose) {
                Text("Close", color = MomentraBrandColors.Ember500)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MomentraBrandColors.Ember500)
            }
            error != null && story == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MomentraBrandColors.TextOnDark, modifier = Modifier.padding(24.dp))
                    Button(
                        onClick = { reload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Ember500),
                    ) { Text("Retry") }
                }
            }
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { page ->
                    val chapter = chapters.getOrNull(page) ?: "cover"
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
                            val pack = sharePack
                            val text = buildString {
                                append(pack?.blurb ?: snap?.identity?.title ?: "Moment Story")
                                append("\n")
                                append(pack?.webUrl ?: pack?.webPath ?: pack?.appDeepLink ?: "")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Moment Story"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MomentraBrandColors.Ember500),
                    ) { Text("Share") }
                    Text(
                        "${pagerState.currentPage + 1} / ${chapters.size}",
                        color = MomentraBrandColors.Indigo100,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryChapterPage(chapter: String, story: MomentStoryDto?) {
    val snap = story?.snapshot
    val title = snap?.identity?.title ?: "Moment"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(MomentraBrandColors.Indigo500.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            snap?.display?.coverEyebrow?.uppercase() ?: chapter.uppercase(),
            color = MomentraBrandColors.Ember500,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(8.dp))
        when (chapter) {
            "cover" -> {
                Text(title, color = MomentraBrandColors.TextOnDark, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(12.dp))
                Text(snap?.narrative?.opening.orEmpty(), color = MomentraBrandColors.Indigo100, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(20.dp))
                MetricGrid(snap?.metrics)
            }
            "alive" -> {
                Text("How it came alive", color = MomentraBrandColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(12.dp))
                Text("Plans, people, and decisions that shaped this moment.", color = MomentraBrandColors.Indigo100, fontFamily = PlusJakartaSans)
            }
            "money" -> {
                Text("Money & fairness", color = MomentraBrandColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(12.dp))
                val money = snap?.money
                Text(
                    "Spent ₹${money?.spent?.toInt() ?: 0} · Remaining ₹${money?.remaining?.toInt() ?: 0}",
                    color = MomentraBrandColors.Indigo100,
                    fontFamily = PlusJakartaSans,
                )
            }
            "memories" -> {
                Text("Memories that stayed", color = MomentraBrandColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                snap?.narrative?.insights.orEmpty().forEach {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MomentraBrandColors.Indigo100, fontFamily = PlusJakartaSans)
                }
            }
            else -> {
                Text("TOGETHER · FORWARD", color = MomentraBrandColors.Ember500, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(16.dp))
                Text(snap?.display?.closeLine ?: "Life happens in moments.", color = MomentraBrandColors.TextOnDark, fontSize = 22.sp, fontFamily = PlusJakartaSans)
                Spacer(Modifier.height(24.dp))
                Text(title, color = MomentraBrandColors.Indigo100.copy(alpha = 0.5f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricGrid(metrics: Map<String, Any?>?) {
    if (metrics.isNullOrEmpty()) return
    val keys = listOf("people", "days", "hours", "months", "plans", "decisions", "photos", "spent", "raised", "target")
    val shown = keys.mapNotNull { k -> metrics[k]?.let { k to it } }.take(6)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (k, v) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MomentraBrandColors.Indigo500.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Text(v.toString(), color = MomentraBrandColors.TextOnDark, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(k.replaceFirstChar { it.uppercase() }, color = MomentraBrandColors.Indigo100, fontSize = 11.sp)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
