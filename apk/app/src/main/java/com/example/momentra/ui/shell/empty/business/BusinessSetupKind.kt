package com.example.momentra.ui.shell.empty.business

import androidx.compose.ui.graphics.Color
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.ui.shell.maestro.MaestroIds

enum class BusinessSetupKind(
    val title: String,
    val familyCode: String,
    val analyticsScreen: String,
    val activateColor: Color,
    val maestroTag: String,
    val body: String,
) {
    MARKETING_CAMPAIGNS(
        "Marketing & Campaigns",
        "MARKETING_CAMPAIGNS",
        AnalyticsScreens.BUSINESS_SETUP_TEAM_OPS,
        Color(0xFF818CF8),
        MaestroIds.BUSINESS_SETUP_MARKETING,
        "Budget, channels, people, and results for a campaign.",
    ),
    TRADING_DISTRIBUTION(
        "Trading & Distribution",
        "TRADING_DISTRIBUTION",
        AnalyticsScreens.BUSINESS_SETUP_RUNWAY,
        Color(0xFFFBBF24),
        MaestroIds.BUSINESS_SETUP_TRADING,
        "Source, move, and collect across wholesale or distribution.",
    ),
    RETAIL_CONSUMER(
        "Retail & Consumer",
        "RETAIL_CONSUMER",
        AnalyticsScreens.BUSINESS_SETUP_OPS,
        Color(0xFF34D399),
        MaestroIds.BUSINESS_SETUP_RETAIL,
        "A store, a food outlet, or a consumer service.",
    ),
    MANUFACTURING_PRODUCTION(
        "Manufacturing & Production",
        "MANUFACTURING_PRODUCTION",
        AnalyticsScreens.BUSINESS_SETUP_OPS,
        Color(0xFF38BDF8),
        MaestroIds.BUSINESS_SETUP_MANUFACTURING,
        "Standard production or made-to-order job work.",
    ),
}
