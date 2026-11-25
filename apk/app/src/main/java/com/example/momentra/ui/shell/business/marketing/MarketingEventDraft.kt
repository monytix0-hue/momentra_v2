package com.example.momentra.ui.shell.business.marketing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans

data class MarketingEventDraft(
    val eventType: String,
    val title: String,
    val amount: String?,
    val note: String?,
    val channelName: String?,
    val counterpartyName: String?,
    val paymentStatus: String?,
    val occurredAt: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val itemName: String? = null,
    val parentEventId: String? = null,
    val revisesEventId: String? = null,
    val masterKind: String? = null,
    val dueAt: String? = null,
    val contract: OperatingContractBody? = null,
    val reportingMode: String? = null,
    val metricCode: String? = null,
    val budgetCategory: String? = null,
    val activityStatus: String? = null,
    val attributedSales: String? = null,
    val revisionKind: String? = null,
    val costBasis: String? = null,
    val overlapDeduped: Boolean? = null,
    val creativeVersion: String? = null,
    val resultSource: String? = null,
    val idempotencyKey: String? = null,
    val effectiveFrom: String? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val supersedesEventId: String? = null,
    val lines: List<OperatingLineBody>? = null,
    val masterSellPrice: String? = null,
    val masterPurchasePrice: String? = null,
    val masterGstRate: String? = null,
    val masterHsn: String? = null,
    val masterGstApplicable: String? = null,
    val masterGstClassification: String? = null,
)

/**
 * V4 placeholder — client-campaign typed capture chrome was pruned; Pulse/typed form remain.
 */
@Composable
fun MarketingQuickAddHub(
    model: OperatingProjectionDto,
    momentId: String?,
    onRecord: (MarketingEventDraft) -> Unit,
    onDecide: (eventId: String, decision: String) -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Quick Adds for ${model.title ?: "campaign"} reopen from Pulse actions.",
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
    }
    @Suppress("UNUSED_PARAMETER")
    fun keep(a: Any?, b: Any?, c: Any?) = Unit
    keep(momentId, onRecord, onDecide)
}
