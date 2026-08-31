package com.example.momentra.ui.theme.shell

import androidx.compose.ui.graphics.Color
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.theme.ShellTokens

/** Global shell chrome — typography/spacing live alongside ShellTokens dimensions. */
object GlobalTheme {
    val topBarBackground: Color = ShellTokens.TopBarBackground
    val bottomBarBackground: Color = ShellTokens.BottomBarBackground
    val surfaceContent: Color = ShellTokens.SurfaceContent
    val contextUnselected: Color = ShellTokens.ContextUnselected
    val bottomSelected: Color = ShellTokens.BottomSelected
    val bottomUnselected: Color = ShellTokens.BottomUnselected
    val companyChipBackground: Color = ShellTokens.CompanyChipBackground
    val companyChipBorder: Color = ShellTokens.CompanyChipBorder
    val actionCircle: Color = ShellTokens.ActionCircle
    val statusOnline: Color = ShellTokens.StatusOnline
    val moduleCardBackground: Color = ShellTokens.ModuleCardBackground
    val createMomentCta: Color = Color(0xFFE8621A)
}

data class ContextTheme(
    val context: AppContext,
    val contextAccent: Color,
    val contextAccentSecondary: Color,
)

object ContextThemes {
    private val personal = ContextTheme(AppContext.PERSONAL, Color(0xFF7C5CFC), Color(0xFFA78BFA))
    private val group = ContextTheme(AppContext.GROUP, Color(0xFFE8621A), Color(0xFFFF8E63))
    private val business = ContextTheme(AppContext.BUSINESS, Color(0xFF818CF8), Color(0xFFA5B4FC))
    /** Figma `1075:7556` Circle Coming Soon — pink, not Personal purple. */
    private val circle = ContextTheme(AppContext.CIRCLE, Color(0xFFE86BA3), Color(0xFFFF6B8A))

    fun of(context: AppContext): ContextTheme = when (context) {
        AppContext.PERSONAL -> personal
        AppContext.GROUP -> group
        AppContext.BUSINESS -> business
        AppContext.CIRCLE -> circle
    }
}

/** Cross-context global surface — NOT a selectable AppContext. Figma Coming Soon `1075:7637`. */
object GlobalSurfaceTheme {
    data class Life360(
        val surface: Color = GlobalTheme.topBarBackground,
        val action: Color = GlobalTheme.actionCircle,
        val online: Color = GlobalTheme.statusOnline,
        /** Coming Soon page background — Figma `#14121B`. */
        val comingSoonBackground: Color = Color(0xFF14121B),
        val card: Color = Color(0xFF161B26),
        val gold: Color = Color(0xFFF2CA50),
        val goldEnd: Color = Color(0xFFFFAB40),
        val textPrimary: Color = Color(0xFFE5E0EE),
        val textSecondary: Color = Color(0xFFC9C4D8),
        val decorativeProgressFraction: Float = 0.65f,
    )

    val life360 = Life360()
}

/** Circle context Coming Soon surface — Figma `1075:7556`. */
object CircleComingSoonTheme {
    val pageStart = Color(0xFF14121B)
    val pageEnd = Color(0xFF1C1B1B)
    val card = Color(0xFF161B26)
    val cardAlt = Color(0xFF1C1B1B)
    val accent = Color(0xFFE86BA3)
    val accentEnd = Color(0xFFFF6B8A)
    val lavender = Color(0xFFB794F6)
    val peach = Color(0xFFFFB5A7)
    val textPrimary = Color(0xFFE5E2E1)
    val textSecondary = Color(0xCCD0C5AF) // ~80% of #D0C5AF
    val selectedTab = Color(0xFFFC6A8B)
    const val decorativeProgressFraction = 0.45f
}

data class MomentTheme(
    val family: String,
    val type: String,
    val primary: Color,
    val secondary: Color,
    val surfaceTint: Color,
    val icon: Color,
)

