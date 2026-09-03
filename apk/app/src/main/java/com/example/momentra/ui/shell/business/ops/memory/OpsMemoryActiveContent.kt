package com.example.momentra.ui.shell.business.ops.memory

import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.ops.components.OpsBiggestLearningCard
import com.example.momentra.ui.shell.business.ops.components.OpsColors
import com.example.momentra.ui.shell.business.ops.components.OpsDiamondDivider
import com.example.momentra.ui.shell.business.ops.components.OpsFilterChipRow
import com.example.momentra.ui.shell.business.ops.components.OpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.ops.components.OpsKnowledgeJourneySection
import com.example.momentra.ui.shell.business.ops.components.OpsMemoryHeroSection
import com.example.momentra.ui.shell.business.ops.components.OpsMemoryListSection
import com.example.momentra.ui.shell.business.ops.components.OpsOutlineButton
import com.example.momentra.ui.shell.business.ops.components.OpsPatternNetworkSection
import com.example.momentra.ui.shell.business.ops.components.OpsPlaybookSection
import com.example.momentra.ui.shell.business.ops.components.OpsScopeDropdown
import com.example.momentra.ui.shell.business.ops.components.OpsWisdomQuoteSection
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val Scopes = listOf("All", "Budget", "Vendors", "Approvals", "Issues")

/** Figma `696:9450` Business Operations Memory — multi-section stack; honest empties. */
@Composable
fun OpsMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onRecordMemory: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessOperations
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessMemoryPayloadDto?>(null) }
    var scopeFilter by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf<String?>(null) }
    var shareBusy by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }

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
    val filtered = remember(items, scopeFilter) {
        if (scopeFilter == "All") items
        else {
            val q = scopeFilter.lowercase()
            items.filter { item ->
                val title = item["title"]?.toString().orEmpty().lowercase()
                val body = item["body"]?.toString().orEmpty().lowercase()
                val hay = "$title $body"
                when {
                    q.startsWith("budget") -> hay.contains("budget") || hay.contains("spend") || hay.contains("cost")
                    q.startsWith("vendor") -> hay.contains("vendor") || hay.contains("supplier") || hay.contains("sla")
                    q.startsWith("approval") -> hay.contains("approval") || hay.contains("sign-off") || hay.contains("approve")
                    q.startsWith("issue") -> hay.contains("issue") || hay.contains("incident") || hay.contains("risk")
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
    val patternCount = if (items.size >= 3) "${items.size / 3}" else "—"
    val accuracy = if (items.isEmpty()) "—" else "${((successItems.size.toFloat() / items.size.coerceAtLeast(1)) * 100).toInt()}%"

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
            Text(it, color = OpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        shareMessage?.let {
            Text(it, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        OpsScopeDropdown(label = "Operations", theme = theme)

        OpsFilterChipRow(
            chips = Scopes,
            selected = scopeFilter,
            onSelect = { scopeFilter = it },
            theme = theme,
        )

        OpsMemoryHeroSection(
            ringLabel = if (items.isEmpty()) "—" else "Live",
            learnings = "$memoryCount",
            patterns = patternCount,
            accuracy = accuracy,
            showLive = items.isNotEmpty(),
            theme = theme,
        )

        OpsBiggestLearningCard(quote = biggestLearning, theme = theme)

        OpsDiamondDivider(theme = theme)

        OpsPatternNetworkSection(theme = theme)

        OpsDiamondDivider(theme = theme)

        OpsPlaybookSection(theme = theme)

        OpsDiamondDivider(theme = theme)

        OpsMemoryListSection(
            title = "Success Memory",
            emptyCopy = "No success memories yet.",
            items = successItems,
            theme = theme,
            accentBorder = OpsColors.Green,
        )

        OpsMemoryListSection(
            title = "Risk Memory",
            emptyCopy = "No risk memories yet.",
            items = riskItems,
            theme = theme,
            accentBorder = OpsColors.Red,
        )

        OpsWisdomQuoteSection(theme = theme)

        OpsKnowledgeJourneySection(items = filtered, theme = theme)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OpsGradientPrimaryButton(
                label = "Record a Learning",
                enabled = !momentId.isNullOrBlank(),
                onClick = onRecordMemory,
                modifier = Modifier.weight(1f),
            )
            OpsOutlineButton(
                label = if (shareBusy) "Sharing…" else "Share with Team",
                enabled = !momentId.isNullOrBlank() && !shareBusy,
                onClick = {
                    val id = momentId ?: return@OpsOutlineButton
                    shareBusy = true
                    shareMessage = null
                    coroutineScope.launch {
                        repository.createShareLink(id).fold(
                            onSuccess = { link ->
                                shareBusy = false
                                val url = link.shareUrl.orEmpty()
                                if (url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share with team"))
                                }
                                shareMessage = link.note ?: "Share link created"
                            },
                            onFailure = {
                                shareBusy = false
                                shareMessage = it.message ?: "Share unavailable"
                            },
                        )
                    }
                },
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
