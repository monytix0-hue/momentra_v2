package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupBudgetSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    currencyCode: String = "INR",
    isWedding: Boolean = false,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentDisplay by remember { mutableStateOf<String?>(null) }
    val sheetBg = if (isWedding) WeddingActiveTheme.Card else TripSheetTokens.Bg

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        repository.getFinance(momentId).fold(
            onSuccess = { facet ->
                val total = facet.payload?.totals?.firstOrNull { it.currencyCode == currencyCode }
                    ?: facet.payload?.totals?.firstOrNull()
                currentDisplay = total?.budgetTotal?.let {
                    GroupFinanceFormat.formatMoney(it, total.currencyCode)
                }
            },
            onFailure = { currentDisplay = null },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        GroupBudgetEditSheet(
            momentId = momentId,
            currencyCode = currencyCode,
            currentBudgetDisplay = currentDisplay,
            onDismiss = onDismiss,
            onSaved = {
                onSaved()
                onDismiss()
            },
            repository = repository,
            tripChrome = !isWedding,
        )
    }
}
