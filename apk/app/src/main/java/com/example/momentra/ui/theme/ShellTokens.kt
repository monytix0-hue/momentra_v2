package com.example.momentra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.momentra.domain.AppContext

/** Shell chrome tokens aligned to Figma Personal/Group/Business headers + bottom nav. */
object ShellTokens {
    val TopBarBackground = Color(0xFF0C0F15)
    val ContextSelectedPersonal = Color(0xFF7C5CFC)
    val ContextSelectedGroup = Color(0xFFE8621A)
    val ContextSelectedBusiness = Color(0xFF818CF8)
    val ContextSelectedCircle = Color(0xFFFC6A8B)
    val ContextUnselected = Color(0xFFC9C4D9)
    val SurfaceContent = Color(0xFF14121B)
    val EmptyBody = Color(0xFFC9C4D8)
    val BottomBarBackground = Color(0xFF0C0F15)
    /** Figma Comp / Personal / Bottom Nav selected accent. */
    val BottomSelected = Color(0xFFC9BFFF)
    val BottomUnselected = Color(0xFFC9C4D8)
    val CompanyChipBackground = Color(0xFF1A2030)
    val CompanyChipBorder = Color(0xFF3A4258)
    val ActionCircle = Color(0xFF1E293B)
    val StatusOnline = Color(0xFF10B981)
    val ModuleCardBackground = Color(0xFF161B26)

    val TopBarHeight = 48.dp
    /** Compact context tabs — less vertical chrome. */
    val ContextSwitcherHeight = 36.dp
    val BottomBarHeight = 72.dp
    val CreateFabSize = 28.dp
    val AvatarSize = 32.dp
    val IconTap = 32.dp

    fun contextSelectedColor(context: AppContext): Color = when (context) {
        AppContext.PERSONAL -> ContextSelectedPersonal
        AppContext.GROUP -> ContextSelectedGroup
        AppContext.BUSINESS -> ContextSelectedBusiness
        AppContext.CIRCLE -> ContextSelectedCircle
    }

}
