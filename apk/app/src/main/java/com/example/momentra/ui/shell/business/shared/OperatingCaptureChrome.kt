package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Capture-sheet chrome for Operating Quick Adds.
 *
 * Default = Group lilac (`BusinessSheetTokens`) so Retail / Restaurant callers stay
 * unchanged. Trading (`variant=trading`) provides [Trading] (slate / emerald / cyan).
 * Distribution (`variant=distribution`) provides [Distribution] (slate / orange / sky).
 * Marketing Client (`variant=client`) provides [MarketingClient] (indigo / violet WHY).
 * Marketing Own (`variant=own`) provides [MarketingOwn] (magenta / amber WHY).
 *
 * Source: Figma `TzLvwVwlPbeVB8ug1zB3GM` TRD-QA + DST-QA + MKT-QA + MKT-OWN-QA frames.
 */
data class OperatingCaptureChrome(
    val sheet: Color,
    val field: Color,
    val border: Color,
    val hairline: Color,
    val chipBorder: Color,
    val ink: Color,
    val value: Color,
    val label: Color,
    val hint: Color,
    val chipIdleText: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentFillSoft: Color,
    val amountBorder: Color,
    val identity: Color,
    val elevated: Color,
    val brand: Color,
    val onAccent: Color,
    val cyan: Color,
    val whyFill: Color,
    val whyBorder: Color,
    val whyBody: Color,
    val danger: Color,
    val moreBorder: Color,
)

val LocalOperatingCaptureChrome = staticCompositionLocalOf { OperatingCaptureChromeTokens.Group }

object OperatingCaptureChromeTokens {
    /** Lilac Group sheet — historical default for non-trading families. */
    val Group = OperatingCaptureChrome(
        sheet = BusinessSheetTokens.bg,
        field = BusinessSheetTokens.field,
        border = BusinessSheetTokens.border,
        hairline = BusinessSheetTokens.border,
        chipBorder = BusinessSheetTokens.border,
        ink = BusinessSheetTokens.text,
        value = BusinessSheetTokens.text,
        label = BusinessSheetTokens.muted,
        hint = BusinessSheetTokens.muted,
        chipIdleText = BusinessSheetTokens.muted,
        accent = Color(0xFFB7B8FF),
        accentSoft = Color(0xFFB7B8FF),
        accentFillSoft = Color(0xFF6C4EF2),
        amountBorder = Color(0xFFC9BFFF),
        identity = Color(0xFF6C4EF2),
        elevated = BusinessSheetTokens.border,
        brand = Color(0xFFC9BFFF),
        onAccent = Color(0xFF2F009C),
        cyan = Color(0xFFB7B8FF),
        whyFill = BusinessSheetTokens.border,
        whyBorder = BusinessSheetTokens.border,
        whyBody = BusinessSheetTokens.text,
        danger = Color(0xFFF87171),
        moreBorder = BusinessSheetTokens.border,
    )

    /** TRD-QA capture chrome — slate sheet, emerald accent, cyan WHY. */
    val Trading = OperatingCaptureChrome(
        sheet = Color(0xFF151D2E),
        field = Color(0xFF1E293B),
        border = Color(0xFF1E293B),
        hairline = Color.White.copy(alpha = 0.08f),
        chipBorder = Color.White.copy(alpha = 0.12f),
        ink = Color(0xFFFFFFFF),
        value = Color(0xFFF1F5F9),
        label = Color(0xFF8B97AD),
        hint = Color(0xFF6B7790),
        chipIdleText = Color(0xFFC9D2E3),
        accent = Color(0xFF10B981),
        accentSoft = Color(0xFF6EE7B7),
        accentFillSoft = Color(0xFF10B981).copy(alpha = 0.15f),
        amountBorder = Color(0xFF10B981).copy(alpha = 0.33f),
        identity = Color(0xFF10B981),
        elevated = Color(0xFF1E293B),
        brand = Color(0xFF10B981),
        onAccent = Color(0xFFFFFFFF),
        cyan = Color(0xFF06B6D4),
        whyFill = Color(0xFF06B6D4).copy(alpha = 0.08f),
        whyBorder = Color(0xFF06B6D4).copy(alpha = 0.30f),
        whyBody = Color(0xFFCFEFF6),
        danger = Color(0xFFEF4444),
        moreBorder = Color.White.copy(alpha = 0.10f),
    )

