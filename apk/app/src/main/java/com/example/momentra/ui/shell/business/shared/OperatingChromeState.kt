package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingAttentionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto

object OperatingChromeState {
    var participantNames by mutableStateOf<List<String>>(emptyList())
    var editing by mutableStateOf<OperatingActivityDto?>(null)
    var settling by mutableStateOf<OperatingPaymentDto?>(null)
    var previewStoryId by mutableStateOf<String?>(null)
    var charging by mutableStateOf<OperatingActivityDto?>(null)
    var quickAddHint by mutableStateOf<String?>(null)
    var record: (suspend (OperatingCaptureDraft) -> Unit)? = null
}

fun openOperatingAttention(
    item: OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: (() -> Unit)? = null,
) {
    val sourceId = item.sourceEventId
        ?: if (item.id.startsWith("open-")) item.id.removePrefix("open-") else item.id
    when (item.cta) {
        "collect", "pay" -> {
            model.paymentReadiness.firstOrNull { it.id == sourceId }?.let { OperatingChromeState.settling = it }
        }
        "booking", "approval" -> {
            model.activity.firstOrNull { it.id == sourceId }?.let { OperatingChromeState.editing = it }
        }
        "charge" -> {
            model.activity.firstOrNull { it.id == sourceId }?.let { OperatingChromeState.charging = it }
        }
        "stock", "budget", "shipment", "activity" -> {
            OperatingChromeState.quickAddHint = item.cta
            onOpenQuickAdd?.invoke()
        }
        else -> {
            model.paymentReadiness.firstOrNull { it.id == sourceId }?.let { OperatingChromeState.settling = it }
                ?: model.activity.firstOrNull { it.id == sourceId }?.let { OperatingChromeState.editing = it }
        }
    }
}

fun operatingAttentionCtaLabel(item: OperatingAttentionDto): String = when (item.cta) {
    "collect" -> "Collect"
    "pay" -> "Pay"
    "booking" -> "Update booking"
    "charge" -> "Finalize charge"
    "approval" -> "Review"
    "stock" -> "Review stock"
    "budget" -> "Review budget"
    "activity" -> "Open activity"
    "shipment" -> "Record shipment"
    else -> "Open"
}
