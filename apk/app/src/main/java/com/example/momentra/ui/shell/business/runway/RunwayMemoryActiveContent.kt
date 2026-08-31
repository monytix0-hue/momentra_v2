package com.example.momentra.ui.shell.business.runway

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
import com.example.momentra.ui.shell.business.runway.components.RunwayColors
import com.example.momentra.ui.shell.business.runway.components.RunwayDiamondDivider
import com.example.momentra.ui.shell.business.runway.components.RunwayEmptyAiCard
import com.example.momentra.ui.shell.business.runway.components.RunwayFilterChipRow
import com.example.momentra.ui.shell.business.runway.components.RunwayGradientPrimaryButton
import com.example.momentra.ui.shell.business.runway.components.RunwayMemoryHeroSection
import com.example.momentra.ui.shell.business.runway.components.RunwayMemoryListSection
import com.example.momentra.ui.shell.business.runway.components.RunwayOutlineButton
import com.example.momentra.ui.theme.PlusJakartaSans

private val Scopes = listOf("All", "Revenue", "Expenses", "Tax", "Investors")

/** Figma `698:9970` — multi-section stack; live memory; honest AI shells. */
@Composable
fun RunwayMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onRecordLearning: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessRunway
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
        repository.getMemory(momentId).fold(
            onSuccess = { payload = it.payload },
            onFailure = { error = it.message },
        )
        loading = false
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
                    q == "revenue" -> hay.contains("revenue") || hay.contains("mrr") || hay.contains("invoice")
                    q == "expenses" -> hay.contains("expense") || hay.contains("burn") || hay.contains("cost")
                    q == "tax" -> hay.contains("tax") || hay.contains("gst") || hay.contains("filing")
                    q == "investors" -> hay.contains("investor") || hay.contains("term sheet") ||
                        hay.contains("funding")
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
            Text(it, color = RunwayColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        RunwayFilterChipRow(
            chips = Scopes,
            selected = scope,
            onSelect = { scope = it },
            theme = theme,
        )

        RunwayMemoryHeroSection(
            ringLabel = ringLabel,
            learnings = if (memoryCount > 0) "$memoryCount" else "—",
            patterns = patterns,
            accuracy = accuracy,
            showLive = items.isNotEmpty(),
            theme = theme,
        )

        RunwayEmptyAiCard(
            title = "Biggest Learning",
            emptyCopy = biggestLearning
                ?: "Biggest learning appears when memory AI projects a signal — record learnings to seed it.",
            theme = theme,
        )

        RunwayDiamondDivider(theme = theme)

        RunwayEmptyAiCard(
            title = "Pattern Network",
            emptyCopy = "Pattern network unavailable — memory.pattern API not mounted.",
            theme = theme,
        )

        RunwayDiamondDivider(theme = theme)

        RunwayEmptyAiCard(
            title = "Financial Playbook",
            emptyCopy = "Playbook rules deferred until AI rule projection exists.",
            theme = theme,
        )

        RunwayDiamondDivider(theme = theme)

        RunwayMemoryListSection(
            title = "Success Memory",
            emptyCopy = "No success memories yet.",
            items = successItems,
            theme = theme,
            accentBorder = RunwayColors.Emerald,
        )

        RunwayMemoryListSection(
            title = "Risk Memory",
            emptyCopy = "No risk memories yet.",
            items = riskItems,
            theme = theme,
            accentBorder = RunwayColors.Red,
        )

        RunwayEmptyAiCard(
            title = "momentra intelligence",
            emptyCopy = "\"Runway health improves when monthly closes and burn reviews share the same cadence.\" — moments intelligence",
            theme = theme,
        )

        RunwayEmptyAiCard(
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
            RunwayGradientPrimaryButton(
                label = "Record a Learning",
                enabled = !momentId.isNullOrBlank(),
                onClick = onRecordLearning,
                modifier = Modifier.weight(1f),
            )
            RunwayOutlineButton(
                label = "Share with Team",
                enabled = false,
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
        hay.contains("fail") || hay.contains("block") || hay.contains("delay") ||
        hay.contains("dip") || hay.contains("overdue")
}
