package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * One Pulse body for Money, Daily Business, and Team.
 * Hero and snapshot both render [BusinessPulsePresentation.displayedFacts].
 */
@Composable
fun BusinessPulseScreen(
    family: BusinessMomentFamilyConfig.Family,
    momentId: String?,
    momentTitle: String?,
    momentTypeCode: String?,
    refreshToken: Long,
    capabilities: List<String> = emptyList(),
    onToday: (BusinessQuickAddKind) -> Unit = {},
    onOpenMoments: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.forTypeCode(
        momentTypeCode ?: when (family) {
            BusinessMomentFamilyConfig.Family.MONEY -> "BUSINESS_RUNWAY"
            BusinessMomentFamilyConfig.Family.DAILY -> "BUSINESS_OPERATIONS"
            BusinessMomentFamilyConfig.Family.TEAM -> "TEAM_OPERATIONS"
        },
    )
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf<BusinessTabDataCache.PulseTab?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var smallShop by remember { mutableStateOf(false) }

    LaunchedEffect(momentId) {
        smallShop = BusinessAudience.isSmallShopMoment(momentId, context = context)
    }

    LaunchedEffect(refreshToken, momentId, family) {
        if (momentId.isNullOrBlank()) {
            loading = false
            tab = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.let { cached ->
            tab = cached
            loading = false
        } ?: run { loading = tab == null }
        loadBusinessPulseTab(repository, momentId, family = family).fold(
            onSuccess = {
                tab = it
                loading = false
            },
            onFailure = { e ->
                if (e !is kotlinx.coroutines.CancellationException) {
                    error = e.message
                }
                loading = false
            },
        )
    }

    val presentation = tab?.let { cached ->
        val cash = cached.life?.runwayPayload?.get("availableCash")?.toString()
        buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = family,
                smallShop = smallShop,
                capabilities = capabilities,
                momentTypeCode = momentTypeCode,
                finance = cached.finance ?: cached.pulse?.finance,
                runwayMonths = cached.pulse?.runwayMonths,
                availableCash = cash,
                lifeFailed = cached.lifeFailed,
                operations = cached.pulse?.operations,
                rosterCount = cached.rosterCount,
                approvals = cached.approvals,
                issues = cached.issues,
                activities = cached.activities,
            ),
        )
    }

    if (loading && presentation == null) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            presentation?.familyTitle ?: theme.typeLabel,
            color = theme.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!error.isNullOrBlank() && presentation == null) {
            Text(error!!, color = theme.secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        presentation?.let { model ->
            Text("Now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            FactBlock(theme, model)
            TodayBlock(theme, model.today, smallShop, onToday)
            AttentionBlock(theme, model)
            RecentBlock(theme, model, onOpenMoments)
            Text("Snapshot", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            FactBlock(theme, model)
        }
    }
}

@Composable
private fun FactBlock(theme: BusinessActiveTheme, model: BusinessPulsePresentation) {
    val facts = model.displayedFacts()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (model.showsCompanyTotals()) {
            Text(
                BusinessPulsePresentation.COMPANY_TOTALS,
                color = theme.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        if (facts.isEmpty()) {
            Text(
                "No figures yet.",
                color = theme.secondary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                facts.forEach { fact ->
                    FactTile(theme, fact, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FactTile(theme: BusinessActiveTheme, fact: PulseFact, modifier: Modifier) {
    val value = when (val metric = fact.metric) {
        is PulseMetric.Available -> metric.value
        PulseMetric.Zero -> "0"
        PulseMetric.Unavailable -> return
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(fact.label, color = theme.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(
            value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TodayBlock(
    theme: BusinessActiveTheme,
    kinds: List<BusinessQuickAddKind>,
    smallShop: Boolean,
    onToday: (BusinessQuickAddKind) -> Unit,
) {
    if (kinds.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Today", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            kinds.forEach { kind ->
                Text(
                    kind.label(smallShop),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.accent.copy(alpha = 0.18f))
                        .clickable { onToday(kind) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AttentionBlock(theme: BusinessActiveTheme, model: BusinessPulsePresentation) {
    if (model.attention.isEmpty() && model.attentionEmptyLine == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Needs attention",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        if (model.attention.isEmpty()) {
            Text(
                model.attentionEmptyLine.orEmpty(),
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            model.attention.forEach { item ->
                Text(
                    item.title,
                    color = theme.text,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.card)
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentBlock(
    theme: BusinessActiveTheme,
    model: BusinessPulsePresentation,
    onOpenMoments: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                BusinessPulsePresentation.RECENT_TITLE,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "See all",
                color = theme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onOpenMoments),
            )
        }
        if (model.recent.isEmpty()) {
            Text("Nothing recorded yet.", color = theme.secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        } else {
            model.recent.forEach { card ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.card)
                        .padding(12.dp),
                ) {
                    Text(card.title, color = theme.text, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    val whenLabel = formatBusinessPulseOccurredAt(card.occurredAt).ifBlank { null }
                    val detail = listOfNotNull(card.amountLabel, whenLabel).joinToString(" · ")
                    if (detail.isNotBlank()) {
                        Text(detail, color = theme.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }
    }
}