    /**
     * DST-QA capture chrome — slate sheet `#13203A`, orange accent `#EA580C`,
     * sky WHY `#38BDF8` (Figma DST-QA-02).
     */
    val Distribution = OperatingCaptureChrome(
        sheet = Color(0xFF13203A),
        field = Color(0xFF1A2A4A),
        border = Color(0xFF1A2A4A),
        hairline = Color.White.copy(alpha = 0.08f),
        chipBorder = Color.White.copy(alpha = 0.12f),
        ink = Color(0xFFFFFFFF),
        value = Color(0xFFF1F5F9),
        label = Color(0xFF8B97AD),
        hint = Color(0xFF6B7790),
        chipIdleText = Color(0xFFC9D2E3),
        accent = Color(0xFFEA580C),
        accentSoft = Color(0xFFFFB598),
        accentFillSoft = Color(0xFFEA580C).copy(alpha = 0.15f),
        amountBorder = Color(0xFFEA580C).copy(alpha = 0.33f),
        identity = Color(0xFFEA580C),
        elevated = Color(0xFF1A2A4A),
        brand = Color(0xFFEA580C),
        onAccent = Color(0xFFFFFFFF),
        cyan = Color(0xFF38BDF8),
        whyFill = Color(0xFF38BDF8).copy(alpha = 0.08f),
        whyBorder = Color(0xFF38BDF8).copy(alpha = 0.30f),
        whyBody = Color(0xFFCFEFF6),
        danger = Color(0xFFEF4444),
        moreBorder = Color.White.copy(alpha = 0.10f),
    )

    /**
     * MKT-QA Client capture chrome — sheet `#1E293B`, field `#334155`,
     * indigo `#6366F1` → violet `#8B5CF6`, WHY body `#C4B5FD`.
     */
    val MarketingClient = OperatingCaptureChrome(
        sheet = Color(0xFF1E293B),
        field = Color(0xFF334155),
        border = Color(0xFF475569),
        hairline = Color.White.copy(alpha = 0.08f),
        chipBorder = Color(0xFF475569),
        ink = Color(0xFFF8FAFC),
        value = Color(0xFFF8FAFC),
        label = Color(0xFF94A3B8),
        hint = Color(0xFF64748B),
        chipIdleText = Color(0xFF94A3B8),
        accent = Color(0xFF6366F1),
        accentSoft = Color(0xFF8B5CF6),
        accentFillSoft = Color(0xFF6366F1).copy(alpha = 0.15f),
        amountBorder = Color(0xFF6366F1).copy(alpha = 0.40f),
        identity = Color(0xFF6366F1),
        elevated = Color(0xFF334155),
        brand = Color(0xFF8B5CF6),
        onAccent = Color(0xFFFFFFFF),
        cyan = Color(0xFF8B5CF6),
        whyFill = Color(0xFF8B5CF6).copy(alpha = 0.12f),
        whyBorder = Color(0xFF8B5CF6).copy(alpha = 0.40f),
        whyBody = Color(0xFFC4B5FD),
        danger = Color(0xFFEF4444),
        moreBorder = Color(0xFF8B5CF6).copy(alpha = 0.40f),
    )

    /**
     * MKT-OWN-QA capture chrome — sheet `#1E293B`, field `#334155`,
     * magenta `#D946EF` → amber `#F59E0B`, WHY body `#FCD34D`.
     * Never reuse [MarketingClient] indigo on Own.
     */
    val MarketingOwn = OperatingCaptureChrome(
        sheet = Color(0xFF1E293B),
        field = Color(0xFF334155),
        border = Color(0xFF475569),
        hairline = Color.White.copy(alpha = 0.08f),
        chipBorder = Color(0xFF475569),
        ink = Color(0xFFF8FAFC),
        value = Color(0xFFF8FAFC),
        label = Color(0xFF94A3B8),
        hint = Color(0xFF64748B),
        chipIdleText = Color(0xFF94A3B8),
        accent = Color(0xFFD946EF),
        accentSoft = Color(0xFFF59E0B),
        accentFillSoft = Color(0xFFD946EF).copy(alpha = 0.15f),
        amountBorder = Color(0xFFD946EF).copy(alpha = 0.40f),
        identity = Color(0xFFD946EF),
        elevated = Color(0xFF334155),
        brand = Color(0xFFF59E0B),
        onAccent = Color(0xFFFFFFFF),
        cyan = Color(0xFFF59E0B),
        whyFill = Color(0xFFF59E0B).copy(alpha = 0.12f),
        whyBorder = Color(0xFFF59E0B).copy(alpha = 0.40f),
        whyBody = Color(0xFFFCD34D),
        danger = Color(0xFFEF4444),
        moreBorder = Color(0xFFF59E0B).copy(alpha = 0.40f),
    )

    /** Amount-first / chip hero treatment for Trading + Distribution + Marketing Client/Own. */
    fun isOpsQa(chrome: OperatingCaptureChrome): Boolean =
        chrome === Trading || chrome === Distribution || chrome === MarketingClient || chrome === MarketingOwn

    fun isMarketingClientQa(chrome: OperatingCaptureChrome): Boolean =
        chrome === MarketingClient

    fun isMarketingOwnQa(chrome: OperatingCaptureChrome): Boolean =
        chrome === MarketingOwn

    fun isMarketingQa(chrome: OperatingCaptureChrome): Boolean =
        chrome === MarketingClient || chrome === MarketingOwn
}
