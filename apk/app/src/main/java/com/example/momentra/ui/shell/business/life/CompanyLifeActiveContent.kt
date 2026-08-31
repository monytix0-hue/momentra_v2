package com.example.momentra.ui.shell.business.life

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.BusinessLifePayloadDto
import com.example.momentra.data.api.WeeklyReportDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.life.components.CompanyLifeActivitySection
import com.example.momentra.ui.shell.business.life.components.CompanyLifeColors
import com.example.momentra.ui.shell.business.life.components.CompanyLifeFilter
import com.example.momentra.ui.shell.business.life.components.CompanyLifeFilterChips
import com.example.momentra.ui.shell.business.life.components.CompanyLifeGradientButton
import com.example.momentra.ui.shell.business.life.components.CompanyLifeHealthHeader
import com.example.momentra.ui.shell.business.life.components.CompanyLifeJourneySection
import com.example.momentra.ui.shell.business.life.components.CompanyLifeModuleCards
import com.example.momentra.ui.shell.business.life.components.CompanyLifeOutlineButton
import com.example.momentra.ui.shell.business.life.components.CompanyLifeSignalsSection
import com.example.momentra.ui.shell.business.life.components.CompanyLifeTrendsSection
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Figma `695:9782` company-unified Business Life — live bind; honest empties. */
@Composable
fun CompanyLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onViewReport: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessLifePayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(CompanyLifeFilter.ALL) }
    var report by remember { mutableStateOf<WeeklyReportDto?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var shareBusy by remember { mutableStateOf(false) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = payload == null
        error = null
        repository.getLife(momentId).fold(
            onSuccess = { payload = it.payload },
            onFailure = { error = it.message },
        )
        loading = false
    }

    if (loading && payload == null) {
        Box(
            modifier = modifier.fillMaxSize().background(CompanyLifeColors.Bg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = CompanyLifeColors.Indigo)
        }
        return
    }

    val kpis = payload?.kpis
    val scoreRaw = kpis?.financialHealthScore?.trim()?.takeIf { it.isNotBlank() }
    val score = scoreRaw?.toDoubleOrNull()?.toInt()?.toString()
        ?: scoreRaw?.takeIf { it.all { ch -> ch.isDigit() } }
        ?: "—"
    val hasLiveScore = score != "—"
    val attention = kpis?.attentionCount ?: 0
    val narrative = when {
        !hasLiveScore && attention > 0 -> "Needs attention"
        !hasLiveScore -> "Awaiting live company signal"
        (score.toIntOrNull() ?: 0) >= 80 && attention == 0 -> "Healthy"
        (score.toIntOrNull() ?: 0) >= 60 -> "Watch"
        else -> "Needs focus"
    }
    val subtitle = when {
        !hasLiveScore -> "Activate modules and log activity to surface company health."
        attention > 0 -> "$attention open signal${if (attention == 1) "" else "s"} need review across modules."
        else -> "Modules with live data are within available thresholds. No critical alerts invented."
    }
    val moduleCount = kpis?.activeModuleCount ?: 0
    val momentCount = kpis?.activeMomentCount ?: 0
    val runway = kpis?.runwayMonths?.trim()?.takeIf { it.isNotBlank() }
    val activeModulesLabel = "$moduleCount MODULE${if (moduleCount == 1) "" else "S"}"
    val momentsLabel = "$momentCount MOMENT${if (momentCount == 1) "" else "S"}"
    val runwayLabel = runway?.let { "$it MONTHS" } ?: "—"

    val familyKey = filter.familyKey
    val signals = payload?.signals.orEmpty().filter { familyKey == null || it.family.equals(familyKey, true) }
    val activity = payload?.activity.orEmpty().filter { familyKey == null || it.family.equals(familyKey, true) }
    val journey = payload?.journey.orEmpty().filter {
        familyKey == null || it.family.equals(familyKey, true) || it.familyCode.contains(
            when (familyKey) {
                "TEAM_OPS" -> "TEAM"
                "RUNWAY" -> "RUNWAY"
                "OPERATIONS" -> "OPERATIONS"
                else -> ""
            },
            ignoreCase = true,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompanyLifeColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        error?.let {
            Text(
                it,
                color = CompanyLifeColors.Red,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        actionMessage?.let {
            Text(
                it,
                color = CompanyLifeColors.Indigo,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = CompanyLifeColors.Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }

        CompanyLifeFilterChips(selected = filter, onSelect = { filter = it })

        CompanyLifeHealthHeader(
            score = score,
            narrative = narrative,
            subtitle = subtitle,
            activeModules = activeModulesLabel,
            totalMoments = momentsLabel,
            avgRunway = runwayLabel,
        )

        CompanyLifeModuleCards(
            team = payload?.modules?.teamOperations,
            runway = payload?.modules?.runway,
            ops = payload?.modules?.businessOperations,
            vendor = payload?.modules?.vendorOperations,
        )

        CompanyLifeSignalsSection(signals = signals)
        CompanyLifeActivitySection(items = activity)
        CompanyLifeJourneySection(steps = journey)
        CompanyLifeTrendsSection(trends = payload?.trends)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompanyLifeGradientButton(
                label = "View Detailed Report",
                enabled = !momentId.isNullOrBlank(),
                onClick = {
                    val id = momentId ?: return@CompanyLifeGradientButton
                    scope.launch {
                        repository.getWeeklyReport(id).fold(
                            onSuccess = {
                                report = it
                                showReport = true
                            },
                            onFailure = {
                                actionMessage = it.message ?: "Report unavailable"
                                onViewReport()
                            },
                        )
                    }
                },
            )
            CompanyLifeOutlineButton(
                label = if (shareBusy) "Sharing…" else "Share with Team",
                enabled = !momentId.isNullOrBlank() && !shareBusy,
                onClick = {
                    val id = momentId ?: return@CompanyLifeOutlineButton
                    shareBusy = true
                    scope.launch {
                        repository.createShareLink(id).fold(
                            onSuccess = { link ->
                                shareBusy = false
                                val url = link.shareUrl.orEmpty()
                                if (url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share business dashboard"))
                                }
                                actionMessage = link.note ?: "Share link created"
                            },
                            onFailure = {
                                shareBusy = false
                                actionMessage = it.message ?: "Share unavailable"
                            },
                        )
                    }
                },
            )
        }
    }

    if (showReport && report != null) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text(report?.title ?: "Weekly Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report?.sections.orEmpty().forEach { section ->
                        Text(section.heading, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        section.items.forEach { item ->
                            Text("• $item", fontSize = 12.sp)
                        }
                    }
                    if (report?.sections.isNullOrEmpty()) {
                        Text(report?.note ?: "No activity in this period.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReport = false }) { Text("Close") }
            },
        )
    }
}
