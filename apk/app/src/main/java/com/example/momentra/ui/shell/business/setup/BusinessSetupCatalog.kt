package com.example.momentra.ui.shell.business.setup

/**
 * Nine Business setup subtypes keyed by type code, grouped into the four guided-create families.
 * Keep in lockstep with `BusinessSetupCatalog.swift`.
 */
enum class BusinessSetupFamily(val familyCode: String, val title: String) {
    MARKETING("MARKETING_CAMPAIGNS", "Marketing"),
    TRADING("TRADING_DISTRIBUTION", "Trading"),
    RETAIL("RETAIL_CONSUMER", "Retail"),
    MANUFACTURING("MANUFACTURING_PRODUCTION", "Manufacturing");

    val chips: List<BusinessSetupChip>
        get() = when (this) {
            MARKETING -> listOf(
                BusinessSetupChip("Client campaign", "CLIENT_CAMPAIGN"),
                BusinessSetupChip("Own business", "OWN_BUSINESS_CAMPAIGN"),
            )
            TRADING -> listOf(
                BusinessSetupChip("Trading", "TRADING_WHOLESALE"),
                BusinessSetupChip("Distribution", "DISTRIBUTION_DEALERSHIP"),
            )
            // Figma Kind of business order (`5:71178`): Restaurant → Consumer Service → Retail Store
            RETAIL -> listOf(
                BusinessSetupChip("Restaurant / Food", "RESTAURANT_FOOD"),
                BusinessSetupChip("Consumer Service", "CONSUMER_SERVICE"),
                BusinessSetupChip("Retail Store", "RETAIL_STORE"),
            )
            MANUFACTURING -> listOf(
                BusinessSetupChip("Manufacturing", "STANDARD_PRODUCTION"),
                BusinessSetupChip("Job order", "MADE_TO_ORDER"),
            )
        }

    val defaultTypeCode: String get() = chips.first().typeCode

    companion object {
        fun forFamilyCode(code: String?): BusinessSetupFamily =
            when ((code ?: "").uppercase()) {
                "MARKETING_CAMPAIGNS" -> MARKETING
                "TRADING_DISTRIBUTION" -> TRADING
                "RETAIL_CONSUMER" -> RETAIL
                "MANUFACTURING_PRODUCTION" -> MANUFACTURING
                else -> forTypeCode(code)
            }

        fun forTypeCode(code: String?): BusinessSetupFamily =
            when (BusinessSetupTheme.domainFor(code ?: "")) {
                com.example.momentra.ui.theme.generated.BusinessDomain.MARKETING,
                com.example.momentra.ui.theme.generated.BusinessDomain.MARKETING_OWN,
                -> MARKETING
                com.example.momentra.ui.theme.generated.BusinessDomain.TRADING,
                com.example.momentra.ui.theme.generated.BusinessDomain.DISTRIBUTION,
                -> TRADING
                com.example.momentra.ui.theme.generated.BusinessDomain.RETAIL,
                com.example.momentra.ui.theme.generated.BusinessDomain.RESTAURANT,
                com.example.momentra.ui.theme.generated.BusinessDomain.SERVICES,
                -> RETAIL
                com.example.momentra.ui.theme.generated.BusinessDomain.MANUFACTURING,
                com.example.momentra.ui.theme.generated.BusinessDomain.MADE_TO_ORDER,
                -> MANUFACTURING
                else -> TRADING
            }
    }
}

data class BusinessSetupChip(
    val label: String,
    val typeCode: String,
)
