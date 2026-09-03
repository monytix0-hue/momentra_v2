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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessFinanceTotalDto
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.business.shared.loadBusinessPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans

private val Red = Color(0xFFF87171)

/** Themed Business Pulse — live getPulse / getFinance only (B01–B03). */
@Composable
fun BusinessPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onAddExpense: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.forTypeCode(momentTypeCode)
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<BusinessPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<BusinessFinancePayloadDto?>(null) }
    var businessFamily by remember { mutableStateOf<String?>(null) }
    var facetStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            businessFamily = null
            facetStatus = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.let { cached ->
            pulse = cached.pulse
            finance = cached.finance
            businessFamily = cached.businessFamily
            facetStatus = cached.facetStatus
            loading = false
        } ?: run { loading = true }
        loadBusinessPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                pulse = data.pulse
                finance = data.finance
                businessFamily = data.businessFamily
                facetStatus = data.facetStatus
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val quality = (pulse?.dataQuality ?: finance?.dataQuality ?: "EMPTY").uppercase()
    val isEmpty = quality == "EMPTY" || facetStatus.equals("EMPTY", ignoreCase = true)
    val totals = finance?.totals.orEmpty()
    val primaryCta = when (theme.typeLabel) {
        "Business Runway" -> "Log Expense"
        "Business Operations" -> "Log Spend"
        else -> "Log Team Update"
    }
    val runwayLabel = when (theme.typeLabel) {
        "Business Runway" -> "Runway"
        "Business Operations" -> "Ops"
        else -> "Capacity"
    }
    val familyLabel = businessFamily
        ?.replace("_", " ")
        ?.takeIf { it.isNotBlank() }
        ?: theme.typeLabel

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let {
            Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                theme.pulseTitle.uppercase(),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${pulse?.attentionCount ?: 0}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    " attention",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Box(modifier = Modifier.weight(1f))
                Text(
                    if (isEmpty) "No signal yet" else "Live",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            pulse?.financialHealthScore?.takeIf { it.isNotBlank() }?.let { health ->
                Text(
                    "Health $health",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricChip(
                label = "Moments",
                value = "${pulse?.activeMomentCount ?: 0}",
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label = "Family",
                value = familyLabel,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label = runwayLabel,
                value = pulse?.runwayMonths?.takeIf { it.isNotBlank() } ?: "—",
                theme = theme,
                modifier = Modifier.weight(1f),
            )
        }

        if (isEmpty) {
            EmptyFinanceCard(theme)
        } else {
            TotalsCard(theme, totals)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .then(
                    if (momentId.isNullOrBlank()) Modifier
                    else Modifier.clickable(onClick = onAddExpense),
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                primaryCta,
                color = Color.White.copy(alpha = if (momentId.isNullOrBlank()) 0.5f else 1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }

        Text(
            "Open Action Center",
            color = theme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenQuickAdd)
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            color = theme.secondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            value,
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyFinanceCard(theme: BusinessActiveTheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "No finance signal yet",
            color = theme.text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Live KPIs appear when pulse/finance projections have data. Missing metrics stay empty.",
            color = theme.secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun TotalsCard(theme: BusinessActiveTheme, totals: List<BusinessFinanceTotalDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Totals",
            color = theme.text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
        )
        if (totals.isEmpty()) {
            Text(
                "No currency totals yet.",
                color = theme.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            totals.forEach { TotalRow(theme, it) }
        }
    }
}

@Composable
private fun TotalRow(theme: BusinessActiveTheme, total: BusinessFinanceTotalDto) {
    val hide = SecurityPreferences(LocalContext.current).hideBalances()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            total.currencyCode,
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Spent ${BalanceMask.mask(total.expenseTotal, hide)}",
                color = theme.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Revenue ${BalanceMask.mask(total.revenueTotal, hide)}",
                color = theme.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Invoices ${BalanceMask.mask(total.invoiceOutstandingTotal, hide)}",
                color = theme.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}
