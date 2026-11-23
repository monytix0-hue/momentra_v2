package com.example.momentra.ui.shell.business.shared

/**
 * Callers: OperatingMomentScreens.kt (widget tiles + source drill-down labels).
 * No existing file maps COMPLETE|PARTIAL|UNAVAILABLE|ZERO to mobile presentation.
 * Reads: server coverage string. Writes: none — never derives finance totals.
 * User: "Map the backend's COMPLETE, PARTIAL, UNAVAILABLE and ZERO states into explicit
 * mobile presentation states." + "Do not allow mobile clients to calculate authoritative
 * finance totals independently."
 */
enum class WidgetCoveragePresentation(
    val serverCode: String,
    val label: String,
    val showValue: Boolean,
    val showSources: Boolean,
) {
    COMPLETE("COMPLETE", "Complete", showValue = true, showSources = true),
    PARTIAL("PARTIAL", "Partial — incomplete history", showValue = false, showSources = true),
    UNAVAILABLE("UNAVAILABLE", "Unavailable", showValue = false, showSources = false),
    ZERO("ZERO", "Zero", showValue = true, showSources = true),
    UNKNOWN("", "Unknown coverage", showValue = false, showSources = false);

    companion object {
        fun fromServer(code: String?): WidgetCoveragePresentation =
            entries.firstOrNull { it.serverCode == code?.trim()?.uppercase() } ?: UNKNOWN
    }
}
