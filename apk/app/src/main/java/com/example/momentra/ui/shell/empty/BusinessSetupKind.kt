package com.example.momentra.ui.shell.empty

import androidx.compose.ui.graphics.Color
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.ui.shell.maestro.MaestroIds

enum class BusinessSetupKind(
    val title: String,
    val familyCode: String,
    val analyticsScreen: String,
    val activateColor: Color,
    val maestroTag: String,
) {
    TEAM_OPERATIONS(
        "Set up Team Operations",
        "TEAM_OPERATIONS",
        AnalyticsScreens.BUSINESS_SETUP_TEAM_OPS,
        Color(0xFF10B981),
        MaestroIds.BUSINESS_SETUP_TEAM,
    ),
    BUSINESS_RUNWAY(
        "Set up Business Runway",
        "BUSINESS_RUNWAY",
        AnalyticsScreens.BUSINESS_SETUP_RUNWAY,
        Color(0xFFFBBF24),
        MaestroIds.BUSINESS_SETUP_RUNWAY,
    ),
    BUSINESS_OPERATIONS(
        "Set up Business Operations",
        "BUSINESS_OPERATIONS",
        AnalyticsScreens.BUSINESS_SETUP_OPS,
        Color(0xFF818CF8),
        MaestroIds.BUSINESS_SETUP_OPS,
    ),
}
