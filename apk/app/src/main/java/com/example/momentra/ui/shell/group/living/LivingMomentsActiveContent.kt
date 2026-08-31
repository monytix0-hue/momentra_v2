package com.example.momentra.ui.shell.group.living

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.GroupActiveLoading
import com.example.momentra.ui.shell.group.GroupFinanceFormat
import com.example.momentra.ui.shell.group.GroupTabDataCache
import com.example.momentra.ui.shell.group.loadGroupPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans

/** Shared Living Moments. Live APIs only. */
@Composable
fun LivingMomentsActiveContent(
    theme: LivingActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var life by remember { mutableStateOf<GroupLifePayloadDto?>(null) }
    var residents by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var assets by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var maintenance by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            loading = false
        } ?: run { loading = true }
        loadGroupPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                title = data.title
                pulse = data.pulse
                finance = data.finance
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
        repository.getLife(momentId).onSuccess { facet ->
            life = facet.payload
            GroupTabDataCache.putLife(momentId, facet.payload)
        }
        repository.listResidents(momentId).onSuccess { residents = it.items }
        repository.listSharedAssets(momentId).onSuccess { assets = it.items }
        repository.listMaintenanceRecords(momentId).onSuccess { maintenance = it.items }
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val expenseTotal = finance?.totals?.firstOrNull()?.expenseTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = residents.size.takeIf { it > 0 } ?: pulse?.participantCount ?: 0
    val openTasks = pulse?.openTaskCount ?: life?.openTaskCount ?: 0
    val displayTitle = momentTitle ?: title ?: "${theme.typeLabel} Moments"
    val planningItems = life?.planningItems.orEmpty()
    val updates = life?.updates.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("SHARED LIVING", color = theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(displayTitle, color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LivingStatCard("RESIDENTS", "$peopleCount", theme.statGradients[0], Modifier.weight(1f))
                LivingStatCard("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), theme.statGradients[1], Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LivingStatCard("ASSETS", "${assets.size}", theme.statGradients[2], Modifier.weight(1f))
                LivingStatCard("TASKS", "$openTasks", theme.statGradients.getOrElse(3) { theme.statGradients[0] }, Modifier.weight(1f))
            }
            if (expenseTotal != null) {
                Text(
                    "Spent ${GroupFinanceFormat.compactMoney(expenseTotal, currency)}",
                    color = theme.accentLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        LivingSectionCard(theme, "Planning & Tasks") {
            if (planningItems.isEmpty()) {
                LivingEmptyBlock(theme, "No tasks yet", "Add a task from Quick Add — nothing is invented.")
            } else {
                planningItems.forEach { item ->
                    LivingListRow(theme, item.title ?: item.planningItemId.orEmpty())
                }
            }
        }

        LivingSectionCard(theme, "Updates") {
            if (updates.isEmpty()) {
                LivingEmptyBlock(theme, "No updates yet", "Share a status update from Quick Add.")
            } else {
                updates.take(8).forEach {
                    Text(it.message ?: it.updateId.orEmpty(), color = theme.text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
            }
        }

        LivingSectionCard(theme, "Residents") {
            if (residents.isEmpty()) {
                LivingEmptyBlock(theme, "No residents yet", "Add a resident from Quick Add.")
            } else {
                residents.forEach { r ->
                    val name = r["name"]?.toString()
                        ?: r["displayName"]?.toString()
                        ?: r["residentId"]?.toString()
                        ?: "Resident"
                    val role = r["roleCode"]?.toString() ?: r["role"]?.toString()
                    LivingListRow(theme, if (!role.isNullOrBlank()) "$name · $role" else name)
                }
            }
        }

        LivingSectionCard(theme, "Shared Assets") {
            if (assets.isEmpty()) {
                LivingEmptyBlock(theme, "No shared assets yet", "Add an asset from Quick Add.")
            } else {
                assets.forEach { item ->
                    val label = item["title"]?.toString()
                        ?: item["sharedAssetId"]?.toString()
                        ?: "Asset"
                    val type = item["assetType"]?.toString()
                    LivingListRow(theme, if (!type.isNullOrBlank()) "$label · $type" else label)
                }
            }
        }

        LivingSectionCard(theme, "Maintenance") {
            if (maintenance.isEmpty()) {
                LivingEmptyBlock(theme, "No maintenance records yet", "Log maintenance from Quick Add.")
            } else {
                maintenance.forEach { item ->
                    val label = item["title"]?.toString()
                        ?: item["maintenanceRecordId"]?.toString()
                        ?: "Maintenance"
                    val status = item["status"]?.toString()
                    LivingListRow(theme, if (!status.isNullOrBlank()) "$label · $status" else label)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add to the ${theme.typeLabel.lowercase()} story", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text("Add a resident, task, asset, or update.", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(
                "+ Open Quick Add",
                color = theme.darkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpenQuickAdd)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun LivingListRow(theme: LivingActiveTheme, label: String) {
    Text(
        label,
        color = theme.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg)
            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}