object MomentThemes {
    fun personal(momentTypeCode: String?): MomentTheme {
        val code = momentTypeCode?.uppercase().orEmpty()
        return when {
            code == "FUTURE_GOAL" || code == "FUTURE_BUILDING" -> MomentTheme(
                family = "PERSONAL",
                type = "FUTURE_BUILDING",
                primary = Color(0xFF10B981),
                secondary = Color(0xFF34D399),
                surfaceTint = Color(0xFF10B981).copy(alpha = 0.16f),
                icon = Color(0xFF10B981),
            )
            code == "LIFESTYLE" -> MomentTheme(
                family = "PERSONAL",
                type = "LIFESTYLE",
                primary = Color(0xFF0EA5A4),
                secondary = Color(0xFF7C5CFC),
                surfaceTint = Color(0xFF0EA5A4).copy(alpha = 0.16f),
                icon = Color(0xFF0EA5A4),
            )
            code == "RELATIONSHIP_CONNECTION" || code == "RELATIONSHIPS" -> MomentTheme(
                family = "PERSONAL",
                type = "RELATIONSHIPS",
                primary = Color(0xFFE91E63),
                secondary = Color(0xFFE12A9E),
                surfaceTint = Color(0xFFE91E63).copy(alpha = 0.16f),
                icon = Color(0xFFE91E63),
            )
            else -> MomentTheme(
                family = "PERSONAL",
                type = "LIFE_OPERATIONS",
                primary = Color(0xFF7C5CFC),
                secondary = Color(0xFFA78BFA),
                surfaceTint = Color(0xFF7C5CFC).copy(alpha = 0.16f),
                icon = Color(0xFF7C5CFC),
            )
        }
    }

    fun group(momentTypeCode: String?): MomentTheme {
        val code = momentTypeCode?.uppercase().orEmpty()
        return when {
            code.contains("WEDDING") -> base("GROUP", "WEDDING", Color(0xFFEC4899), Color(0xFFF472B6))
            code.contains("PARTY") || code.contains("HOUSE") -> base("GROUP", "HOUSE_PARTY", Color(0xFF3B82F6), Color(0xFF60A5FA))
            code.contains("OUTING") || code.contains("OFFICE") -> base("GROUP", "OFFICE_OUTING", Color(0xFF14B8A6), Color(0xFF2DD4BF))
            code.contains("GIFT") -> base("GROUP", "GIFT_POOL", Color(0xFFE8621A), Color(0xFFFF8E63))
            code.contains("PURCHASE") || code.contains("ASSET") -> base("GROUP", "GROUP_PURCHASE", Color(0xFFE8621A), Color(0xFFFF8E63))
            code.contains("FLAT") || code.contains("CO_LIVING") || code.contains("COLIVING") ->
                base("GROUP", "CO_LIVING", Color(0xFF3B82F6), Color(0xFF60A5FA))
            code.contains("HOUSEHOLD") || code.contains("FAMILY") ->
                base("GROUP", "FAMILY_HOUSEHOLD", Color(0xFFEC4899), Color(0xFFF472B6))
            code.contains("TRIP") -> base("GROUP", "TRIP", Color(0xFFE8744F), Color(0xFFFF8E63))
            else -> base("GROUP", "CUSTOM", Color(0xFFE8621A), Color(0xFFC9C4D9))
        }
    }

    fun business(momentTypeCode: String?): MomentTheme {
        val code = momentTypeCode?.uppercase().orEmpty()
        return when {
            code.contains("RUNWAY") -> base("BUSINESS", "BUSINESS_RUNWAY", Color(0xFF34D399), Color(0xFF6EE7B7))
            code.contains("TEAM") -> base("BUSINESS", "TEAM_OPERATIONS", Color(0xFF818CF8), Color(0xFFA5B4FC))
            else -> base("BUSINESS", "BUSINESS_OPERATIONS", Color(0xFF818CF8), Color(0xFFFB923C))
        }
    }

    fun resolve(context: AppContext, momentTypeCode: String?): MomentTheme = when (context) {
        AppContext.PERSONAL -> personal(momentTypeCode)
        AppContext.GROUP -> group(momentTypeCode)
        AppContext.BUSINESS -> business(momentTypeCode)
        AppContext.CIRCLE -> personal(null)
    }

    private fun base(family: String, type: String, primary: Color, secondary: Color) = MomentTheme(
        family = family,
        type = type,
        primary = primary,
        secondary = secondary,
        surfaceTint = primary.copy(alpha = 0.16f),
        icon = primary,
    )
}
