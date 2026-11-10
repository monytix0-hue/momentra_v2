package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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

    /** Memory-tab `Generate business story` sheet (TRD-MEMORY-v12 / DST-MEMORY-v12). */
    var storyConfigOpen by mutableStateOf(false)

    /** Masters sheet tab name, so Quick Add can deep-link into `Create SKU`. */
    var mastersOpen by mutableStateOf<String?>(null)

    /**
     * Parts / Materials browser (`MTO-SHEET` / `MFG-SHEET`).
     * Values: `"parts"` or `"materials"`.
     */
    var partsBrowserOpen by mutableStateOf<String?>(null)

    /**
     * Master chosen in the Parts/Materials browser. Capture sheets consume this on open
     * (name / id / qty defaults); hubs open the matching Quick Add when no capture is up.
     */
    var browserSelection by mutableStateOf<OperatingBrowserSelection?>(null)

    /** True while an `OperatingCaptureSheet` is mounted and can absorb [browserSelection]. */
    var captureOpen by mutableStateOf(false)

    /** `Switch Moment` sheet (TRD-SWITCH 2566:33577). */
    var switchMomentOpen by mutableStateOf(false)

    /** Set by the shell so the Switch Moment sheet can move the selected moment. */
    var onSwitchMoment: ((String) -> Unit)? = null

    /** Bumped after a master write so the open projection reloads. */
    var refresh by mutableStateOf(0)

    /**
     * View-all sheet period chip → API `period` query (`this_month` / `last_month` / `custom`).
     * Null keeps the historic unscoped load until the operator taps a chip.
     */
    var periodFilter by mutableStateOf<String?>(null)

    /** ISO dates for `period=custom` (`from` / `to` query params). */
    var periodFrom by mutableStateOf<String?>(null)
    var periodTo by mutableStateOf<String?>(null)

    /** The moment chrome sheets act on; set by `OperatingMomentPage` before rendering. */
    var momentId by mutableStateOf<String?>(null)

    /** Remap Figma period chip labels onto the operating projection query and reload. */
    fun applySheetPeriod(label: String) {
        when (label) {
            "Last month" -> {
                periodFilter = "last_month"
                periodFrom = null
                periodTo = null
            }
            "Custom range ›" -> {
                periodFilter = "custom"
                val today = java.time.LocalDate.now()
                if (periodFrom.isNullOrBlank()) periodFrom = today.withDayOfYear(1).toString()
                if (periodTo.isNullOrBlank()) periodTo = today.toString()
            }
            else -> {
                periodFilter = "this_month"
                periodFrom = null
                periodTo = null
            }
        }
        refresh += 1
    }

    /** Vertical accent/surface for the shared chrome sheets. */
    var storyAccent by mutableStateOf(Color(0xFF10B981))
    var storySurface by mutableStateOf(Color(0xFF090D16))
    var storyBorder by mutableStateOf(Color(0xFF1E293B))
    var storyInk by mutableStateOf(Color.White)
    var storyBody by mutableStateOf(Color(0xFF94A3B8))

    fun storyTheme(): OperatingSheetTheme = OperatingSheetTheme(
        accent = storyAccent,
        accentSoft = storyAccent,
        canvas = storySurface,
        surface = storySurface,
        inset = Color.Black.copy(alpha = 0.25f),
        border = storyBorder,
        ink = storyInk,
        body = storyBody,
        warning = Color(0xFFF59E0B),
        negative = Color(0xFFF87171),
    )
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
