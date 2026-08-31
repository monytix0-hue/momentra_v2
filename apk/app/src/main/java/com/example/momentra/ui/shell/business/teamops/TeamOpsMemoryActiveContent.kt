package com.example.momentra.ui.shell.business.teamops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.BusinessActiveTheme
import com.example.momentra.ui.shell.business.BusinessTabDataCache
import com.example.momentra.ui.shell.business.loadBusinessMemoryTab
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsColors
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsDiamondDivider
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsEmptyAiCard
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsFilterChipRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsMemoryHeroSection
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsMemoryListSection
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsOutlineButton
import com.example.momentra.ui.theme.PlusJakartaSans

private val Scopes = listOf("All", "Team", "Runway", "Ops")

/** Figma `692:35410` — multi-section stack; live memory lists; AI shells honest empty. */
@Composable
fun TeamOpsMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onRecordLearning: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.TeamOperations
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessMemoryPayloadDto?>(null) }
    var scope by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = payload == null
        error = null
        BusinessTabDataCache.peekMemory(momentId)?.memory?.let { cached ->
            payload = cached
            loading = false
        }
        loadBusinessMemoryTab(repository, momentId).fold(
            onSuccess = { data -> payload = data.memory; loading = false },
            onFailure = { e -> error = e.message; loading = false },
        )
    }

    if (loading && payload == null) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val items = payload?.items.orEmpty()
    val filtered = remember(items, scope) {
        if (scope == "All") items
        else {
            val q = scope.lowercase()
            items.filter { item ->
                val title = item["title"]?.toString().orEmpty().lowercase()
                val body = item["body"]?.toString().orEmpty().lowercase()
                val hay = "$title $body"
                when {
                    q == "team" -> hay.contains("team") || hay.contains("owner") || hay.contains("delivery")
                    q == "runway" -> hay.contains("runway") || hay.contains("budget") || hay.contains("hire")
                    q == "ops" -> hay.contains("ops") || hay.contains("vendor") || hay.contains("sla") ||
                        hay.contains("operation")
                    else -> true
                }
            }
        }
    }
    val memoryCount = payload?.memoryCount ?: items.size
    val successItems = filtered.filter { !isRiskItem(it) }
    val riskItems = filtered.filter { isRiskItem(it) }
    val biggestLearning = filtered.firstOrNull()?.let { item ->
        item["body"]?.toString()?.takeIf { it.isNotBlank() }
            ?: item["title"]?.toString()?.takeIf { it.isNotBlank() }
    }
    // Patterns / accuracy AI APIs missing — honest empties
    val patterns = "—"
    val accuracy = "—"
    val ringLabel = if (items.isEmpty()) "—" else "Live"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            Text(it, color = TeamOpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        TeamOpsFilterChipRow(
            chips = Scopes,
            selected = scope,
            onSelect = { scope = it },
            theme = theme,
        )

        TeamOpsMemoryHeroSection(
            ringLabel = ringLabel,
            learnings = if (memoryCount > 0) "$memoryCount" else "—",
            patterns = patterns,
            accuracy = accuracy,
            showLive = items.isNotEmpty(),
            theme = theme,
        )

        TeamOpsEmptyAiCard(
            title = "Biggest Learning",
            emptyCopy = biggestLearning
                ?: "Biggest learning appears when memory AI projects a signal — record learnings to seed it.",
            theme = theme,
        )

        TeamOpsDiamondDivider(theme = theme)

        TeamOpsEmptyAiCard(
            title = "Pattern Network",
            emptyCopy = "Pattern network unavailable — memory.pattern API not mounted.",
            theme = theme,
        )

        TeamOpsDiamondDivider(theme = theme)

        TeamOpsEmptyAiCard(
            title = "Business Playbook",
            emptyCopy = "Playbook rules deferred until AI rule projection exists.",
            theme = theme,
        )

        TeamOpsDiamondDivider(theme = theme)

        TeamOpsMemoryListSection(
            title = "Success Memory",
            emptyCopy = "No success memories yet.",
            items = successItems,
            theme = theme,
            accentBorder = TeamOpsColors.Emerald,
        )

        TeamOpsMemoryListSection(
            title = "Risk Memory",
            emptyCopy = "No risk memories yet.",
            items = riskItems,
            theme = theme,
            accentBorder = TeamOpsColors.Red,
        )

        TeamOpsEmptyAiCard(
            title = "Team Wisdom",
            emptyCopy = "\"Momentum stays strongest when financial and execution decisions share the same operating cadence.\" — moments intelligence",
            theme = theme,
        )

        TeamOpsEmptyAiCard(
            title = "Knowledge Journey",
            emptyCopy = if (filtered.isEmpty()) {
                "Journey milestones appear as memories are recorded."
            } else {
                filtered.take(5).joinToString(" → ") {
                    it["title"]?.toString()?.ifBlank { "Memory" } ?: "Memory"
                }
            },
            theme = theme,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TeamOpsGradientPrimaryButton(
                label = "Record a Learning",
                enabled = !momentId.isNullOrBlank(),
                onClick = onRecordLearning,
                modifier = Modifier.weight(1f),
            )
            TeamOpsOutlineButton(
                label = "Share with Team",
                enabled = true,
                onClick = onOpenQuickAdd,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun isRiskItem(item: Map<String, Any?>): Boolean {
    val hay = "${item["title"]} ${item["body"]}".lowercase()
    return hay.contains("risk") || hay.contains("issue") || hay.contains("incident") ||
        hay.contains("fail") || hay.contains("block")
}
