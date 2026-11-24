package com.example.momentra.ui.shell.business.setup

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.theme.generated.BusinessDomain
import com.example.momentra.ui.theme.generated.BusinessTokens

/**
 * Per-moment setup palette resolved from `BusinessTokens` domain colors.
 * Chip swaps retheme the whole document from the new type code.
 * Keep in lockstep with `BusinessSetupTheme.swift`.
 */
data class BusinessSetupTheme(
    val typeCode: String,
    val domain: BusinessDomain,
    val canvas: Color,
    val canvasTop: Color,
    val surface: Color,
    val inset: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    val onAccent: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val label: Color,
    val hint: Color,
) {
    val heroBrush: Brush
        get() = Brush.linearGradient(listOf(heroStart, heroEnd))

    val accentBrush: Brush
        get() = Brush.horizontalGradient(listOf(accent, accentSoft))

    companion object {
        fun forTypeCode(raw: String?): BusinessSetupTheme {
            val code = (raw ?: "").uppercase()
            val domain = domainFor(code)
            val c = BusinessTokens.Colors
            return when (domain) {
                BusinessDomain.MARKETING -> theme(
                    code, domain, c.MARKETINGCanvas, c.MARKETINGCanvasTop, c.MARKETINGSurface, c.MARKETINGInset,
                    c.MARKETINGAccent, c.MARKETINGAccentSoft, c.MARKETINGAccentDeep, c.MARKETINGOnAccent,
                    c.MARKETINGHeroStart, c.MARKETINGHeroEnd, c.MARKETINGLabel, c.MARKETINGHint,
                )
                BusinessDomain.MARKETING_OWN -> theme(
                    code, domain, c.MARKETING_OWNCanvas, c.MARKETING_OWNCanvasTop, c.MARKETING_OWNSurface, c.MARKETING_OWNInset,
                    c.MARKETING_OWNAccent, c.MARKETING_OWNAccentSoft, c.MARKETING_OWNAccentDeep, c.MARKETING_OWNOnAccent,
                    c.MARKETING_OWNHeroStart, c.MARKETING_OWNHeroEnd, c.MARKETING_OWNLabel, c.MARKETING_OWNHint,
                )
                BusinessDomain.TRADING -> theme(
                    code, domain, c.TRADINGCanvas, c.TRADINGCanvasTop, c.TRADINGSurface, c.TRADINGInset,
                    c.TRADINGAccent, c.TRADINGAccentSoft, c.TRADINGAccentDeep, c.TRADINGOnAccent,
                    c.TRADINGHeroStart, c.TRADINGHeroEnd, c.TRADINGLabel, c.TRADINGHint,
                )
                BusinessDomain.DISTRIBUTION -> theme(
                    code, domain, c.DISTRIBUTIONCanvas, c.DISTRIBUTIONCanvasTop, c.DISTRIBUTIONSurface, c.DISTRIBUTIONInset,
                    c.DISTRIBUTIONAccent, c.DISTRIBUTIONAccentSoft, c.DISTRIBUTIONAccentDeep, c.DISTRIBUTIONOnAccent,
                    c.DISTRIBUTIONHeroStart, c.DISTRIBUTIONHeroEnd, c.DISTRIBUTIONLabel, c.DISTRIBUTIONHint,
                )
                BusinessDomain.RETAIL -> theme(
                    code, domain, c.RETAILCanvas, c.RETAILCanvasTop, c.RETAILSurface, c.RETAILInset,
                    c.RETAILAccent, c.RETAILAccentSoft, c.RETAILAccentDeep, c.RETAILOnAccent,
                    c.RETAILHeroStart, c.RETAILHeroEnd, c.RETAILLabel, c.RETAILHint,
                )
                BusinessDomain.RESTAURANT -> theme(
                    code, domain, c.RESTAURANTCanvas, c.RESTAURANTCanvasTop, c.RESTAURANTSurface, c.RESTAURANTInset,
                    c.RESTAURANTAccent, c.RESTAURANTAccentSoft, c.RESTAURANTAccentDeep, c.RESTAURANTOnAccent,
                    c.RESTAURANTHeroStart, c.RESTAURANTHeroEnd, c.RESTAURANTLabel, c.RESTAURANTHint,
                )
                BusinessDomain.SERVICES -> theme(
                    code, domain, c.SERVICESCanvas, c.SERVICESCanvasTop, c.SERVICESSurface, c.SERVICESInset,
                    c.SERVICESAccent, c.SERVICESAccentSoft, c.SERVICESAccentDeep, c.SERVICESOnAccent,
                    c.SERVICESHeroStart, c.SERVICESHeroEnd, c.SERVICESLabel, c.SERVICESHint,
                )
                BusinessDomain.MANUFACTURING -> theme(
                    code, domain, c.MANUFACTURINGCanvas, c.MANUFACTURINGCanvasTop, c.MANUFACTURINGSurface, c.MANUFACTURINGInset,
                    c.MANUFACTURINGAccent, c.MANUFACTURINGAccentSoft, c.MANUFACTURINGAccentDeep, c.MANUFACTURINGOnAccent,
                    c.MANUFACTURINGHeroStart, c.MANUFACTURINGHeroEnd, c.MANUFACTURINGLabel, c.MANUFACTURINGHint,
                )
                BusinessDomain.MADE_TO_ORDER -> theme(
                    code, domain, c.MADE_TO_ORDERCanvas, c.MADE_TO_ORDERCanvasTop, c.MADE_TO_ORDERSurface, c.MADE_TO_ORDERInset,
                    c.MADE_TO_ORDERAccent, c.MADE_TO_ORDERAccentSoft, c.MADE_TO_ORDERAccentDeep, c.MADE_TO_ORDEROnAccent,
                    c.MADE_TO_ORDERHeroStart, c.MADE_TO_ORDERHeroEnd, c.MADE_TO_ORDERLabel, c.MADE_TO_ORDERHint,
                )
                BusinessDomain.CORPORATE -> theme(
                    code, domain, c.CORPORATECanvas, c.CORPORATECanvasTop, c.CORPORATESurface, c.CORPORATEInset,
                    c.CORPORATEAccent, c.CORPORATEAccentSoft, c.CORPORATEAccentDeep, c.CORPORATEOnAccent,
                    c.CORPORATEHeroStart, c.CORPORATEHeroEnd, c.CORPORATELabel, c.CORPORATEHint,
                )
            }
        }

        fun domainFor(typeCode: String): BusinessDomain {
            val code = typeCode.uppercase()
            return when {
                code == "CLIENT_CAMPAIGN" -> BusinessDomain.MARKETING
                code == "OWN_BUSINESS_CAMPAIGN" -> BusinessDomain.MARKETING_OWN
                code == "TRADING_WHOLESALE" -> BusinessDomain.TRADING
                code == "DISTRIBUTION_DEALERSHIP" -> BusinessDomain.DISTRIBUTION
                code == "RETAIL_STORE" -> BusinessDomain.RETAIL
                code == "RESTAURANT_FOOD" -> BusinessDomain.RESTAURANT
                code == "CONSUMER_SERVICE" -> BusinessDomain.SERVICES
                code == "STANDARD_PRODUCTION" -> BusinessDomain.MANUFACTURING
                code == "MADE_TO_ORDER" -> BusinessDomain.MADE_TO_ORDER
                code.contains("OWN") && code.contains("CAMPAIGN") -> BusinessDomain.MARKETING_OWN
                code.contains("MARKETING") || code.contains("CAMPAIGN") -> BusinessDomain.MARKETING
                code.contains("DISTRIBUTION") -> BusinessDomain.DISTRIBUTION
                code.contains("TRADING") -> BusinessDomain.TRADING
                code.contains("RESTAURANT") -> BusinessDomain.RESTAURANT
                code.contains("RETAIL") -> BusinessDomain.RETAIL
                code.contains("SERVICE") -> BusinessDomain.SERVICES
                code.contains("MADE") || code.contains("ORDER") -> BusinessDomain.MADE_TO_ORDER
                code.contains("MANUFACTUR") || code.contains("PRODUCTION") -> BusinessDomain.MANUFACTURING
                else -> BusinessDomain.TRADING
            }
        }

        private fun theme(
            code: String,
            domain: BusinessDomain,
            canvas: Color,
            canvasTop: Color,
            surface: Color,
            inset: Color,
            accent: Color,
            soft: Color,
            deep: Color,
            onAccent: Color,
            heroStart: Color,
            heroEnd: Color,
            label: Color,
            hint: Color,
        ) = BusinessSetupTheme(
            typeCode = code.ifBlank { domain.value.uppercase() },
            domain = domain,
            canvas = canvas,
            canvasTop = canvasTop,
            surface = surface,
            inset = inset,
            accent = accent,
            accentSoft = soft,
            accentDeep = deep,
            onAccent = onAccent,
            heroStart = heroStart,
            heroEnd = heroEnd,
            label = label,
            hint = hint,
        )
    }
}
