package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GroupBudgetCustomField
import com.example.momentra.ui.shell.empty.group.GroupBudgetUtils
import com.example.momentra.ui.shell.empty.group.GroupSetupTheme
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

@Composable
fun GroupBudgetEditSheet(
    momentId: String,
    currencyCode: String,
    currentBudgetDisplay: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    tripChrome: Boolean = false,
) {
    var amount by remember { mutableStateOf(currentBudgetDisplay?.replace("₹", "") ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cardBg = if (tripChrome) TripSheetTokens.Bg else GroupSetupTheme.Card
    val borderColor = if (tripChrome) TripSheetTokens.Border else GroupSetupTheme.Border
    val titleColor = if (tripChrome) TripSheetTokens.Text else GroupActiveTheme.Text
    val subtitleColor = if (tripChrome) TripSheetTokens.Muted else GroupActiveTheme.Secondary

    fun submit() {
        val parsed = GroupBudgetUtils.parseDisplayToApiAmount(amount)
        if (parsed == null) {
            error = "Enter a valid amount"
            return
        }
        saving = true
        error = null
        scope.launch {
            repository.patchGroupBudget(momentId, parsed, currencyCode).fold(
                onSuccess = {
                    saving = false
                    onSaved()
                },
                onFailure = {
                    saving = false
                    error = it.message ?: "Save failed"
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TripSheetTokens.Accent.copy(alpha = 0.18f))
                    .border(1.dp, TripSheetTokens.Accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("💰", fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Edit group budget",
                    color = titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Updates the planned budget for this shared moment.",
                    color = subtitleColor,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        GroupBudgetCustomField(
            value = amount,
            onValueChange = { amount = it },
            currencyCode = currencyCode,
        )
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (tripChrome) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TripSheetTokens.Field)
                        .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(14.dp))
                        .clickable(enabled = !saving, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", color = TripSheetTokens.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(TripSheetTokens.Accent, TripSheetTokens.AccentEnd)))
                        .clickable(enabled = !saving, onClick = { submit() })
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (saving) "Saving…" else "Save",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupCtaButton(
                    label = "Cancel",
                    enabled = !saving,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                GroupCtaButton(
                    label = if (saving) "Saving…" else "Save",
                    enabled = !saving,
                    onClick = { submit() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
