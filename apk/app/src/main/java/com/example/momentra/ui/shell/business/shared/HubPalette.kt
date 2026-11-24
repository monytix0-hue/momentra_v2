package com.example.momentra.ui.shell.business.shared

import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.theme.generated.BusinessTokens

data class HubPalette(
    val bg: Color,
    val card: Color,
    val border: Color,
    val muted: Color,
    val text: Color,
    val accent: Color,
    val onAccent: Color,
)

fun pulseGroupTitle(code: String): String = when (code.uppercase()) {
    "TARGET_BUDGET" -> "Target & budget"
    "FINANCE" -> "Finance"
    "PAYMENT_READINESS" -> "Payment readiness"
    "ATTENTION" -> "Needs Attention"
    "ACTIVITY" -> "Activity"
    "GST" -> "GST"
    else -> code.replace('_', ' ')
}

fun hubPaletteFor(typeCode: String?, familyCode: String?): HubPalette {
    val code = typeCode?.trim()?.uppercase()
    if (code == DistributionDealershipHub.TYPE_CODE) {
        return HubPalette(
            bg = BusinessTokens.Colors.DISTRIBUTIONCanvas,
            card = BusinessTokens.Colors.DISTRIBUTIONSurface,
            border = BusinessTokens.Colors.DISTRIBUTIONAccent.copy(alpha = 0.28f),
            muted = BusinessTokens.Colors.DISTRIBUTIONHint,
            text = BusinessTokens.Colors.DISTRIBUTIONLabel,
            accent = BusinessTokens.Colors.DISTRIBUTIONAccent,
            onAccent = BusinessTokens.Colors.DISTRIBUTIONOnAccent,
        )
    }
    if (OwnMarketingHub.isOwn(code)) {
        return HubPalette(
            bg = BusinessTokens.Colors.MARKETING_OWNCanvas,
            card = BusinessTokens.Colors.MARKETING_OWNSurface,
            border = BusinessTokens.Colors.MARKETING_OWNAccent.copy(alpha = 0.28f),
            muted = BusinessTokens.Colors.MARKETING_OWNHint,
            text = BusinessTokens.Colors.MARKETING_OWNLabel,
            accent = BusinessTokens.Colors.MARKETING_OWNAccent,
            onAccent = BusinessTokens.Colors.MARKETING_OWNOnAccent,
        )
    }
    val tradingWholesale = code == "TRADING_WHOLESALE" ||
        (familyCode == "TRADING_DISTRIBUTION" && code != DistributionDealershipHub.TYPE_CODE)
    return if (tradingWholesale) {
        HubPalette(
            bg = BusinessTokens.Colors.TRADINGCanvas,
            card = Color(0xFF111827),
            border = BusinessTokens.Colors.TRADINGAccent.copy(alpha = 0.28f),
            muted = Color(0xFF94A3B8),
            text = Color(0xFFF1F5F9),
            accent = BusinessTokens.Colors.TRADINGAccent,
            onAccent = BusinessTokens.Colors.TRADINGOnAccent,
        )
    } else {
        val accent = BusinessAppStateAccent.forTypeCode(typeCode).color
        HubPalette(
            bg = Color(0xFF0C0F15),
            card = Color(0xFF161B26),
            border = Color(0xFF1E293B),
            muted = Color(0xFF94A3B8),
            text = Color(0xFFE5E0EE),
            accent = accent,
            onAccent = Color(0xFF0C0F15),
        )
    }
}