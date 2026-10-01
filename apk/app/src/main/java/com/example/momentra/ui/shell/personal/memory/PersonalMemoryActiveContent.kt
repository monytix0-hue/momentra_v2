package com.example.momentra.ui.shell.personal.memory

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.PersonalMemoryDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.shell.group.shared.RemoteMemoryImage
import com.example.momentra.ui.shell.personal.shared.PersonalMemorySummaryModel
import com.example.momentra.ui.theme.PlusJakartaSans
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale

private val MemBg = Color(0xFF14121B)
private val MemCard = Color(0xFF1C1B2E)
private val MemCardAlt = Color(0xFF161B26)
private val MemText = Color(0xFFE5E0EE)
private val MemMuted = Color(0xFFC9C4D8)
private val MemDim = Color(0xFF8C8C9E)
private val MemPurple = Color(0xFF7C5CFC)
private val MemGreen = Color(0xFF10B981)
private val MemRed = Color(0xFFEF4444)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** M4 Memory — reflection over time (five honest blocks via PersonalMemorySummaryModel). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalMemoryActiveContent(
    refreshToken: Long,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<PersonalMemoryDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPatternWhy by remember { mutableStateOf(false) }
    var showEvolutionDetail by remember { mutableStateOf(false) }
    var showRelive by remember { mutableStateOf(false) }

    LaunchedEffect(refreshToken) {
        if (payload != null) loading = false else loading = true
        error = null
        repository.getMemory().fold(
            onSuccess = { payload = it; loading = false },
            onFailure = { error = it.message; loading = false },
        )
    }

    if (loading && payload == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MemPurple)
        }
        return
    }

    val data = payload
    if (data == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                error ?: "Memory projection unavailable",
                color = MemMuted,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        return
    }

    val summary = remember(data) { PersonalMemorySummaryModel.from(data) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MemBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            Text(it, color = MemRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        MemoryHeroBlock(summary.hero)
        if (summary.reliveMedia.isNotEmpty()) {
            MemoryReliveBlock(
                media = summary.reliveMedia,
                onOpenRelive = { showRelive = true },
            )
        }
        if (summary.highlights.items.isNotEmpty()) {
            MemoryHighlightsBlock(summary.highlights)
        }
        summary.pattern?.let { pattern ->
            MemoryPatternBlock(
                pattern = pattern,
                hasWhy = summary.patternWhy != null,
                onSeeWhy = { showPatternWhy = true },
            )
        }
        if (summary.returnBehaviours.isNotEmpty()) {
            MemoryReturnBlock(summary.returnBehaviours)
        }
        summary.evolution?.let { evo ->
            MemoryEvolutionBlock(
                evolution = evo,
                hasDetail = summary.evolutionDetail != null,
                onSeeEvolution = { showEvolutionDetail = true },
            )
        }
        if (summary.highlights.items.isEmpty() &&
            summary.pattern == null &&
            summary.returnBehaviours.isEmpty() &&
            summary.evolution == null &&
            summary.hero.memoryCount == 0 &&
            summary.hero.activityCount == 0
        ) {
            MemoryEmptyHint()
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPatternWhy) {
        val why = summary.patternWhy.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { showPatternWhy = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MemCard,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "See why",
                    color = MemPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                why.forEach { item ->
                    Text(
                        item.label,
                        color = MemText,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MemCardAlt)
                            .padding(12.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showEvolutionDetail) {
        val detail = summary.evolutionDetail
        if (detail != null) {
            ModalBottomSheet(
                onDismissRequest = { showEvolutionDetail = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MemCard,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Your evolution",
                        color = MemPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text("Then", color = MemDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(detail.thenSummary, color = MemText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    Text("Now", color = MemDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(detail.nowSummary, color = MemText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    detail.notes.forEach { note ->
                        Text(note, color = MemMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showRelive && summary.reliveMedia.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showRelive = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MemCard,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Relive",
                    color = MemPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                summary.reliveMedia.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { item ->
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RemoteMemoryImage(
                                    url = item.downloadUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                item.title?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = MemMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans, maxLines = 2)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MemoryReliveBlock(
    media: List<PersonalMemorySummaryModel.ReliveMediaItem>,
    onOpenRelive: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Relive",
            color = MemPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            media.take(6).forEach { item ->
                RemoteMemoryImage(
                    url = item.downloadUrl,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            "Open Relive",
            color = MemPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.clickable(onClick = onOpenRelive),
        )
    }
}

@Composable
private fun MemoryHeroBlock(hero: PersonalMemorySummaryModel.Hero) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            hero.periodLabel.uppercase(),
            color = MemPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        hero.sentence?.let {
            Text(
                it,
                color = MemText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CountChip("${hero.memoryCount}", "memories")
            CountChip("${hero.activityCount}", "activities")
            if (hero.highlightCount > 0) {
                CountChip("${hero.highlightCount}", "highlights")
            }
        }
    }
}

@Composable
private fun CountChip(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = MemText, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(label, color = MemDim, fontSize = 10.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MemoryHighlightsBlock(highlights: PersonalMemorySummaryModel.Highlights) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            highlights.heading,
            color = MemPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        highlights.items.forEach { item ->
            Text(
                item.title,
                color = MemText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MemCardAlt)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MemoryPatternBlock(
    pattern: PersonalMemorySummaryModel.Pattern,
    hasWhy: Boolean,
    onSeeWhy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Patterns worth knowing",
            color = MemPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(pattern.title, color = MemText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(pattern.body, color = MemMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        if (hasWhy) {
            Text(
                "See why",
                color = MemPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onSeeWhy),
            )
        }
    }
}

@Composable
private fun MemoryReturnBlock(items: List<PersonalMemorySummaryModel.ReturnBehaviour>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Worth doing again",
            color = MemPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MemCardAlt)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.label, color = MemText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
                item.strengthLabel?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MemGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

@Composable
private fun MemoryEvolutionBlock(
    evolution: PersonalMemorySummaryModel.Evolution,
    hasDetail: Boolean,
    onSeeEvolution: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Then → Now",
            color = MemPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(evolution.thenLabel, color = MemDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("→", color = MemDim, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            Text(evolution.nowLabel, color = MemText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
        Text(evolution.summary, color = MemMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
        if (hasDetail) {
            Text(
                "See your evolution",
                color = MemPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onSeeEvolution),
            )
        }
    }
}

@Composable
private fun MemoryEmptyHint() {
    Text(
        "As you log life and save memories, this space will help you revisit what mattered.",
        color = MemMuted,
        fontSize = 13.sp,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MemCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
    )
}
