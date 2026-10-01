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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.momentra.ui.shell.business.life.components.CompanyLifeColors
import com.example.momentra.ui.shell.business.life.components.CompanyLifeGradientButton
import com.example.momentra.ui.shell.business.life.components.CompanyLifeOutlineButton
import com.example.momentra.ui.shell.business.shared.BusinessLifeActivityFact
import com.example.momentra.ui.shell.business.shared.BusinessLifeFacts
import com.example.momentra.ui.shell.business.shared.BusinessLifeLens
import com.example.momentra.ui.shell.business.shared.BusinessLifeMovementTab
import com.example.momentra.ui.shell.business.shared.BusinessLifeSignalFact
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.shared.buildBusinessLifeCompanyState
import com.example.momentra.ui.shell.business.shared.businessLifeLens
import com.example.momentra.ui.shell.business.shared.movementTabFor
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Company Life. The payload is the company. The lens only changes focus. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun CompanyLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onViewReport: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenVendor: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    companyId: String? = null,
    momentTypeCode: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessLifePayloadDto?>(null) }
    var shownCompany by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var lens by remember(momentTypeCode) { mutableStateOf(businessLifeLens(momentTypeCode)) }
    var movement by remember(lens) { mutableStateOf(movementTabFor(lens)) }
    var retry by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<WeeklyReportDto?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var shareBusy by remember { mutableStateOf(false) }

    LaunchedEffect(companyId) {
        if (!companyId.isNullOrBlank() && shownCompany != null && companyId != shownCompany) {
            payload = null
            error = null
            shownCompany = companyId
        }
    }

    LaunchedEffect(momentTypeCode) {
        val next = businessLifeLens(momentTypeCode)
        lens = next
        movement = movementTabFor(next)
    }

    LaunchedEffect(refreshToken, momentId, companyId, retry) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        if (payload == null) {
            BusinessTabDataCache.peekPulse(momentId)?.life?.let { payload = it }
        }
        loading = payload == null
        error = null
        repository.getLife(momentId).fold(
            onSuccess = { facet ->
                val incoming = facet.companyId
                if (!companyId.isNullOrBlank() && !incoming.isNullOrBlank() && incoming != companyId) return@fold
                if (!shownCompany.isNullOrBlank() && !incoming.isNullOrBlank() && incoming != shownCompany && companyId.isNullOrBlank()) {
                    return@fold
                }
                payload = facet.payload
                shownCompany = incoming ?: companyId
                BusinessTabDataCache.putLife(momentId, facet.payload)
            },
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

    val state = payload?.let {
        buildBusinessLifeCompanyState(it.toLifeFacts(), Instant.now(), zone)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompanyLifeColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Company life",
            color = CompanyLifeColors.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        BusinessLifeLensChips(selected = lens, onSelect = { lens = it })
        error?.let {
            Text(it, color = CompanyLifeColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            if (payload == null) {
                TextButton(onClick = { retry += 1 }) { Text("Try again") }
            }
        }
        actionMessage?.let {
            Text(it, color = CompanyLifeColors.Indigo, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (state != null) {
            BusinessLifeBlocks(state = state, movement = movement, onMovement = { movement = it })
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

private fun BusinessLifePayloadDto.toLifeFacts(): BusinessLifeFacts = BusinessLifeFacts(
    sections = sections,
    runway = runwayPayload.orEmpty(),
    daily = businessOperationsPayload.orEmpty(),
    team = teamOperationsPayload.orEmpty(),
    runwayStatus = runwayPayload.statusLabel() ?: modules?.runway?.statusLabel,
    dailyStatus = businessOperationsPayload.statusLabel() ?: modules?.businessOperations?.statusLabel,
    teamStatus = teamOperationsPayload.statusLabel() ?: modules?.teamOperations?.statusLabel,
    runwayMonths = kpis?.runwayMonths,
    signals = signals.map { BusinessLifeSignalFact(it.title, it.family, it.statusLabel) },
    activity = activity.map { BusinessLifeActivityFact(it.title, it.occurredAt, it.activityCode) },
)

private fun Map<String, Any?>?.statusLabel(): String? =
    this?.get("statusLabel")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
