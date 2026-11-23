package com.example.momentra.ui.shell.business.setup

import androidx.compose.ui.graphics.toArgb
import com.example.momentra.ui.theme.generated.BusinessDomain
import com.example.momentra.ui.theme.generated.BusinessTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupThemeTest {

    @Test
    fun nineTypeCodesResolveDistinctDomains() {
        val mapping = mapOf(
            "CLIENT_CAMPAIGN" to BusinessDomain.MARKETING,
            "OWN_BUSINESS_CAMPAIGN" to BusinessDomain.MARKETING_OWN,
            "TRADING_WHOLESALE" to BusinessDomain.TRADING,
            "DISTRIBUTION_DEALERSHIP" to BusinessDomain.DISTRIBUTION,
            "RETAIL_STORE" to BusinessDomain.RETAIL,
            "RESTAURANT_FOOD" to BusinessDomain.RESTAURANT,
            "CONSUMER_SERVICE" to BusinessDomain.SERVICES,
            "STANDARD_PRODUCTION" to BusinessDomain.MANUFACTURING,
            "MADE_TO_ORDER" to BusinessDomain.MADE_TO_ORDER,
        )
        mapping.forEach { (code, domain) ->
            assertEquals(code, domain, BusinessSetupTheme.domainFor(code))
            assertEquals(domain, BusinessSetupTheme.forTypeCode(code).domain)
        }
    }

    @Test
    fun madeToOrderAccentAliasesTokens() {
        val theme = BusinessSetupTheme.forTypeCode("MADE_TO_ORDER")
        assertEquals(
            BusinessTokens.Colors.MADE_TO_ORDERAccent.toArgb(),
            theme.accent.toArgb(),
        )
    }

    @Test
    fun familyChipsCoverNineSubtypes() {
        val codes = BusinessSetupFamily.entries.flatMap { it.chips.map { c -> c.typeCode } }.toSet()
        assertEquals(9, codes.size)
        assertTrue(codes.contains("OWN_BUSINESS_CAMPAIGN"))
        assertTrue(codes.contains("MADE_TO_ORDER"))
    }

    @Test
    fun retailKindOfBusinessLabelsMatchFigma() {
        val labels = BusinessSetupFamily.RETAIL.chips.map { it.label }
        val codes = BusinessSetupFamily.RETAIL.chips.map { it.typeCode }
        assertEquals(
            listOf("Restaurant / Food", "Consumer Service", "Retail Store"),
            labels,
        )
        assertEquals(
            listOf("RESTAURANT_FOOD", "CONSUMER_SERVICE", "RETAIL_STORE"),
            codes,
        )
    }

    @Test
    fun marketingChipLabelsIncludeClientCampaign() {
        assertEquals("Client campaign", BusinessSetupFamily.MARKETING.chips.first().label)
        assertEquals("OWN_BUSINESS_CAMPAIGN", BusinessSetupFamily.MARKETING.chips.last().typeCode)
    }

    @Test
    fun documentsExistForEveryChip() {
        BusinessSetupFamily.entries.forEach { family ->
            family.chips.forEach { chip ->
                val spec = BusinessSetupDocuments.spec(chip.typeCode)
                assertEquals(chip.typeCode, spec.typeCode)
                assertTrue(spec.sections.isNotEmpty())
            }
        }
    }
}
